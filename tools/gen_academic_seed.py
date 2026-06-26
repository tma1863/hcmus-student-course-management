#!/usr/bin/env python3
"""Generate academic seed data: students, open_courses, enrollments (+ GPA).

Reads the reference data (major programs + curriculum) from the live Postgres DB
and emits three idempotent SQL files in the repo root:

    students_seed.sql       all students for every major program (correct IDs)
    open_courses_seed.sql   one offering per curriculum course in sem 1-4, plus
                            retake offerings for failed sem-1 required courses
    enrollments_seed.sql    graded enrollments for required sem 1-3 courses, with
                            a 10-20% struggling cohort (fails + retakes), and an
                            UPDATE that sets each student's GPA

The grading scale and GPA rule mirror the backend (common/util/GradeScale.java and
EnrollmentServiceImpl.recalculateGpa) exactly, so seeded GPAs equal what the API
would compute: each distinct course counts once (latest attempt), credit-weighted
on the 4.0 scale, rounded HALF_UP to 2 decimals.

Deterministic: a fixed RNG seed + uuid5 student ids make re-runs reproduce byte
-for-byte identical output.

Data source: the script shells out to `docker exec <container> psql ...`, matching
this project's docker-compose setup (there is no psql/driver on the host).

Usage:
    python3 tools/gen_academic_seed.py            # uses defaults below
    python3 tools/gen_academic_seed.py --container scm-postgres-db
"""

import argparse
import os
import subprocess
import sys
import uuid
from collections import defaultdict
from decimal import Decimal, ROUND_HALF_UP
from random import Random

# --- configuration ---------------------------------------------------------

SEED = 20260626
# Stable namespace so uuid5(student_id) is reproducible across runs.
NAMESPACE = uuid.UUID("5f1d2c3b-4a59-4e6f-8b7c-0a1b2c3d4e5f")

# Cohort size per major_program_id.
COHORT_SIZES = {1: 110, 2: 230, 3: 150}
DEFAULT_COHORT = 100

# Fraction of each cohort that struggles (fails 1-2 courses). Within the agreed 10-20%.
STRUGGLER_FRACTION = 0.15

# program_semester -> (academic_year, operational term, open-course status).
# Odd program semesters fall in term 1, even in term 2; semester 4 is the upcoming
# (even) term, still OPEN for registration. Sem 1-3 are in the past (CLOSED).
TERMS = {
    1: ("2025-2026", 1, "CLOSED"),
    2: ("2025-2026", 2, "CLOSED"),
    3: ("2026-2027", 1, "CLOSED"),
    4: ("2026-2027", 2, "OPEN"),
}
# A failed odd (sem-1) course is retaken in the next odd term — the same term as sem 3.
RETAKE_TERM = ("2026-2027", 1, "CLOSED")

# Seat buffer above the cohort size for every offering.
SEAT_BUFFER = 20

# --- Vietnamese name pools -------------------------------------------------

SURNAMES = [
    "Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Huỳnh", "Phan", "Vũ", "Võ", "Đặng",
    "Bùi", "Đỗ", "Hồ", "Ngô", "Dương", "Lý", "Đinh", "Trương", "Đoàn", "Mai",
]
MID_M = ["Văn", "Hữu", "Công", "Quốc", "Thành", "Minh", "Anh", "Bá", "Đức", "Xuân", "Hoàng", "Tuấn"]
MID_F = ["Thị", "Thu", "Kim", "Mỹ", "Diễm", "Bích", "Ngọc", "Thanh", "Phương", "Hồng", "Khánh", "Cẩm"]
GIVEN_M = ["An", "Bình", "Cường", "Dũng", "Hải", "Khang", "Long", "Nam", "Quân", "Trí", "Đạt", "Hùng", "Phong", "Sơn", "Tài", "Vinh"]
GIVEN_F = ["Châu", "Hà", "Linh", "Ngân", "Oanh", "Vy", "Dung", "Hương", "Lan", "Nhung", "Trang", "Yến", "Thảo", "Quyên", "Hân", "Mai"]


# --- grade scale (mirror of common/util/GradeScale.java) -------------------

def grade_point(score):
    """4.0-scale grade point for a raw 0-10 score (as a Decimal), matching GradeScale."""
    s = Decimal(str(score))
    if s >= Decimal("8.5"):
        return Decimal("4.0")
    if s >= Decimal("8.0"):
        return Decimal("3.5")
    if s >= Decimal("7.0"):
        return Decimal("3.0")
    if s >= Decimal("6.5"):
        return Decimal("2.5")
    if s >= Decimal("5.5"):
        return Decimal("2.0")
    if s >= Decimal("5.0"):
        return Decimal("1.5")
    if s >= Decimal("4.0"):
        return Decimal("1.0")
    return Decimal("0.0")


