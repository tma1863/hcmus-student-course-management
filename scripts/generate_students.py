#!/usr/bin/env python3
"""Generate a .sql seed file with random students per major.

Output INSERT statements target the `students` table (PostgreSQL) and respect
all DB constraints:
  - id          UUID
  - student_id  VARCHAR(8) UNIQUE  -> "<YY><major_code><####>" (matches the app)
  - name        VARCHAR(255)
  - gender             'MALE' | 'FEMALE'
  - major_program_id   FK -> major_programs.id
  - status             always 'STUDYING'
  - gpa         not inserted -> DB default (0.0) applies

Run:  python3 scripts/generate_students.py [output.sql]
"""

import random
import sys
import uuid

# Current major programs in the database, with how many students to generate for
# each. "id" is the major_programs.id that students reference.
MAJOR_PROGRAMS = [
    {"id": 1, "majorCode": "28", "entryYear": "2025", "name": "Khoa học Dữ liệu", "count": 110},
    {"id": 2, "majorCode": "11", "entryYear": "2025", "name": "Toán Ứng dụng", "count": 230},
]

SURNAMES = [
    "Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Huỳnh", "Phan", "Vũ", "Võ",
    "Đặng", "Bùi", "Đỗ", "Hồ", "Ngô", "Dương", "Lý", "Đinh", "Trương",
]

MIDDLE_MALE = ["Văn", "Hữu", "Đức", "Minh", "Quốc", "Hoàng", "Thành", "Anh", "Bá", "Công"]
MIDDLE_FEMALE = ["Thị", "Ngọc", "Thúy", "Thu", "Kim", "Hồng", "Mỹ", "Diễm", "Phương", "Bích"]

GIVEN_MALE = [
    "An", "Bình", "Cường", "Dũng", "Đạt", "Hải", "Hùng", "Khoa", "Long",
    "Nam", "Phúc", "Quân", "Sơn", "Tài", "Thắng", "Trí", "Tuấn", "Vinh", "Khang", "Bảo",
]
GIVEN_FEMALE = [
    "An", "Châu", "Dung", "Giang", "Hà", "Hằng", "Hương", "Lan", "Linh",
    "Mai", "Ngân", "Nhung", "Oanh", "Quỳnh", "Thảo", "Trang", "Uyên", "Vy", "Yến", "Như",
]


def random_name(gender):
    surname = random.choice(SURNAMES)
    if gender == "MALE":
        return f"{surname} {random.choice(MIDDLE_MALE)} {random.choice(GIVEN_MALE)}"
    return f"{surname} {random.choice(MIDDLE_FEMALE)} {random.choice(GIVEN_FEMALE)}"


def sql_escape(value):
    return value.replace("'", "''")


def generate():
    rows = []
    for program in MAJOR_PROGRAMS:
        prefix = program["entryYear"][-2:] + program["majorCode"]  # 4 chars
        for seq in range(1, program["count"] + 1):
            student_id = f"{prefix}{seq:04d}"  # 8 chars total
            gender = random.choice(["MALE", "FEMALE"])
            rows.append({
                "id": str(uuid.uuid4()),
                "student_id": student_id,
                "name": random_name(gender),
                "gender": gender,
                "major_program_id": program["id"],
            })
    return rows


def write_sql(rows, out_path):
    with open(out_path, "w", encoding="utf-8") as f:
        f.write("-- Auto-generated student seed data\n")
        f.write(f"-- {len(rows)} students across {len(MAJOR_PROGRAMS)} major programs\n")
        f.write("-- status is always STUDYING; gpa is left to the DB default.\n\n")
        # gpa omitted on purpose -> column DEFAULT (0.0) applies.
        f.write("INSERT INTO students "
                "(id, student_id, name, gender, major_program_id, status, created_at, updated_at)"
                "\nVALUES\n")
        values = []
        for r in rows:
            values.append(
                f"  ('{r['id']}', '{r['student_id']}', '{sql_escape(r['name'])}', "
                f"'{r['gender']}', {r['major_program_id']}, 'STUDYING', "
                f"NOW(), NOW())"
            )
        f.write(",\n".join(values))
        f.write(";\n")


def main():
    out_path = sys.argv[1] if len(sys.argv) > 1 else "students_seed.sql"
    random.seed()  # set a fixed int here for reproducible output if desired
    rows = generate()
    write_sql(rows, out_path)
    counts = ", ".join(f"program {m['id']}={m['count']}" for m in MAJOR_PROGRAMS)
    print(f"Wrote {len(rows)} students ({counts}) to {out_path}")


if __name__ == "__main__":
    main()
