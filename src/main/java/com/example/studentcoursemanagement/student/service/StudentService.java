package com.example.studentcoursemanagement.student.service;

import com.example.studentcoursemanagement.student.dto.request.CreateStudentRequest;
import com.example.studentcoursemanagement.student.dto.response.StudentResponse;
import com.example.studentcoursemanagement.student.entity.Student;
import java.util.UUID;

public interface StudentService {
  StudentResponse createStudent(CreateStudentRequest request);

  /** Loads a managed student by id, or throws {@code STUDENT_NOT_FOUND}. */
  Student getStudentById(UUID id);
}
