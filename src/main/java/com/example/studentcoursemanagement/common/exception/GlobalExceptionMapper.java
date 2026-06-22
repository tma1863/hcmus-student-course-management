package com.example.studentcoursemanagement.common.exception;

import com.example.studentcoursemanagement.common.dto.ApiErrorResponse;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

  @Override
  public Response toResponse(Throwable exception) {
    if (exception instanceof ApiException apiException) {
      return Response.status(apiException.getErrorCode().getStatus())
          .type(MediaType.APPLICATION_JSON)
          .entity(new ApiErrorResponse(apiException.getMessage()))
          .build();
    }

    return Response.status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
        .type(MediaType.APPLICATION_JSON)
        .entity(new ApiErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR.getMessage()))
        .build();
  }
}
