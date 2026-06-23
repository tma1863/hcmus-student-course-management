package com.example.studentcoursemanagement.student.dao;

import com.example.studentcoursemanagement.student.entity.Student;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

@ApplicationScoped
public class StudentRepository implements PanacheRepository<Student> {

  public Optional<String> findLatestStudentIdByPrefix(String prefix) {
    return find("studentId like ?1 order by studentId desc", prefix + "%")
        .firstResultOptional()
        .map(student -> student.studentId);
  }
}
