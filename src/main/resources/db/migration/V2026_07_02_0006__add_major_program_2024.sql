-- Add the 2024 entry-year editions of the same three majors seeded for 2025 in
-- V2026_07_01_0001. The majors themselves (Kỹ sư phần mềm, Khoa học Dữ liệu,
-- Hệ thống thông tin) are reused as-is; only new major_programs rows are added.
--
-- ids are pinned to 4/5/6 and mirror the 2025 major->program mapping so the two
-- cohorts line up predictably:
--   program 1 (2025) & 4 (2024) -> major_id 2 (Khoa học Dữ liệu)   -> ids 2520.. / 2420..
--   program 2 (2025) & 5 (2024) -> major_id 1 (Kỹ sư phần mềm)      -> ids 2510.. / 2410..
--   program 3 (2025) & 6 (2024) -> major_id 3 (Hệ thống thông tin)  -> ids 2530.. / 2430..
--
-- Idempotent: ON CONFLICT on the (major_id, entry_year) unique key makes a re-run
-- a no-op, and setval keeps the BIGSERIAL sequence ahead of the explicit ids.
BEGIN;

INSERT INTO major_programs (id, major_id, entry_year, total_required_credit, total_optional_credit, created_at, updated_at) VALUES
  (4, 2, '2024', 120, 20, NOW(), NOW()),
  (5, 1, '2024', 120, 20, NOW(), NOW()),
  (6, 3, '2024', 120, 20, NOW(), NOW())
ON CONFLICT ON CONSTRAINT uq_major_programs_major_entry_year DO NOTHING;

-- Keep the sequence ahead of the explicit ids we inserted.
SELECT setval('major_programs_id_seq', (SELECT MAX(id) FROM major_programs));

COMMIT;
