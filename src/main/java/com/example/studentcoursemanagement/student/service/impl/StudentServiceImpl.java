package com.example.studentcoursemanagement.student.service.impl;

import com.example.studentcoursemanagement.major.entity.MajorProgram;
import com.example.studentcoursemanagement.major.service.MajorProgramService;
import com.example.studentcoursemanagement.student.dao.StudentRepository;
import com.example.studentcoursemanagement.student.dto.request.CreateStudentRequest;
import com.example.studentcoursemanagement.student.dto.response.StudentResponse;
import com.example.studentcoursemanagement.student.entity.Student;
import com.example.studentcoursemanagement.student.enums.EStudentStatus;
import com.example.studentcoursemanagement.student.mapper.StudentMapper;
import com.example.studentcoursemanagement.student.service.StudentService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;

@ApplicationScoped
public class StudentServiceImpl implements StudentService {

  @Inject StudentRepository studentRepository;
  @Inject MajorProgramService majorProgramService;
  @Inject StudentMapper studentMapper;

  @Override
  @Transactional
  public StudentResponse createStudent(CreateStudentRequest request) {
    MajorProgram majorProgram = majorProgramService.getMajorProgramById(request.majorProgramId());

    Student student =
        Student.builder()
            .name(request.name().trim())
            .gender(request.gender())
            .majorProgram(majorProgram)
            .status(EStudentStatus.STUDYING)
            .gpa(BigDecimal.ZERO.setScale(2))
            .studentId(generateStudentId(majorProgram.entryYear, majorProgram.major.majorCode))
            .build();

    studentRepository.persist(student);

    return studentMapper.toResponse(student);
  }

  private String generateStudentId(String entryYear, String majorCode) {
    String prefix = entryYear.substring(entryYear.length() - 2) + majorCode;

    int nextSequence =
        studentRepository
            .findLatestStudentIdByPrefix(prefix)
            .filter(studentId -> studentId.length() == 8)
            .map(studentId -> Integer.parseInt(studentId.substring(4)) + 1)
            .orElse(1);

    return prefix + String.format("%04d", nextSequence);
  }
}
