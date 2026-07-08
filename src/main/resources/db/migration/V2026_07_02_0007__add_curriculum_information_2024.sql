-- Curriculum for the 2024 entry-year programs added in V2026_07_02_0006.
-- Because programs 4/5/6 are the 2024 editions of the SAME majors as the 2025
-- programs 1/2/3, their curriculum is identical. Rather than regenerate it, we
-- copy each 2025 program's course_majors and prerequisites onto its 2024 twin:
--   program 1 -> 4   (Khoa học Dữ liệu)
--   program 2 -> 5   (Kỹ sư phần mềm)
--   program 3 -> 6   (Hệ thống thông tin)
-- Courses are global and already seeded (V2026_07_02_0003), so only the per-
-- program bridge rows (course_majors) and their prerequisite edges are added.
-- Idempotent: ON CONFLICT guards make a re-run a no-op.
BEGIN;

-- 1) Bridge rows: copy every course_major of the source program to its 2024 twin,
--    preserving course, credits, semester and required-flag; new BIGSERIAL ids.
INSERT INTO course_majors (course_id, major_program_id, credits, program_semester, is_required)
SELECT cm.course_id, tgt.new_pid, cm.credits, cm.program_semester, cm.is_required
FROM course_majors cm
JOIN (VALUES (1, 4), (2, 5), (3, 6)) AS tgt(src_pid, new_pid)
  ON cm.major_program_id = tgt.src_pid
ON CONFLICT ON CONSTRAINT uq_course_majors_course_major_program DO NOTHING;

SELECT setval('course_majors_id_seq', (SELECT MAX(id) FROM course_majors));

-- 2) Prerequisite edges: for each copied bridge row, replicate its source's
--    prerequisites. New and source rows are matched by (program, course); the
--    prerequisite_course_id is a global course id, so it carries over unchanged.
INSERT INTO course_major_prerequisites (course_major_id, prerequisite_course_id)
SELECT new_cm.id, p.prerequisite_course_id
FROM course_major_prerequisites p
JOIN course_majors old_cm ON old_cm.id = p.course_major_id
JOIN (VALUES (1, 4), (2, 5), (3, 6)) AS tgt(src_pid, new_pid)
  ON old_cm.major_program_id = tgt.src_pid
JOIN course_majors new_cm
  ON new_cm.major_program_id = tgt.new_pid
 AND new_cm.course_id = old_cm.course_id
ON CONFLICT DO NOTHING;

-- 3) Credit totals: mirror the source program's already-computed totals.
UPDATE major_programs tgt
SET total_required_credit = src.total_required_credit,
    total_optional_credit = src.total_optional_credit,
    updated_at = NOW()
FROM major_programs src
JOIN (VALUES (1, 4), (2, 5), (3, 6)) AS m(src_pid, new_pid) ON src.id = m.src_pid
WHERE tgt.id = m.new_pid;

COMMIT;
