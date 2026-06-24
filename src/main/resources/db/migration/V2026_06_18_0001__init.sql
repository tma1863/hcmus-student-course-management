-- A major is a pure academic identity: a code and a display name.
CREATE TABLE majors (
	id BIGSERIAL PRIMARY KEY,
	major_code VARCHAR(2) NOT NULL UNIQUE,
	name VARCHAR(255) NOT NULL,
	created_at TIMESTAMP,
	updated_at TIMESTAMP
);

-- A major program is one entry-year edition of a major, carrying the
-- year-specific credit requirements. A student belongs to a program, not to
-- the bare major, because the entry year governs the curriculum that applies.
CREATE TABLE major_programs (
	id BIGSERIAL PRIMARY KEY,
	major_id BIGINT NOT NULL,
	entry_year VARCHAR(4) NOT NULL,
	total_required_credit INTEGER NOT NULL,
	total_optional_credit INTEGER NOT NULL,
	created_at TIMESTAMP,
	updated_at TIMESTAMP,
	CONSTRAINT fk_major_programs_major_id
		FOREIGN KEY (major_id)
		REFERENCES majors (id),
	CONSTRAINT uq_major_programs_major_entry_year
		UNIQUE (major_id, entry_year)
);

CREATE TABLE students (
	id UUID PRIMARY KEY,
	student_id VARCHAR(8) NOT NULL UNIQUE,
	name VARCHAR(255) NOT NULL,
	gender VARCHAR(255) NOT NULL,
	major_program_id BIGINT NOT NULL,
	status VARCHAR(255) NOT NULL,
	gpa NUMERIC(3, 2) NOT NULL,
	created_at TIMESTAMP,
	updated_at TIMESTAMP,
	CONSTRAINT fk_students_major_program_id
		FOREIGN KEY (major_program_id)
		REFERENCES major_programs (id)
);
