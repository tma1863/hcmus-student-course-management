package com.example.studentcoursemanagement.enrollment.rest;

import com.example.studentcoursemanagement.enrollment.dto.request.CreateEnrollmentRequest;
import com.example.studentcoursemanagement.enrollment.dto.request.RecordScoreRequest;
import com.example.studentcoursemanagement.enrollment.dto.response.EnrollmentResponse;
import com.example.studentcoursemanagement.enrollment.service.EnrollmentService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/enrollments")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class EnrollmentResource {

  @Inject EnrollmentService enrollmentService;

  @POST
  public Response enroll(@Valid CreateEnrollmentRequest request) {
    EnrollmentResponse response = enrollmentService.enroll(request);
    return Response.status(Response.Status.CREATED).entity(response).build();
  }

  @PATCH
  @Path("/{id}/score")
  public Response recordScore(@PathParam("id") Long id, @Valid RecordScoreRequest request) {
    EnrollmentResponse response = enrollmentService.recordScore(id, request);
    return Response.ok(response).build();
  }
}
