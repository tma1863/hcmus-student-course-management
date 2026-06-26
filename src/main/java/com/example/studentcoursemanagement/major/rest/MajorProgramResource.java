package com.example.studentcoursemanagement.major.rest;

import com.example.studentcoursemanagement.major.dto.request.CreateMajorProgramRequest;
import com.example.studentcoursemanagement.major.dto.request.UpdateMajorProgramCreditsRequest;
import com.example.studentcoursemanagement.major.dto.response.MajorProgramResponse;
import com.example.studentcoursemanagement.major.service.MajorProgramService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/majors/{majorId}/major-programs")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MajorProgramResource {

  @Inject MajorProgramService majorProgramService;

  @POST
  public Response createMajorProgram(
          @PathParam("majorId") Long majorId, @Valid CreateMajorProgramRequest request) {
    MajorProgramResponse response = majorProgramService.createMajorProgram(majorId, request);
    return Response.status(Response.Status.CREATED).entity(response).build();
  }

  @PUT
  @Path("/{id}/credits")
  public Response updateCredits(
          @PathParam("majorId") Long majorId,
          @PathParam("id") Long id, @Valid UpdateMajorProgramCreditsRequest request) {
    MajorProgramResponse response = majorProgramService.updateCredits(majorId, id, request);
    return Response.ok(response).build();
  }

  @GET
  public Response getAllMajorPrograms(
            @PathParam("majorId") Long majorId) {
    return Response.ok(majorProgramService.getAllMajorPrograms(majorId)).build();
  }
}
