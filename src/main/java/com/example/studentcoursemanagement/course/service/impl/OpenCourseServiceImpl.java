package com.example.studentcoursemanagement.course.service.impl;

import com.example.studentcoursemanagement.common.exception.ApiException;
import com.example.studentcoursemanagement.common.exception.ErrorCode;
import com.example.studentcoursemanagement.course.dao.OpenCourseRepository;
import com.example.studentcoursemanagement.course.entity.OpenCourse;
import com.example.studentcoursemanagement.course.service.OpenCourseService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class OpenCourseServiceImpl implements OpenCourseService {

  @Inject OpenCourseRepository openCourseRepository;

  @Override
  public OpenCourse getOpenCourseById(Long id) {
    return openCourseRepository
        .findByIdOptional(id)
        .orElseThrow(() -> new ApiException(ErrorCode.OPEN_COURSE_NOT_FOUND));
  }
}
