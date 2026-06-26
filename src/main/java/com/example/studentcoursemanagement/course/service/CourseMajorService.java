package com.example.studentcoursemanagement.course.service;

import com.example.studentcoursemanagement.course.dto.request.AddCourseToProgramRequest;
import com.example.studentcoursemanagement.course.dto.response.CourseMajorResponse;
import com.example.studentcoursemanagement.course.entity.CourseMajor;
import java.util.List;

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

  /**
   * All curriculum rows of a program, ordered by recommended semester, with {@code course} and
   * {@code prerequisites} eagerly loaded — for building a program's curriculum report.
   */
  List<CourseMajor> listProgramCurriculumWithDetails(Long majorProgramId);

  /** Curriculum rows of a program (no eager fetching) — for recomputing credit totals. */
  List<CourseMajor> listProgramCurriculum(Long majorProgramId);
}
