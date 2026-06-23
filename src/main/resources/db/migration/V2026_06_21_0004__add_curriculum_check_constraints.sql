ALTER TABLE course_majors
    ADD CONSTRAINT chk_course_majors_credits
    CHECK (credits BETWEEN 1 AND 10);

ALTER TABLE course_majors
    ADD CONSTRAINT chk_course_majors_semester
    CHECK (semester IN (1, 2));

ALTER TABLE open_courses
    ADD CONSTRAINT chk_open_courses_semester
    CHECK (semester IN (1, 2));

ALTER TABLE open_courses
    ADD CONSTRAINT chk_open_courses_status
    CHECK (status IN ('OPEN', 'FULL', 'CLOSED'));

ALTER TABLE open_courses
    ADD CONSTRAINT chk_open_courses_capacity
    CHECK (max_students > 0);

ALTER TABLE open_courses
    ADD CONSTRAINT chk_open_courses_enrolled
    CHECK (enrolled_count >= 0 AND enrolled_count <= max_students);
