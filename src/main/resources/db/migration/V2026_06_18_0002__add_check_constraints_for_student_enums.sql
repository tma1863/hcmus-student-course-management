ALTER TABLE students
    ADD CONSTRAINT chk_students_gender
    CHECK (gender IN ('MALE', 'FEMALE'));

ALTER TABLE students
    ADD CONSTRAINT chk_students_status
    CHECK (status IN ('STUDYING', 'GRADUATED', 'DROPPED_OUT'));

