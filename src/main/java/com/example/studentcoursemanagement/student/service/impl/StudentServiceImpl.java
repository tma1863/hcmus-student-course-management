package com.example.studentcoursemanagement.student.service.impl;

import com.example.studentcoursemanagement.common.exception.ApiException;
import com.example.studentcoursemanagement.common.exception.ErrorCode;
import com.example.studentcoursemanagement.major.dao.MajorRepository;
import com.example.studentcoursemanagement.major.entity.Major;
import com.example.studentcoursemanagement.student.dao.StudentRepository;
import com.example.studentcoursemanagement.student.dto.request.CreateStudentRequest;
import com.example.studentcoursemanagement.student.dto.response.StudentResponse;
import com.example.studentcoursemanagement.student.entity.Student;
import com.example.studentcoursemanagement.student.enums.EStudentStatus;
import com.example.studentcoursemanagement.student.service.StudentService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;

@ApplicationScoped
public class StudentServiceImpl implements StudentService {

  @Inject StudentRepository studentRepository;
  @Inject MajorRepository majorRepository;

  @Override
  @Transactional
  public StudentResponse createStudent(CreateStudentRequest request) {
    String majorCode = request.majorCode().trim().toUpperCase();
    String entryYear = request.entryYear().trim();

    Major major =
        majorRepository
            .findByMajorCodeAndEntryYear(majorCode, entryYear)
            .orElseThrow(() -> new ApiException(ErrorCode.MAJOR_NOT_FOUND));

    Student student = new Student();
    student.name = request.name().trim();
    student.gender = request.gender();
    student.major = major;
    student.status = EStudentStatus.STUDYING;
    student.gpa = BigDecimal.ZERO;
    student.studentId = generateStudentId(entryYear, majorCode);

    studentRepository.persist(student);

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

  private String generateStudentId(String entryYear, String majorCode) {
    String yearSuffix = entryYear.substring(entryYear.length() - 2);
    String prefix = yearSuffix + majorCode;

    int nextSequence =
        studentRepository
            .findLatestStudentIdByPrefix(prefix)
            .filter(studentId -> studentId.length() == 8)
            .map(studentId -> Integer.parseInt(studentId.substring(4)) + 1)
            .orElse(1);

    return prefix + String.format("%04d", nextSequence);
  }
}