def is_pass(score):
    return grade_point(score) >= Decimal("1.0")


# --- DB access -------------------------------------------------------------

def psql_rows(args, query):
    """Run a query via docker exec psql and return rows as lists of string cells."""
    cmd = [
        "docker", "exec", "-e", f"PGPASSWORD={args.password}", args.container,
        "psql", "-U", args.user, "-d", args.db, "-v", "ON_ERROR_STOP=1",
        "--no-align", "--field-separator=|", "--tuples-only", "-c", query,
    ]
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        sys.exit(f"psql failed:\n{result.stderr}")
    return [line.split("|") for line in result.stdout.splitlines() if line.strip()]


def load_reference(args):
    programs = []  # (program_id, major_code, entry_year)
    for pid, code, year in psql_rows(
        args,
        "SELECT mp.id, m.major_code, mp.entry_year FROM major_programs mp "
        "JOIN majors m ON m.id = mp.major_id ORDER BY mp.id",
    ):
        programs.append((int(pid), code, year))

    course_majors = []  # dict per row
    for cid, prog, credits, sem, req in psql_rows(
        args,
        "SELECT id, major_program_id, credits, program_semester, is_required "
        "FROM course_majors WHERE program_semester BETWEEN 1 AND 4 ORDER BY id",
    ):
        course_majors.append({
            "id": int(cid),
            "program": int(prog),
            "credits": int(credits),
            "semester": int(sem),
            "required": req == "t",
        })
    return programs, course_majors


# --- generation ------------------------------------------------------------

def sql_str(value):
    return "'" + str(value).replace("'", "''") + "'"


def gen_students(programs, rng):
    students = []  # dict: uuid, student_id, name, gender, program
    for program_id, major_code, entry_year in programs:
        n = COHORT_SIZES.get(program_id, DEFAULT_COHORT)
        prefix = entry_year[-2:] + major_code  # e.g. "25" + "20" -> "2520"
        for i in range(1, n + 1):
            student_id = f"{prefix}{i:04d}"
            gender = rng.choice(["MALE", "FEMALE"])
            surname = rng.choice(SURNAMES)
            if gender == "MALE":
                name = f"{surname} {rng.choice(MID_M)} {rng.choice(GIVEN_M)}"
            else:
                name = f"{surname} {rng.choice(MID_F)} {rng.choice(GIVEN_F)}"
            students.append({
                "uuid": str(uuid.uuid5(NAMESPACE, student_id)),
                "student_id": student_id,
                "name": name,
                "gender": gender,
                "program": program_id,
            })
    return students


def pass_score(rng):
    """Realistic passing score (>= 4.0), centred ~7.0, one decimal."""
    value = rng.gauss(7.0, 1.2)
    value = max(4.0, min(10.0, value))
    return round(value, 1)


def fail_score(rng):
    return round(rng.uniform(2.0, 3.9), 1)


def retake_score(rng):
    """A modest pass on the retake."""
    return round(rng.uniform(4.5, 7.5), 1)


