package com.example.studentcoursemanagement.enrollment.dao;

import com.example.studentcoursemanagement.course.entity.CourseMajor;
import com.example.studentcoursemanagement.course.entity.OpenCourse;
import com.example.studentcoursemanagement.enrollment.entity.Enrollment;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.Subgraph;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class EnrollmentRepository implements PanacheRepository<Enrollment> {

  /** All enrollments of a student, with the open course → course-major → course chain fetched. */
  public List<Enrollment> listByStudentIdWithDetails(UUID studentId) {
    return findWithDetails("student.id = ?1", studentId);
  }

  /** Graded enrollments only (score recorded), with the same eager fetch — used for GPA. */
  public List<Enrollment> listGradedByStudentIdWithDetails(UUID studentId) {
    return findWithDetails("student.id = ?1 and score is not null", studentId);
  }

  public Optional<Enrollment> findByStudentAndOpenCourse(UUID studentId, Long openCourseId) {
    return find("student.id = ?1 and openCourse.id = ?2", studentId, openCourseId)
        .firstResultOptional();
  }

  /** How many times this student has already enrolled in the given course (across all terms). */
  public long countAttempts(UUID studentId, Long courseId) {
    return count("student.id = ?1 and openCourse.courseMajor.course.id = ?2", studentId, courseId);
  }

  /**
   * Fetches the {@code openCourse → courseMajor → course} chain eagerly via an entity graph <em>for
   * this query only</em> (the mappings stay {@code LAZY}), avoiding N+1 selects when building the
   * report or recomputing GPA.
   */
  private List<Enrollment> findWithDetails(String query, Object... params) {
    EntityGraph<Enrollment> graph = getEntityManager().createEntityGraph(Enrollment.class);
    Subgraph<OpenCourse> openCourseGraph = graph.addSubgraph("openCourse");
    Subgraph<CourseMajor> courseMajorGraph = openCourseGraph.addSubgraph("courseMajor");
    courseMajorGraph.addAttributeNodes("course");

    return find(query, params).withHint("jakarta.persistence.fetchgraph", graph).list();
  }
}
