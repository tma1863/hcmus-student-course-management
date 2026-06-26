package com.example.studentcoursemanagement.enrollment.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Enrolls a student in an offered class.
 *
 * @param studentId the student's primary key
 * @param openCourseId the open course to enroll in (must be {@code OPEN} with seats left)
 */
public record CreateEnrollmentRequest(
    @NotNull UUID studentId, @NotNull Long openCourseId) {}
