package com.example.studentcoursemanagement.major.dto.response;

public record MajorResponse(
    Long id,
    String majorCode,
    String entryYear,
    String name,
    Integer totalRequiredCredit,
    Integer totalOptionalCredit) {}
