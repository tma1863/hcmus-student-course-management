#!/usr/bin/env python3
"""Generate the 2024 entry-year cohort as ADDITIVE seed migrations.

Companion to gen_academic_seed.py (which seeds the 2025 cohort). This script adds
a second cohort for the 2024 major-programs (ids 4/5/6) created in migration
V2026_07_02_0006, whose curriculum was copied in V2026_07_02_0007.

Two things differ from the 2025 generator, both by design:

  * Retake depth. A 2024-entry student has FOUR completed terms behind them
    (2024-2025 t1/t2, 2025-2026 t1/t2), so a failed first-semester course can be
    retaken across successive terms. attempt_number therefore ranges 1..4, versus
    the 2025 cohort's 1..2 (one retake window so far).

  * Additivity. The 2025 seeds TRUNCATE + reseed with explicit ids. Those
    migrations are already applied, so this cohort is layered on top instead:
      - students / enrollments: plain INSERT ... ON CONFLICT DO NOTHING
      - open_courses: explicit ids continuing after the 2025 block (>=102), which
        is deterministic because the 2025 open_courses occupy exactly ids 1..101.
      - open_courses reference course_majors by the STABLE (major_program_id,
        course_id) key via subquery, NOT by the transient serial id, so the
        migration is correct on a fresh DB where those ids differ.

The grade scale and GPA rule are imported from gen_academic_seed so seeded GPAs
match EnrollmentServiceImpl.recalculateGpa exactly. Deterministic: fixed RNG seed
plus uuid5 student ids reproduce byte-for-byte identical output.

Usage:
    python3 tools/gen_2024_cohort.py            # reads the live DB, writes migrations
"""

import argparse
import os
import subprocess
import sys
import uuid
from collections import defaultdict
from decimal import Decimal, ROUND_HALF_UP
from random import Random

from gen_academic_seed import (
    NAMESPACE, SURNAMES, MID_M, MID_F, GIVEN_M, GIVEN_F, SEAT_BUFFER,
    grade_point, pass_score, fail_score, retake_score, sql_str,
)

# --- configuration ---------------------------------------------------------

SEED = 20240626  # distinct from the 2025 generator's seed.

# Cohort size per 2024 major_program_id. Mirrors the same-major 2025 cohort:
#   4 (major 2, Khoa học Dữ liệu)  <- like program 1 (110)
#   5 (major 1, Kỹ sư phần mềm)    <- like program 2 (230)
#   6 (major 3, Hệ thống thông tin)<- like program 3 (150)
COHORT_SIZES = {4: 110, 5: 230, 6: 150}
PROGRAM_IDS = [4, 5, 6]

# Fraction of each cohort that struggles (fails 1-2 courses). Same band as 2025.
STRUGGLER_FRACTION = 0.15

# 2024 entry timeline. program_semester -> (academic_year, operational term, status).
# One year earlier than the 2025 cohort; all four terms are in the past -> CLOSED.
TERMS = {
    1: ("2024-2025", 1, "CLOSED"),
    2: ("2024-2025", 2, "CLOSED"),
    3: ("2025-2026", 1, "CLOSED"),
    4: ("2025-2026", 2, "CLOSED"),
}
# Retake offerings for a failed first-semester course, one per successive term.
# attempt_number -> (academic_year, term, status). Supports attempts 2, 3 and 4.
RETAKE_TERMS = {
    2: ("2024-2025", 2, "CLOSED"),
    3: ("2025-2026", 1, "CLOSED"),
    4: ("2025-2026", 2, "CLOSED"),
}
MAX_ATTEMPT = 4

# Probability a retake still FAILS (so the chain continues to the next term). The
# final allowed attempt is never forced to fail, capping chains at MAX_ATTEMPT.
RETAKE_FAIL_PROB = 0.45

MIGRATION_DIR = os.path.join(
    os.path.dirname(__file__), "..", "src", "main", "resources", "db", "migration")


# --- DB access -------------------------------------------------------------

