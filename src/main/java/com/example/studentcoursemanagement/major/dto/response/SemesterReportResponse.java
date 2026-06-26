package com.example.studentcoursemanagement.major.dto.response;

import java.util.List;

/** All curriculum courses recommended for a single program semester. */
public record SemesterReportResponse(Integer semester, List<CourseReportResponse> courses) {}
