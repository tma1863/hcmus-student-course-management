package com.example.studentcoursemanagement.course.dto.response;

import java.util.List;

/** A single curriculum row (course within a program) returned after a curriculum mutation. */
public record CourseMajorResponse(
    Long id,
    Long majorProgramId,
    String courseId,
    String courseName,
    Integer credits,
    Integer programSemester,
    Boolean isRequired,
    List<String> prerequisiteCourseIds) {}
