package com.example.studentcoursemanagement.course.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Adds an existing course to a major program's curriculum.
 *
 * @param courseId the course business key to add (e.g. {@code "MTH00001"})
 * @param credits credit weight within this program (1–10)
 * @param programSemester recommended semester in the curriculum timeline (1–10)
 * @param isRequired {@code true} for a mandatory course, {@code false} for an elective
 * @param prerequisiteCourseIds business keys of courses that must be taken first (may be empty)
 */
public record AddCourseToProgramRequest(
    @NotBlank String courseId,
    @NotNull @Min(1) @Max(10) Integer credits,
    @NotNull @Min(1) @Max(10) Integer programSemester,
    @NotNull Boolean isRequired,
    List<String> prerequisiteCourseIds) {}
