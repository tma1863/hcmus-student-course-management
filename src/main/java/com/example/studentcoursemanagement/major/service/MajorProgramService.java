package com.example.studentcoursemanagement.major.service;

import com.example.studentcoursemanagement.major.dto.request.CreateMajorProgramRequest;
import com.example.studentcoursemanagement.major.dto.request.UpdateMajorProgramCreditsRequest;
import com.example.studentcoursemanagement.major.dto.response.MajorProgramResponse;
import com.example.studentcoursemanagement.major.entity.MajorProgram;
import java.util.List;

public interface MajorProgramService {
  MajorProgramResponse createMajorProgram(Long majorId, CreateMajorProgramRequest request);

  MajorProgramResponse updateCredits(
      Long majorId, Long id, UpdateMajorProgramCreditsRequest request);

  List<MajorProgramResponse> getAllMajorPrograms(Long majorId);

  /** Returns the managed MajorProgram or throws ApiException(MAJOR_PROGRAM_NOT_FOUND). */
  MajorProgram getMajorProgramById(Long id);
}