def psql_rows(args, query):
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
    """2024 curriculum bridge rows, plus each program's major_code and the highest
    existing open_course id (the offset the new offerings continue from)."""
    programs = {}  # program_id -> major_code
    for pid, code in psql_rows(
        args,
        "SELECT mp.id, m.major_code FROM major_programs mp "
        "JOIN majors m ON m.id = mp.major_id "
        "WHERE mp.entry_year = '2024' ORDER BY mp.id",
    ):
        programs[int(pid)] = code

    course_majors = []
    for cid, prog, gcourse, credits, sem, req in psql_rows(
        args,
        "SELECT id, major_program_id, course_id, credits, program_semester, is_required "
        "FROM course_majors WHERE major_program_id IN (4,5,6) "
        "AND program_semester BETWEEN 1 AND 4 ORDER BY id",
    ):
        course_majors.append({
            "id": int(cid),
            "program": int(prog),
            "course_id": int(gcourse),   # global courses.id -> stable across DBs
            "credits": int(credits),
            "semester": int(sem),
            "required": req == "t",
        })

    max_oc = int(psql_rows(args, "SELECT COALESCE(MAX(id), 0) FROM open_courses")[0][0])
    return programs, course_majors, max_oc


# --- generation ------------------------------------------------------------

def gen_students(programs, rng):
    students = []
    for program_id in PROGRAM_IDS:
        major_code = programs[program_id]
        n = COHORT_SIZES[program_id]
        prefix = "24" + major_code          # 2024 entry year -> "24" + code
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


def retake_outcome(rng, final):
    """Score + status for a retake attempt. Non-final retakes may fail again so the
    chain continues; the final attempt is always graded as a genuine result."""
    if not final and rng.random() < RETAKE_FAIL_PROB:
        return fail_score(rng), "FAILED"
    return retake_score(rng), "COMPLETED"


def build(programs, course_majors, max_oc, rng):
    cm_by_id = {cm["id"]: cm for cm in course_majors}

    # Home offering (explicit id, continuing after the 2025 block) per sem 1-4 course.
    oc_seq = max_oc
    home_oc = {}          # cm_id -> open_course id
    open_courses = []     # dict per offering: id, program, course_id, semester(term), ay, status
    for cm in sorted(course_majors, key=lambda c: c["id"]):
        oc_seq += 1
        ay, term, status = TERMS[cm["semester"]]
        home_oc[cm["id"]] = oc_seq
        open_courses.append({
            "id": oc_seq, "program": cm["program"], "course_id": cm["course_id"],
            "term": term, "academic_year": ay, "status": status, "enrolled": 0,
        })

    required_sem13 = defaultdict(list)   # program -> [cm,...] required, sem 1-3
    for cm in course_majors:
        if cm["required"] and cm["semester"] in (1, 2, 3):
            required_sem13[cm["program"]].append(cm)
    for lst in required_sem13.values():
        lst.sort(key=lambda c: c["id"])

    students = gen_students(programs, rng)
    students_by_program = defaultdict(list)
    for s in students:
        students_by_program[s["program"]].append(s)

    # Deterministic struggling cohort per program.
    strugglers = set()
    for program_id, cohort in students_by_program.items():
        k = round(len(cohort) * STRUGGLER_FRACTION)
        for s in rng.sample(sorted(cohort, key=lambda x: x["student_id"]), k):
            strugglers.add(s["student_id"])

    enrollments = []                     # (uuid, oc_id, score|None, status, attempt)
    student_records = defaultdict(list)  # uuid -> [(cm_id, credits, score, attempt)]
    # Retake offerings created on demand, keyed by (cm_id, attempt) so every attempt
    # is a distinct offering in a distinct term (keeps (student, open_course) unique).
    retake_oc = {}                       # (cm_id, attempt) -> open_course id

    def ensure_retake_oc(cm, attempt):
        key = (cm["id"], attempt)
        if key in retake_oc:
            return retake_oc[key]
        nonlocal oc_seq
        oc_seq += 1
        ay, term, status = RETAKE_TERMS[attempt]
        retake_oc[key] = oc_seq
        open_courses.append({
            "id": oc_seq, "program": cm["program"], "course_id": cm["course_id"],
            "term": term, "academic_year": ay, "status": status, "enrolled": 0,
        })
        return oc_seq

    for program_id, cohort in students_by_program.items():
        req = required_sem13.get(program_id, [])
        for s in cohort:
            fail_ids = set()
            if s["student_id"] in strugglers and req:
                n_fail = rng.randint(1, 2)
                chosen = rng.sample(req, min(n_fail, len(req)))
                fail_ids = {cm["id"] for cm in chosen}

            for cm in req:
                # Attempt 1 in the home offering.
                if cm["id"] in fail_ids:
                    score, status = fail_score(rng), "FAILED"
                else:
                    score, status = pass_score(rng), "COMPLETED"
                enrollments.append((s["uuid"], home_oc[cm["id"]], score, status, 1))
                student_records[s["uuid"]].append((cm["id"], cm["credits"], score, 1))

                # Only failed FIRST-semester courses have the term runway to retake;
                # each retake is a fresh attempt in the next term, up to MAX_ATTEMPT.
                if status == "FAILED" and cm["semester"] == 1:
                    attempt = 1
                    while attempt < MAX_ATTEMPT:
                        attempt += 1
                        final = attempt == MAX_ATTEMPT
                        rscore, rstatus = retake_outcome(rng, final)
                        oc_id = ensure_retake_oc(cm, attempt)
                        enrollments.append((s["uuid"], oc_id, rscore, rstatus, attempt))
                        student_records[s["uuid"]].append(
                            (cm["id"], cm["credits"], rscore, attempt))
                        if rstatus == "COMPLETED":
                            break

    # Seat counts.
    enrolled_count = defaultdict(int)
    for _, oc_id, _, _, _ in enrollments:
        enrolled_count[oc_id] += 1
    for oc in open_courses:
        oc["enrolled"] = enrolled_count[oc["id"]]

    # GPA: latest attempt per course, credit-weighted, HALF_UP scale 2 (mirrors backend).
    gpa_by_uuid = {}
    for s in students:
        latest = {}
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


