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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    MajorProgram program =
        majorProgramService.getMajorProgramByMajorIdAndId(majorId, majorProgramId);

    Course course = findCourse(request.courseId());
    if (courseMajorRepository.existsByProgramAndCourseId(majorProgramId, course.courseId)) {
      throw new ApiException(ErrorCode.COURSE_ALREADY_IN_PROGRAM);
    }

    List<Course> prerequisites = normalizePrerequisites(request.prerequisiteCourseIds(), course);
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

  @Override
  public List<CourseMajor> listProgramCurriculumWithDetails(Long majorProgramId) {
    return courseMajorRepository.listByMajorProgramIdWithDetails(majorProgramId);
  }

  @Override
  public List<CourseMajor> listProgramCurriculum(Long majorProgramId) {
    return courseMajorRepository.listByMajorProgramId(majorProgramId);
  }

  private Course findCourse(String courseId) {
    return courseRepository
        .findByCourseId(courseId.trim())
        .orElseThrow(() -> new ApiException(ErrorCode.COURSE_NOT_FOUND));
  }

  /**
   * Check and clean the list of courses requested from users input, including the de-duplicated and
   * order-preserving
   *
   * @param prerequisiteCourseIds
   * @param target
   * step 1: check null or empty, return empty list --> .filter()
   * step 2: trim the list and remove null or blank, de-duplicate and preserve order by using Set, LinkedHashSet --> in
   *     stream, use .distinct(). .trim()
   * step 3: check if any courseId in the list is equal to target.courseId, if yes, throw ApiException with ErrorCode.PREREQUISITE_CYCLE_DETECTED
   * step 4: find the course by courseId, if not found, throw ApiException with ErrorCode.COURSE_NOT_FOUND step 5: return the list of Course --> .map() and collect to list
   */
  private List<Course> normalizePrerequisites(List<String> prerequisiteCourseIds, Course target) {
    if (prerequisiteCourseIds == null) {
      return new ArrayList<>();
    }

    return prerequisiteCourseIds.stream()
        .filter(id -> id != null && !id.isBlank())
        .map(String::trim)
        .distinct()
        .map(
            id -> {
              if (id.equals(target.courseId)) {
                throw new ApiException(ErrorCode.PREREQUISITE_CYCLE_DETECTED);
              }
              return findCourse(id);
            })
        .collect(Collectors.toCollection(ArrayList::new));
  }

  /**

   */
  private void assertNoCycle(Long majorProgramId, Course newCourse, List<Course> prerequisites) {
    if (prerequisites.isEmpty()) {
      return;
    }
    Map<String, List<String>> requires = mapCoursesToPrerequisites(majorProgramId);
    for (Course prerequisite : prerequisites) {
      if (isPrerequisiteOf(requires, prerequisite.courseId, newCourse.courseId)) {
        throw new ApiException(ErrorCode.PREREQUISITE_CYCLE_DETECTED);
      }
    }
  }

  /** Adjacency map of "requires" edges for the program's existing curriculum rows. */
  private Map<String, List<String>> mapCoursesToPrerequisites(Long majorProgramId) {
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

  /** BFS*/
  private boolean isPrerequisiteOf(Map<String, List<String>> requires, String start, String target) {
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
