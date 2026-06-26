package com.example.studentcoursemanagement.major.dto.response;

import java.util.List;

/**
 * Full curriculum report of one major program: program metadata plus its courses grouped by
 * recommended semester (ascending).
 */
public record MajorProgramReportResponse(
    Long id,
    Long majorId,
    String majorCode,
    String name,
    String entryYear,
    Integer totalRequiredCredit,
    Integer totalOptionalCredit,
    List<SemesterReportResponse> semesters) {}
