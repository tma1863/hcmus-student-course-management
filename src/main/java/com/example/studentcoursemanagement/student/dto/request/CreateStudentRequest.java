package com.example.studentcoursemanagement.student.dto.request;

import com.example.studentcoursemanagement.student.enums.EGender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateStudentRequest(
    @NotBlank String name,
    @NotNull EGender gender,
    @NotBlank @Size(min = 4, max = 4) String entryYear,
    @NotBlank @Size(min = 2, max = 2) String majorCode) {}

