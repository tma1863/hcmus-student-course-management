package com.example.studentcoursemanagement.student.mapper;

import com.example.studentcoursemanagement.student.dto.response.StudentResponse;
import com.example.studentcoursemanagement.student.entity.Student;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class StudentMapper {

  public StudentResponse toResponse(Student student) {
    return new StudentResponse(
        student.id,
        student.studentId,
        student.name,
        student.gender,
        student.major.majorCode,
        student.major.entryYear,
        student.status,
        student.gpa);
  }
}