def build(programs, course_majors, rng):
    cm_by_id = {cm["id"]: cm for cm in course_majors}

    # Home offering (one open course) for every curriculum course in sem 1-4.
    oc_seq = 0
    home_oc = {}              # cm_id -> open_course id
    open_courses = []         # dict per offering
    for cm in sorted(course_majors, key=lambda c: c["id"]):
        oc_seq += 1
        ay, term, status = TERMS[cm["semester"]]
        home_oc[cm["id"]] = oc_seq
        open_courses.append({
            "id": oc_seq, "cm_id": cm["id"], "academic_year": ay,
            "term": term, "status": status, "program": cm["program"], "enrolled": 0,
        })

    required_sem13 = defaultdict(list)  # program -> [cm,...]
    for cm in course_majors:
        if cm["required"] and cm["semester"] in (1, 2, 3):
            required_sem13[cm["program"]].append(cm)
    for lst in required_sem13.values():
        lst.sort(key=lambda c: c["id"])

    students = gen_students(programs, rng)
    students_by_program = defaultdict(list)
    for s in students:
        students_by_program[s["program"]].append(s)

    # Pick the struggling cohort per program (deterministic).
    strugglers = set()
    for program_id, cohort in students_by_program.items():
        k = round(len(cohort) * STRUGGLER_FRACTION)
        for s in rng.sample(sorted(cohort, key=lambda x: x["student_id"]), k):
            strugglers.add(s["student_id"])

    # First pass: attempt-1 enrollments, and remember which courses get retaken.
    enrollments = []          # (student_uuid, oc_id, score_or_None, status, attempt)
    student_records = defaultdict(list)  # uuid -> [(cm_id, credits, score, attempt)]
    retake_plan = []          # (student_uuid, cm_id, score)
    retake_needed = set()     # cm_ids needing a retake offering

    for program_id, cohort in students_by_program.items():
        req = required_sem13.get(program_id, [])
        for s in cohort:
            fail_ids = set()
            if s["student_id"] in strugglers and req:
                n_fail = rng.randint(1, 2)
                chosen = rng.sample(req, min(n_fail, len(req)))
                fail_ids = {cm["id"] for cm in chosen}

            for cm in req:
                if cm["id"] in fail_ids:
                    score, status = fail_score(rng), "FAILED"
                else:
                    score, status = pass_score(rng), "COMPLETED"
                enrollments.append((s["uuid"], home_oc[cm["id"]], score, status, 1))
                student_records[s["uuid"]].append((cm["id"], cm["credits"], score, 1))

                # Retake only failed sem-1 (odd) courses — their next odd term (sem 3's
                # term) is within the studied window. Failed sem-2/3 courses stay FAILED.
                if status == "FAILED" and cm["semester"] == 1:
                    rscore = retake_score(rng)
                    retake_plan.append((s["uuid"], cm["id"], rscore))
                    retake_needed.add(cm["id"])

    # Retake offerings (next odd term) for the courses that need them.
    retake_oc = {}  # cm_id -> open_course id
    ay, term, status = RETAKE_TERM
    for cm_id in sorted(retake_needed):
        oc_seq += 1
        retake_oc[cm_id] = oc_seq
        open_courses.append({
            "id": oc_seq, "cm_id": cm_id, "academic_year": ay, "term": term,
            "status": status, "program": cm_by_id[cm_id]["program"], "enrolled": 0,
        })

    for student_uuid, cm_id, rscore in retake_plan:
        enrollments.append((student_uuid, retake_oc[cm_id], rscore, "COMPLETED", 2))
        student_records[student_uuid].append(
            (cm_id, cm_by_id[cm_id]["credits"], rscore, 2))

    # Seat counts per offering.
    enrolled_count = defaultdict(int)
    for _, oc_id, _, _, _ in enrollments:
        enrolled_count[oc_id] += 1
    for oc in open_courses:
        oc["enrolled"] = enrolled_count[oc["id"]]

    # GPA per student: latest attempt per course, credit-weighted, HALF_UP scale 2.
    gpa_by_uuid = {}
    for s in students:
        latest = {}  # cm_id -> (attempt, score, credits)
        for cm_id, credits, score, attempt in student_records.get(s["uuid"], []):
            if cm_id not in latest or attempt > latest[cm_id][0]:
                latest[cm_id] = (attempt, score, credits)
        total_points = Decimal("0")
        total_credits = 0
        for _, score, credits in latest.values():
            total_points += grade_point(score) * Decimal(credits)
            total_credits += credits
        if total_credits == 0:
            gpa = Decimal("0.00")
        else:
            gpa = (total_points / Decimal(total_credits)).quantize(
                Decimal("0.01"), rounding=ROUND_HALF_UP)
        gpa_by_uuid[s["uuid"]] = gpa

    return students, open_courses, enrollments, gpa_by_uuid


# --- SQL emission ----------------------------------------------------------

def chunked_insert(out, header, rows, chunk=500):
    for start in range(0, len(rows), chunk):
        out.append(header)
        batch = rows[start:start + chunk]
        out.append(",\n".join(batch) + ";")
        out.append("")


def write_students_sql(path, students):
    out = [
        "-- Auto-generated by tools/gen_academic_seed.py — do not edit by hand.",
        f"-- {len(students)} students across all major programs; ids follow "
        "last2(entryYear)+majorCode+seq.",
        "BEGIN;",
        "TRUNCATE students CASCADE;",
        "",
    ]
    header = ("INSERT INTO students "
              "(id, student_id, name, gender, major_program_id, status, gpa, created_at, updated_at) VALUES")
    rows = [
        f"  ({sql_str(s['uuid'])}, {sql_str(s['student_id'])}, {sql_str(s['name'])}, "
        f"{sql_str(s['gender'])}, {s['program']}, 'STUDYING', 0, NOW(), NOW())"
        for s in students
    ]
    chunked_insert(out, header, rows)
    out.append("COMMIT;")
    _write(path, out)


