package com.example.studentcoursemanagement.major.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMajorRequest(
    @NotBlank @Size(min = 2, max = 2) String majorCode,
    @NotBlank @Size(min = 4, max = 4) String entryYear,
    @NotBlank String name) {}

