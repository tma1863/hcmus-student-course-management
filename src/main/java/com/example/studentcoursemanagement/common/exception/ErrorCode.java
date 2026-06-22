package com.example.studentcoursemanagement.common.exception;

import jakarta.ws.rs.core.Response;

public enum ErrorCode {
  MAJOR_NOT_FOUND(Response.Status.NOT_FOUND, "Major not found"),
  MAJOR_ALREADY_EXISTS(Response.Status.CONFLICT, "Major already exists"),
  STUDENT_NOT_FOUND(Response.Status.NOT_FOUND, "Student not found"),
  STUDENT_EMAIL_EXISTS(Response.Status.CONFLICT, "Student email already exists"),
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
