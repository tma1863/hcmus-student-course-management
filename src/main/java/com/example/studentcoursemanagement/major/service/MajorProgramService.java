package com.example.studentcoursemanagement.major.service;

import com.example.studentcoursemanagement.major.dto.request.CreateMajorProgramRequest;
import com.example.studentcoursemanagement.major.dto.response.MajorProgramReportResponse;
import com.example.studentcoursemanagement.major.dto.response.MajorProgramResponse;
import com.example.studentcoursemanagement.major.entity.MajorProgram;
import java.util.List;

public interface MajorProgramService {
  MajorProgramResponse createMajorProgram(Long majorId, CreateMajorProgramRequest request);

  List<MajorProgramResponse> getAllMajorPrograms(Long majorId);

  /** Full curriculum report grouped and ordered by recommended semester. */
  MajorProgramReportResponse getMajorProgramReport(Long majorId, Long id);

  /**
   * Recomputes and persists {@code totalRequiredCredit} / {@code totalOptionalCredit} from the
   * program's curriculum rows. Must be invoked after any add/remove of a course to the program.
   */
  void recalculateCredits(Long majorProgramId);

  /** Returns the managed MajorProgram or throws ApiException(MAJOR_PROGRAM_NOT_FOUND). */
  MajorProgram getMajorProgramById(Long id);

  /** Returns the managed MajorProgram, asserting it belongs to {@code majorId}. */
  MajorProgram getMajorProgramByMajorIdAndId(Long majorId, Long id);
}
