package com.example.studentcoursemanagement.student.rest;

import com.example.studentcoursemanagement.student.dto.request.CreateStudentRequest;
import com.example.studentcoursemanagement.student.dto.response.StudentResponse;
import com.example.studentcoursemanagement.student.service.StudentService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/students")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class StudentResource {

  @Inject StudentService studentService;

  @POST
  public Response createStudent(@Valid CreateStudentRequest request) {
    StudentResponse response = studentService.createStudent(request);
    return Response.status(Response.Status.CREATED).entity(response).build();
  }
}
