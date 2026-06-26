package com.example.studentcoursemanagement.student.rest;

import com.example.studentcoursemanagement.enrollment.service.StudentReportService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.UUID;

@Path("/api/students/{studentId}/report")
@Produces(MediaType.APPLICATION_JSON)
public class StudentReportResource {

  @Inject StudentReportService studentReportService;

  @GET
  public Response getReport(@PathParam("studentId") UUID studentId) {
    return Response.ok(studentReportService.getReport(studentId)).build();
  }
}
