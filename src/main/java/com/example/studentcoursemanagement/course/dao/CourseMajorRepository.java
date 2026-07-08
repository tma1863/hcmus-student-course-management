package com.example.studentcoursemanagement.course.dao;

import com.example.studentcoursemanagement.course.entity.CourseMajor;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityGraph;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class CourseMajorRepository implements PanacheRepository<CourseMajor> {

  public List<CourseMajor> listByMajorProgramIdWithDetails(Long majorProgramId) {
    //    EntityGraph<CourseMajor> graph = getEntityManager().createEntityGraph(CourseMajor.class);
    //    graph.addAttributeNodes("course", "prerequisites");

    EntityGraph<?> graph = getEntityManager().getEntityGraph("CourseMajor.withDetails");
    return find("majorProgram.id", Sort.by("programSemester"), majorProgramId)
        .withHint("jakarta.persistence.fetchgraph", graph)
        .list();
  }

  /** Curriculum rows of a program (no eager fetching) — used to recompute credit totals. */
  public List<CourseMajor> listByMajorProgramId(Long majorProgramId) {
    return list("majorProgram.id", majorProgramId);
  }

  /** A specific curriculum row identified by its program and the course business key. */
  public Optional<CourseMajor> findByProgramAndCourseId(Long majorProgramId, String courseId) {
    return find("majorProgram.id = ?1 and course.courseId = ?2", majorProgramId, courseId)
        .firstResultOptional();
  }

  public boolean existsByProgramAndCourseId(Long majorProgramId, String courseId) {
    return count("majorProgram.id = ?1 and course.courseId = ?2", majorProgramId, courseId) > 0;
  }
}
