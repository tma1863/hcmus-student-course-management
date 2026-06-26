package com.example.studentcoursemanagement.common.exception;

import jakarta.ws.rs.core.Response;

public enum ErrorCode {
  MAJOR_NOT_FOUND(Response.Status.NOT_FOUND, "Major not found"),
  MAJOR_ALREADY_EXISTS(Response.Status.CONFLICT, "Major already exists"),
  MAJOR_PROGRAM_NOT_FOUND(Response.Status.NOT_FOUND, "Major program not found"),
  MAJOR_PROGRAM_ALREADY_EXISTS(Response.Status.CONFLICT, "Major program already exists"),
  STUDENT_NOT_FOUND(Response.Status.NOT_FOUND, "Student not found"),
  STUDENT_EMAIL_EXISTS(Response.Status.CONFLICT, "Student email already exists"),
  COURSE_NOT_FOUND(Response.Status.NOT_FOUND, "Course not found"),
  COURSE_ALREADY_IN_PROGRAM(
      Response.Status.CONFLICT, "Course already exists in this major program"),
  COURSE_NOT_IN_PROGRAM(
      Response.Status.NOT_FOUND, "Course is not part of this major program's curriculum"),
  PREREQUISITE_CYCLE_DETECTED(
      Response.Status.CONFLICT, "Adding these prerequisites would create a cyclic dependency"),
  OPEN_COURSE_NOT_FOUND(Response.Status.NOT_FOUND, "Open course not found"),
  OPEN_COURSE_NOT_OPEN(Response.Status.CONFLICT, "Open course is not open for enrollment"),
  OPEN_COURSE_FULL(Response.Status.CONFLICT, "Open course has no seats left"),
  ENROLLMENT_NOT_FOUND(Response.Status.NOT_FOUND, "Enrollment not found"),
  ALREADY_ENROLLED(Response.Status.CONFLICT, "Student is already enrolled in this open course"),
  INVALID_SCORE(Response.Status.BAD_REQUEST, "Score must be between 0 and 10"),
  VALIDATION_ERROR(Response.Status.BAD_REQUEST, "Validation error"),
  INTERNAL_SERVER_ERROR(Response.Status.INTERNAL_SERVER_ERROR, "Internal server error");

  private final Response.Status status;
  private final String message;

  ErrorCode(Response.Status status, String message) {
    this.status = status;
    this.message = message;
  }

  public Response.Status getStatus() {
    return status;
  }

  public String getMessage() {
    return message;
  }
}
