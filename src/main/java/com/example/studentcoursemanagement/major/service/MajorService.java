package com.example.studentcoursemanagement.major.service;

import com.example.studentcoursemanagement.major.dto.request.CreateMajorRequest;
import com.example.studentcoursemanagement.major.dto.response.MajorResponse;

public interface MajorService {
  MajorResponse createMajor(CreateMajorRequest request);
}

