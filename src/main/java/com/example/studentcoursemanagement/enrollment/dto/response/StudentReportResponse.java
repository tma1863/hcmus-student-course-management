package com.example.studentcoursemanagement.enrollment.dto.response;

import com.example.studentcoursemanagement.student.enums.EStudentStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * A student's academic report (transcript): identity, the cumulative aggregates, and the flat list
 * of attempted courses. The cumulative GPA is recomputed live from the enrollments using the same
 * rule as {@code EnrollmentService.recalculateGpa} (latest graded attempt per course,
 * credit-weighted).
 */
public record StudentReportResponse(
    UUID studentId,
    String studentCode,
    String name,
    String majorCode,
    String majorName,
    String entryYear,
    EStudentStatus status,
    BigDecimal cumulativeGpa,
    Integer totalCreditsEarned,
    List<TranscriptEntryResponse> courses) {}
