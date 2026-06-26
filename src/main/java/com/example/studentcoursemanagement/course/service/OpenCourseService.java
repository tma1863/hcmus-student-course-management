package com.example.studentcoursemanagement.course.service;

import com.example.studentcoursemanagement.course.entity.OpenCourse;

public interface OpenCourseService {

  /** Loads a managed open course by id, or throws {@code OPEN_COURSE_NOT_FOUND}. */
  OpenCourse getOpenCourseById(Long id);
}
