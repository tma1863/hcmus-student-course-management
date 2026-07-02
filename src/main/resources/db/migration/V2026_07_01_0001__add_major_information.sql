-- Seed the foundational academic identities: majors and their entry-year programs.
-- Must run before student (V2026_07_02_0002) and curriculum (V2026_07_02_0003)
-- seeds, which reference major_program_id 1, 2 and 3.
-- Idempotent: clears both tables, then reseeds inside one transaction.
BEGIN;

TRUNCATE TABLE major_programs, majors RESTART IDENTITY CASCADE;

-- A major is a pure academic identity: a 2-char code and a display name.
-- The code matches the 3rd-4th digits of the student_id (e.g. 2520xxxx -> code 20).
INSERT INTO majors (id, major_code, name, created_at, updated_at) VALUES
  (1, '10', 'Kỹ sư phần mềm', NOW(), NOW()),
  (2, '20', 'Khoa học Dữ liệu', NOW(), NOW()),
  (3, '30', 'Hệ thống thông tin', NOW(), NOW());

-- One entry-year edition per major (all entry year 2025). The ids are pinned to
-- match the major_program_id already referenced by students and curriculum:
--   program 1 -> major code 20 (Khoa học Dữ liệu)   -> students 2520xxxx
--   program 2 -> major code 10 (Kỹ sư phần mềm)      -> students 2510xxxx
--   program 3 -> major code 30 (Hệ thống thông tin)  -> students 2530xxxx
INSERT INTO major_programs (id, major_id, entry_year, total_required_credit, total_optional_credit, created_at, updated_at) VALUES
  (1, 2, '2025', 120, 20, NOW(), NOW()),
  (2, 1, '2025', 120, 20, NOW(), NOW()),
  (3, 3, '2025', 120, 20, NOW(), NOW());

-- Keep the BIGSERIAL sequences ahead of the explicit ids we inserted.
SELECT setval('majors_id_seq', (SELECT MAX(id) FROM majors));
SELECT setval('major_programs_id_seq', (SELECT MAX(id) FROM major_programs));

COMMIT;
