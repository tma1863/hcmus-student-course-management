-- Student enrollments: which offered class (open_course) a student took, and the score earned.
-- A retake is a separate row against a later same-parity open course (different term, so the
-- (student_id, open_course_id) uniqueness still holds); attempt_number tells them apart.
CREATE TABLE enrollments (
	id BIGSERIAL PRIMARY KEY,
	student_id UUID NOT NULL,
	open_course_id BIGINT NOT NULL,
	score NUMERIC(4, 2),
	status VARCHAR(255) NOT NULL,
	attempt_number INTEGER NOT NULL DEFAULT 1,
	created_at TIMESTAMP,
	updated_at TIMESTAMP,
	CONSTRAINT fk_enrollments_student_id
		FOREIGN KEY (student_id)
		REFERENCES students (id),
	CONSTRAINT fk_enrollments_open_course_id
		FOREIGN KEY (open_course_id)
		REFERENCES open_courses (id),
	CONSTRAINT uq_enrollments_student_open_course
		UNIQUE (student_id, open_course_id),
	CONSTRAINT ck_enrollments_score
		CHECK (score IS NULL OR (score >= 0 AND score <= 10)),
	CONSTRAINT ck_enrollments_status
		CHECK (status IN ('ENROLLED', 'COMPLETED', 'FAILED')),
	CONSTRAINT ck_enrollments_attempt_number
		CHECK (attempt_number >= 1)
);

-- Report lookups fetch all enrollments of one student.
CREATE INDEX idx_enrollments_student_id ON enrollments (student_id);