# --- SQL emission (additive migrations) ------------------------------------

def _write(path, lines):
    with open(path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n")
    print(f"Wrote {path}", file=sys.stderr)


def chunked(out, header, rows, tail, chunk=500):
    """Emit INSERTs in batches of `chunk`, each ending with `tail` (e.g. an ON
    CONFLICT clause) before the terminating semicolon."""
    for start in range(0, len(rows), chunk):
        out.append(header)
        out.append(",\n".join(rows[start:start + chunk]))
        out.append(tail + ";")
        out.append("")


def write_students(path, students):
    out = [
        "-- Auto-generated by tools/gen_2024_cohort.py — do not edit by hand.",
        f"-- {len(students)} students for the 2024 entry-year programs (ids 4/5/6);",
        "-- student_id follows 24 + majorCode + seq. Additive: layered on the 2025",
        "-- cohort (V2026_07_02_0002), so INSERT ... ON CONFLICT instead of TRUNCATE.",
        "BEGIN;",
        "",
    ]
    header = ("INSERT INTO students "
              "(id, student_id, name, gender, major_program_id, status, gpa, created_at, updated_at) VALUES")
    rows = [
        f"  ({sql_str(s['uuid'])}, {sql_str(s['student_id'])}, {sql_str(s['name'])}, "
        f"{sql_str(s['gender'])}, {s['program']}, 'STUDYING', 0, NOW(), NOW())"
        for s in students
    ]
    chunked(out, header, rows, "ON CONFLICT (id) DO NOTHING")
    out.append("COMMIT;")
    _write(path, out)


def write_open_courses(path, open_courses):
    out = [
        "-- Auto-generated by tools/gen_2024_cohort.py — do not edit by hand.",
        f"-- {len(open_courses)} offerings for the 2024 cohort: home offerings for",
        "-- curriculum sem 1-4 plus retake offerings for failed first-semester courses.",
        "-- Explicit ids continue after the 2025 block (1..101). course_major_id is",
        "-- resolved by the stable (major_program_id, course_id) key so the ids stay",
        "-- valid on a fresh database. Additive: INSERT ... ON CONFLICT, no TRUNCATE.",
        "BEGIN;",
        "",
    ]
    header = ("INSERT INTO open_courses "
              "(id, course_major_id, semester, academic_year, max_students, enrolled_count, status, created_at, updated_at) VALUES")
    rows = []
    for oc in open_courses:
        max_students = COHORT_SIZES[oc["program"]] + SEAT_BUFFER
        cm_ref = (f"(SELECT id FROM course_majors WHERE major_program_id = {oc['program']} "
                  f"AND course_id = {oc['course_id']})")
        rows.append(
            f"  ({oc['id']}, {cm_ref}, {oc['term']}, {sql_str(oc['academic_year'])}, "
            f"{max_students}, {oc['enrolled']}, {sql_str(oc['status'])}, NOW(), NOW())")
    chunked(out, header, rows, "ON CONFLICT (id) DO NOTHING")
    out.append("-- Keep the identity sequence ahead of the explicit ids inserted above.")
    out.append("SELECT setval(pg_get_serial_sequence('open_courses', 'id'), "
               "(SELECT MAX(id) FROM open_courses));")
    out.append("COMMIT;")
    _write(path, out)


def write_enrollments(path, enrollments, gpa_by_uuid):
    attempts = defaultdict(int)
    for _, _, _, _, a in enrollments:
        attempts[a] += 1
    dist = ", ".join(f"attempt {a}: {attempts[a]}" for a in sorted(attempts))
    out = [
        "-- Auto-generated by tools/gen_2024_cohort.py — do not edit by hand.",
        f"-- {len(enrollments)} graded enrollments for the 2024 cohort ({dist}).",
        "-- GPAs mirror EnrollmentService.recalculateGpa. Additive: INSERT ... ON",
        "-- CONFLICT on (student_id, open_course_id); GPA UPDATEs touch only 2024 ids.",
        "-- Load order: 0006 majors -> 0007 curriculum -> 0008 students -> 0009 open",
        "-- courses -> this file.",
        "BEGIN;",
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
    chunked(out, header, rows,
            "ON CONFLICT ON CONSTRAINT uq_enrollments_student_open_course DO NOTHING")
    out.append("-- Cumulative GPA per 2024 student (latest attempt per course, credit-weighted).")
    for student_uuid in sorted(gpa_by_uuid):
        out.append(
            f"UPDATE students SET gpa = {gpa_by_uuid[student_uuid]} "
            f"WHERE id = {sql_str(student_uuid)};")
    out.append("COMMIT;")
    _write(path, out)


# --- main ------------------------------------------------------------------

def main():
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--container", default=os.environ.get("PG_CONTAINER", "scm-postgres-db"))
    parser.add_argument("--db", default=os.environ.get("POSTGRES_DB", "student_course_management"))
    parser.add_argument("--user", default=os.environ.get("POSTGRES_USER", "postgres"))
    parser.add_argument("--password", default=os.environ.get("POSTGRES_PASSWORD", "postgres"))
    parser.add_argument("--out-dir", default=MIGRATION_DIR)
    args = parser.parse_args()

    rng = Random(SEED)
    programs, course_majors, max_oc = load_reference(args)
    if sorted(programs) != PROGRAM_IDS:
        sys.exit(f"Expected 2024 programs {PROGRAM_IDS}, found {sorted(programs)}. "
                 "Apply V2026_07_02_0006 and _0007 first.")
    if not course_majors:
        sys.exit("No 2024 course_majors found. Apply V2026_07_02_0007 first.")

    students, open_courses, enrollments, gpa_by_uuid = build(
        programs, course_majors, max_oc, rng)

    out_dir = os.path.abspath(args.out_dir)
    write_students(os.path.join(out_dir, "V2026_07_02_0008__add_student_information_2024.sql"), students)
    write_open_courses(os.path.join(out_dir, "V2026_07_02_0009__add_open_course_information_2024.sql"), open_courses)
    write_enrollments(os.path.join(out_dir, "V2026_07_02_0010__add_enrollment_information_2024.sql"), enrollments, gpa_by_uuid)

    print(f"Generated {len(students)} students, {len(open_courses)} open courses, "
          f"{len(enrollments)} enrollments (max open_course id {max_oc} -> "
          f"{max_oc + len(open_courses)}).", file=sys.stderr)


if __name__ == "__main__":
    main()
