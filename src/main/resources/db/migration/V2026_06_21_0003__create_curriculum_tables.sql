-- Global course catalog
CREATE TABLE courses (
	id BIGSERIAL PRIMARY KEY,
	course_id VARCHAR(8) NOT NULL UNIQUE,
	name VARCHAR(255) NOT NULL,
	created_at TIMESTAMP,
	updated_at TIMESTAMP
);

CREATE INDEX idx_courses_course_id ON courses (course_id);

-- Curriculum bridge: which course belongs to which major program, with payload
CREATE TABLE course_majors (
	id BIGSERIAL PRIMARY KEY,
	course_id BIGINT NOT NULL,
	major_program_id BIGINT NOT NULL,
	credits INTEGER NOT NULL,
	program_semester INTEGER NOT NULL,
	created_at TIMESTAMP,
	updated_at TIMESTAMP,
	CONSTRAINT fk_course_majors_course_id
		FOREIGN KEY (course_id)
		REFERENCES courses (id),
	CONSTRAINT fk_course_majors_major_program_id
		FOREIGN KEY (major_program_id)
		REFERENCES major_programs (id),
	CONSTRAINT uq_course_majors_course_major_program
		UNIQUE (course_id, major_program_id)
);

-- Prerequisites: a CourseMajor entry requires foundational Course(s)
CREATE TABLE course_major_prerequisites (
	course_major_id BIGINT NOT NULL,
	prerequisite_course_id BIGINT NOT NULL,
	PRIMARY KEY (course_major_id, prerequisite_course_id),
	CONSTRAINT fk_cmp_course_major_id
		FOREIGN KEY (course_major_id)
		REFERENCES course_majors (id)
		ON DELETE CASCADE,
	CONSTRAINT fk_cmp_prerequisite_course_id
		FOREIGN KEY (prerequisite_course_id)
		REFERENCES courses (id)
);

-- Operational offered classes
CREATE TABLE open_courses (
	id BIGSERIAL PRIMARY KEY,
	course_major_id BIGINT NOT NULL,
	semester INTEGER NOT NULL,
	academic_year VARCHAR(9) NOT NULL,
	max_students INTEGER NOT NULL,
	enrolled_count INTEGER NOT NULL DEFAULT 0,
	status VARCHAR(255) NOT NULL,
	created_at TIMESTAMP,
	updated_at TIMESTAMP,
	CONSTRAINT fk_open_courses_course_major_id
		FOREIGN KEY (course_major_id)
		REFERENCES course_majors (id)
);
