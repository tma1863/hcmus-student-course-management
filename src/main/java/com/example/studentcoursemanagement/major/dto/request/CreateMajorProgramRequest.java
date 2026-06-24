package com.example.studentcoursemanagement.major.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateMajorProgramRequest(
    @NotNull @Positive Long majorId, @NotNull @Size(min = 4, max = 4) String entryYear) {}
