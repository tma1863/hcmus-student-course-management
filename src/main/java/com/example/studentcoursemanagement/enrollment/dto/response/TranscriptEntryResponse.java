package com.example.studentcoursemanagement.enrollment.dto.response;

import com.example.studentcoursemanagement.enrollment.enums.EEnrollmentStatus;
import java.math.BigDecimal;

/**
 * One attempt at one course in the transcript. All attempts are listed for transparency; {@code
 * countedInGpa} marks the single attempt (the latest graded one) that contributes to the GPA and
 * credit totals. {@code score}/{@code letter}/{@code gradePoint} are {@code null} for an {@code
 * ENROLLED} (not-yet-graded) attempt.
 */
public record TranscriptEntryResponse(
    String courseId,
    String courseName,
    Integer credits,
    Integer attemptNumber,
    BigDecimal score,
    String letter,
    BigDecimal gradePoint,
    EEnrollmentStatus status,
    boolean countedInGpa) {}
