package com.example.studentcoursemanagement.enrollment.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Records (or updates) the raw 0–10 score for an enrollment. The backend derives the pass/fail
 * status from the score and recomputes the student's GPA.
 */
public record RecordScoreRequest(
    @NotNull @DecimalMin("0.0") @DecimalMax("10.0") BigDecimal score) {}