def write_open_courses_sql(path, open_courses):
    out = [
        "-- Auto-generated by tools/gen_academic_seed.py — do not edit by hand.",
        f"-- {len(open_courses)} offerings: home offerings for curriculum sem 1-4 "
        "plus retake offerings for failed sem-1 courses.",
        "BEGIN;",
        "TRUNCATE open_courses CASCADE;",
        "",
    ]
    header = ("INSERT INTO open_courses "
              "(id, course_major_id, semester, academic_year, max_students, enrolled_count, status, created_at, updated_at) VALUES")
    rows = []
    for oc in open_courses:
        max_students = COHORT_SIZES.get(oc["program"], DEFAULT_COHORT) + SEAT_BUFFER
        rows.append(
            f"  ({oc['id']}, {oc['cm_id']}, {oc['term']}, {sql_str(oc['academic_year'])}, "
            f"{max_students}, {oc['enrolled']}, {sql_str(oc['status'])}, NOW(), NOW())")
    chunked_insert(out, header, rows)
    out.append("-- Keep the identity sequence ahead of the explicit ids inserted above.")
    out.append("SELECT setval(pg_get_serial_sequence('open_courses', 'id'), "
               "(SELECT MAX(id) FROM open_courses));")
    out.append("COMMIT;")
    _write(path, out)


def write_enrollments_sql(path, enrollments, gpa_by_uuid):
    out = [
        "-- Auto-generated by tools/gen_academic_seed.py — do not edit by hand.",
        f"-- {len(enrollments)} graded enrollments for required sem 1-3 courses; GPAs "
        "are recomputed to match EnrollmentService.recalculateGpa.",
        "-- Load order: students_seed.sql -> open_courses_seed.sql -> this file.",
        "BEGIN;",
        "TRUNCATE enrollments RESTART IDENTITY;",
        "",
    ]
    header = ("INSERT INTO enrollments "
              "(student_id, open_course_id, score, status, attempt_number, created_at, updated_at) VALUES")
    rows = []
    for student_uuid, oc_id, score, status, attempt in enrollments:
        score_sql = "NULL" if score is None else f"{score:.2f}"
        rows.append(
            f"  ({sql_str(student_uuid)}, {oc_id}, {score_sql}, {sql_str(status)}, "
            f"{attempt}, NOW(), NOW())")
    chunked_insert(out, header, rows)

    out.append("-- Cumulative GPA per student (latest attempt per course, credit-weighted).")
    for student_uuid in sorted(gpa_by_uuid):
        gpa = gpa_by_uuid[student_uuid]
        out.append(f"UPDATE students SET gpa = {gpa} WHERE id = {sql_str(student_uuid)};")
    out.append("COMMIT;")
    _write(path, out)


def _write(path, lines):
    with open(path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n")
    print(f"Wrote {path}", file=sys.stderr)


# --- main ------------------------------------------------------------------

def main():
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--container", default=os.environ.get("PG_CONTAINER", "scm-postgres-db"))
    parser.add_argument("--db", default=os.environ.get("POSTGRES_DB", "student_course_management"))
    parser.add_argument("--user", default=os.environ.get("POSTGRES_USER", "postgres"))
    parser.add_argument("--password", default=os.environ.get("POSTGRES_PASSWORD", "postgres"))
    parser.add_argument("--out-dir", default=os.path.join(os.path.dirname(__file__), ".."))
    args = parser.parse_args()

    rng = Random(SEED)
    programs, course_majors = load_reference(args)
    if not programs:
        sys.exit("No major programs found in the database.")
    students, open_courses, enrollments, gpa_by_uuid = build(programs, course_majors, rng)

    out_dir = os.path.abspath(args.out_dir)
    write_students_sql(os.path.join(out_dir, "students_seed.sql"), students)
    write_open_courses_sql(os.path.join(out_dir, "open_courses_seed.sql"), open_courses)
    write_enrollments_sql(os.path.join(out_dir, "enrollments_seed.sql"), enrollments, gpa_by_uuid)

    print(
        f"Generated {len(students)} students, {len(open_courses)} open courses, "
        f"{len(enrollments)} enrollments.", file=sys.stderr)


if __name__ == "__main__":
    main()
