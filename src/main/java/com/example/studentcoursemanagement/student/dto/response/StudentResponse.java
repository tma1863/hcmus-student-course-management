package com.example.studentcoursemanagement.student.dto.response;

import com.example.studentcoursemanagement.student.enums.EGender;
import com.example.studentcoursemanagement.student.enums.EStudentStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record StudentResponse(
    UUID id,
    String studentId,
    String name,
    EGender gender,
    String majorCode,
    String entryYear,
    EStudentStatus status,
    BigDecimal gpa) {}

