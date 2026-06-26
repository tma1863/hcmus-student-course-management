package com.example.studentcoursemanagement.course.service;

import com.example.studentcoursemanagement.course.dto.request.AddCourseToProgramRequest;
import com.example.studentcoursemanagement.course.dto.response.CourseMajorResponse;

public interface CourseMajorService {

  /**
   * Adds a course to a program's curriculum, wires its prerequisites (rejecting cyclic ones), and
   * recomputes the program's credit totals.
   */
  CourseMajorResponse addCourseToProgram(
      Long majorId, Long majorProgramId, AddCourseToProgramRequest request);

  /**
   * Removes a course from a program's curriculum (by course business key) and recomputes the
   * program's credit totals.
   */
  void removeCourseFromProgram(Long majorId, Long majorProgramId, String courseId);
}
