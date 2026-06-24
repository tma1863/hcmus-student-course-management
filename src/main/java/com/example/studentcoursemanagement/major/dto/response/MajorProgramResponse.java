package com.example.studentcoursemanagement.major.dto.response;

public record MajorProgramResponse(
    Long id,
    Long majorId,
    String majorCode,
    String name,
    String entryYear,
    Integer totalRequiredCredit,
    Integer totalOptionalCredit) {}
