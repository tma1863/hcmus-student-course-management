package com.example.studentcoursemanagement.major.service;

import com.example.studentcoursemanagement.major.dto.request.CreateMajorRequest;
import com.example.studentcoursemanagement.major.dto.response.MajorResponse;
import com.example.studentcoursemanagement.major.entity.Major;

public interface MajorService {
  MajorResponse createMajor(CreateMajorRequest request);

  /** Returns the managed Major or throws ApiException(MAJOR_NOT_FOUND). */
  Major getMajorById(Long id);
}
