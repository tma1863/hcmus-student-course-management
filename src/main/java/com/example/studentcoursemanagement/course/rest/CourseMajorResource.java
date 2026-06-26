package com.example.studentcoursemanagement.course.rest;

import com.example.studentcoursemanagement.course.dto.request.AddCourseToProgramRequest;
import com.example.studentcoursemanagement.course.dto.response.CourseMajorResponse;
import com.example.studentcoursemanagement.course.service.CourseMajorService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/majors/{majorId}/major-programs/{majorProgramId}/courses")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CourseMajorResource {

  @Inject CourseMajorService courseMajorService;

  @POST
  public Response addCourse(
      @PathParam("majorId") Long majorId,
      @PathParam("majorProgramId") Long majorProgramId,
      @Valid AddCourseToProgramRequest request) {
    CourseMajorResponse response =
        courseMajorService.addCourseToProgram(majorId, majorProgramId, request);
    return Response.status(Response.Status.CREATED).entity(response).build();
  }

  @DELETE
  @Path("/{courseId}")
  public Response removeCourse(
      @PathParam("majorId") Long majorId,
      @PathParam("majorProgramId") Long majorProgramId,
      @PathParam("courseId") String courseId) {
    courseMajorService.removeCourseFromProgram(majorId, majorProgramId, courseId);
    return Response.noContent().build();
  }
}
