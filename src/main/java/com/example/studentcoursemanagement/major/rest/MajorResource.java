package com.example.studentcoursemanagement.major.rest;

import com.example.studentcoursemanagement.major.dto.request.CreateMajorRequest;
import com.example.studentcoursemanagement.major.dto.response.MajorResponse;
import com.example.studentcoursemanagement.major.service.MajorService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/majors")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MajorResource {

  @Inject MajorService majorService;

  @POST
  public Response createMajor(@Valid CreateMajorRequest request) {
    MajorResponse response = majorService.createMajor(request);
    return Response.status(Response.Status.CREATED).entity(response).build();
  }
}

