package com.example.studentcoursemanagement.student.dao;

import com.example.studentcoursemanagement.student.entity.Student;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import java.util.UUID;

// Student's id is a UUID, so the repository is keyed on UUID (not the Long default of
// PanacheRepository) — this makes findByIdOptional(UUID) and friends type-correct.
@ApplicationScoped
public class StudentRepository implements PanacheRepositoryBase<Student, UUID> {

  public Optional<String> findLatestStudentIdByPrefix(String prefix) {
    return find("studentId like ?1 order by studentId desc", prefix + "%")
        .firstResultOptional()
        .map(student -> student.studentId);
  }
}
