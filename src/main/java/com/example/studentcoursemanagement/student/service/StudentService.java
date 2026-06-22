package com.example.studentcoursemanagement.student.service;

import com.example.studentcoursemanagement.student.dto.request.CreateStudentRequest;
import com.example.studentcoursemanagement.student.dto.response.StudentResponse;

public interface StudentService {
  StudentResponse createStudent(CreateStudentRequest request);
}

