package com.example.studentcoursemanagement.course.service.impl;

import com.example.studentcoursemanagement.common.exception.ApiException;
import com.example.studentcoursemanagement.common.exception.ErrorCode;
import com.example.studentcoursemanagement.course.dao.CourseMajorRepository;
import com.example.studentcoursemanagement.course.dao.CourseRepository;
import com.example.studentcoursemanagement.course.dto.request.AddCourseToProgramRequest;
import com.example.studentcoursemanagement.course.dto.response.CourseMajorResponse;
import com.example.studentcoursemanagement.course.entity.Course;
import com.example.studentcoursemanagement.course.entity.CourseMajor;
import com.example.studentcoursemanagement.course.mapper.CourseMajorMapper;
import com.example.studentcoursemanagement.course.service.CourseMajorService;
import com.example.studentcoursemanagement.major.entity.MajorProgram;
import com.example.studentcoursemanagement.major.service.MajorProgramService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class CourseMajorServiceImpl implements CourseMajorService {

  @Inject CourseRepository courseRepository;
  @Inject CourseMajorRepository courseMajorRepository;
  @Inject CourseMajorMapper courseMajorMapper;
  @Inject MajorProgramService majorProgramService;

  @Override
  @Transactional
  public CourseMajorResponse addCourseToProgram(
      Long majorId, Long majorProgramId, AddCourseToProgramRequest request) {
    MajorProgram program = majorProgramService.getMajorProgramByMajorIdAndId(majorId, majorProgramId);

    Course course = findCourse(request.courseId());
    if (courseMajorRepository.existsByProgramAndCourseId(majorProgramId, course.courseId)) {
      throw new ApiException(ErrorCode.COURSE_ALREADY_IN_PROGRAM);
    }

    List<Course> prerequisites = resolvePrerequisites(request.prerequisiteCourseIds(), course);
    assertNoCycle(majorProgramId, course, prerequisites);

    CourseMajor courseMajor =
        CourseMajor.builder()
            .course(course)
            .majorProgram(program)
            .credits(request.credits())
            .programSemester(request.programSemester())
            .isRequired(request.isRequired())
            .prerequisites(prerequisites)
            .build();
    courseMajorRepository.persist(courseMajor);

    majorProgramService.recalculateCredits(majorProgramId);

    return courseMajorMapper.toResponse(courseMajor);
  }

  @Override
  @Transactional
  public void removeCourseFromProgram(Long majorId, Long majorProgramId, String courseId) {
    majorProgramService.getMajorProgramByMajorIdAndId(majorId, majorProgramId);

    CourseMajor courseMajor =
        courseMajorRepository
            .findByProgramAndCourseId(majorProgramId, courseId.trim())
            .orElseThrow(() -> new ApiException(ErrorCode.COURSE_NOT_IN_PROGRAM));

    courseMajorRepository.delete(courseMajor);

    majorProgramService.recalculateCredits(majorProgramId);
  }

  private Course findCourse(String courseId) {
    return courseRepository
        .findByCourseId(courseId.trim())
        .orElseThrow(() -> new ApiException(ErrorCode.COURSE_NOT_FOUND));
  }

  /** Resolves prerequisite business keys to managed courses, de-duplicated and order-preserving. */
  private List<Course> resolvePrerequisites(List<String> prerequisiteCourseIds, Course target) {
    if (prerequisiteCourseIds == null || prerequisiteCourseIds.isEmpty()) {
      return new ArrayList<>();
    }
    Set<String> keys = new LinkedHashSet<>();
    for (String key : prerequisiteCourseIds) {
      if (key != null && !key.isBlank()) {
        keys.add(key.trim());
      }
    }
    List<Course> prerequisites = new ArrayList<>();
    for (String key : keys) {
      // A course requiring itself is a trivial 1-node cycle.
      if (key.equals(target.courseId)) {
        throw new ApiException(ErrorCode.PREREQUISITE_CYCLE_DETECTED);
      }
      prerequisites.add(findCourse(key));
    }
    return prerequisites;
  }

  /**
   * Rejects prerequisites that would introduce a cyclic dependency within this program. Edge {@code
   * A -> B} means "A requires B". Adding {@code newCourse -> p} forms a cycle iff {@code p} can
   * already reach {@code newCourse} along existing requirement edges.
   */
  private void assertNoCycle(Long majorProgramId, Course newCourse, List<Course> prerequisites) {
    if (prerequisites.isEmpty()) {
      return;
    }
    Map<String, List<String>> requires = buildRequirementGraph(majorProgramId);
    for (Course prerequisite : prerequisites) {
      if (canReach(requires, prerequisite.courseId, newCourse.courseId)) {
        throw new ApiException(ErrorCode.PREREQUISITE_CYCLE_DETECTED);
      }
    }
  }

  /** Adjacency map of "requires" edges for the program's existing curriculum rows. */
  private Map<String, List<String>> buildRequirementGraph(Long majorProgramId) {
    Map<String, List<String>> requires = new HashMap<>();
    for (CourseMajor row : courseMajorRepository.listByMajorProgramIdWithDetails(majorProgramId)) {
      List<String> targets =
          row.prerequisites.stream().map(prerequisite -> prerequisite.courseId).toList();
      if (!targets.isEmpty()) {
        requires.computeIfAbsent(row.course.courseId, key -> new ArrayList<>()).addAll(targets);
      }
    }
    return requires;
  }

  /** BFS: is {@code target} reachable from {@code start} following requirement edges? */
  private boolean canReach(Map<String, List<String>> requires, String start, String target) {
    Set<String> visited = new HashSet<>();
    Deque<String> queue = new ArrayDeque<>();
    queue.add(start);
    while (!queue.isEmpty()) {
      String current = queue.poll();
      if (current.equals(target)) {
        return true;
      }
      if (visited.add(current)) {
        queue.addAll(requires.getOrDefault(current, List.of()));
      }
    }
    return false;
  }
}
