-- Distinguish required (mandatory) vs optional (elective) courses within a program.
-- Required courses carry 3-4 credits; optional courses carry 1-2 credits (enforced by
-- business logic, not a DB check, since the 3-10 / 1-2 split is policy rather than schema).
ALTER TABLE course_majors
    ADD COLUMN is_required BOOLEAN NOT NULL DEFAULT TRUE;

-- Drop the default so future inserts must state the type explicitly; the default existed
-- only to satisfy NOT NULL for any pre-existing rows.
ALTER TABLE course_majors
    ALTER COLUMN is_required DROP DEFAULT;
