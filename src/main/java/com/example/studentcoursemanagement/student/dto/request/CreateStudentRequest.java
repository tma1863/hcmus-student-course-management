package com.example.studentcoursemanagement.student.dto.request;

import com.example.studentcoursemanagement.student.enums.EGender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateStudentRequest(
    @NotBlank String name, @NotNull EGender gender, @NotNull @Positive Long majorProgramId) {}
