package com.example.studentcoursemanagement.course.dao;

import com.example.studentcoursemanagement.course.entity.Course;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

@ApplicationScoped
public class CourseRepository implements PanacheRepository<Course> {

  /** Looks up a course by its user-facing business key (e.g. {@code "MTH00001"}). */
  public Optional<Course> findByCourseId(String courseId) {
    return find("courseId", courseId).firstResultOptional();
  }
}
