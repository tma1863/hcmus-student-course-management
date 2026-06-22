CREATE TABLE majors (
	id BIGSERIAL PRIMARY KEY,
	major_code VARCHAR(2) NOT NULL,
	name VARCHAR(255) NOT NULL,
	entry_year VARCHAR(4) NOT NULL,
	total_required_credit INTEGER NOT NULL,
	total_optional_credit INTEGER NOT NULL,
	created_at TIMESTAMP,
	updated_at TIMESTAMP,
	CONSTRAINT uq_majors_major_code_entry_year UNIQUE (major_code, entry_year)
);

CREATE TABLE students (
	id UUID PRIMARY KEY,
	student_id VARCHAR(8) NOT NULL UNIQUE,
	name VARCHAR(255) NOT NULL,
	gender VARCHAR(255) NOT NULL,
	major_id BIGINT NOT NULL,
	status VARCHAR(255) NOT NULL,
	gpa NUMERIC(3, 2) NOT NULL,
	created_at TIMESTAMP,
	updated_at TIMESTAMP,
	CONSTRAINT fk_students_major_id
		FOREIGN KEY (major_id)
		REFERENCES majors (id)
);

