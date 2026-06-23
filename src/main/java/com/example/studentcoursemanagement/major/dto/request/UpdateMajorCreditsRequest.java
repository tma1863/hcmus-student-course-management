package com.example.studentcoursemanagement.major.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateMajorCreditsRequest(
    @NotNull @PositiveOrZero Integer totalRequiredCredit,
    @NotNull @PositiveOrZero Integer totalOptionalCredit) {}
