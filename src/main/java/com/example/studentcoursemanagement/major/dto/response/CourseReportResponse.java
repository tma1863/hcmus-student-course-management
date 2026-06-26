package com.example.studentcoursemanagement.major.dto.response;

import java.util.List;

/** One curriculum course within a semester of a major-program report. */
public record CourseReportResponse(
    String courseId,
    String name,
    Integer credits,
    Boolean isRequired,
    List<PrerequisiteResponse> prerequisites) {}
