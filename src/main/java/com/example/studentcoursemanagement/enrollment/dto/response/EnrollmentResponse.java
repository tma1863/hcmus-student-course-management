package com.example.studentcoursemanagement.enrollment.dto.response;

import com.example.studentcoursemanagement.enrollment.enums.EEnrollmentStatus;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * A single enrollment with its resolved course context and derived grade. {@code score},
 * {@code letter} and {@code gradePoint} are {@code null} while the enrollment is {@code ENROLLED}.
 *
 * @param term the operational term within the academic year ({@code OpenCourse.semester}, 1 or 2)
 */
public record EnrollmentResponse(
    Long id,
    UUID studentId,
    String studentCode,
    Long openCourseId,
    String courseId,
    String courseName,
    Integer credits,
    Integer programSemester,
    String academicYear,
    Integer term,
    BigDecimal score,
    String letter,
    BigDecimal gradePoint,
    EEnrollmentStatus status,
    Integer attemptNumber) {}
