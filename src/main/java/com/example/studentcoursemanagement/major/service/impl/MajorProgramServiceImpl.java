package com.example.studentcoursemanagement.major.service.impl;

import com.example.studentcoursemanagement.common.exception.ApiException;
import com.example.studentcoursemanagement.common.exception.ErrorCode;
import com.example.studentcoursemanagement.course.entity.CourseMajor;
import com.example.studentcoursemanagement.course.service.CourseMajorService;
import com.example.studentcoursemanagement.major.dao.MajorProgramRepository;
import com.example.studentcoursemanagement.major.dto.request.CreateMajorProgramRequest;
import com.example.studentcoursemanagement.major.dto.response.CourseReportResponse;
import com.example.studentcoursemanagement.major.dto.response.MajorProgramReportResponse;
import com.example.studentcoursemanagement.major.dto.response.MajorProgramResponse;
import com.example.studentcoursemanagement.major.dto.response.SemesterReportResponse;
import com.example.studentcoursemanagement.major.entity.Major;
import com.example.studentcoursemanagement.major.entity.MajorProgram;
import com.example.studentcoursemanagement.major.mapper.MajorProgramMapper;
import com.example.studentcoursemanagement.major.mapper.MajorProgramReportMapper;
import com.example.studentcoursemanagement.major.service.MajorProgramService;
import com.example.studentcoursemanagement.major.service.MajorService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@ApplicationScoped
public class MajorProgramServiceImpl implements MajorProgramService {

  private static final Comparator<CourseMajor> CURRICULUM_ORDER =
      Comparator.comparing((CourseMajor cm) -> cm.isRequired, Comparator.reverseOrder())
          .thenComparing(cm -> cm.course.courseId);

  @Inject MajorProgramRepository majorProgramRepository;
  @Inject CourseMajorService courseMajorService;
  @Inject MajorProgramMapper majorProgramMapper;
  @Inject MajorProgramReportMapper majorProgramReportMapper;
  @Inject MajorService majorService;

  @Override
  @Transactional
  public MajorProgramResponse createMajorProgram(Long majorId, CreateMajorProgramRequest request) {
    Major major = majorService.getMajorById(majorId);
    String entryYear = request.entryYear().trim();

    if (majorProgramRepository.existsByMajorIdAndEntryYear(major.id, entryYear)) {
      throw new ApiException(ErrorCode.MAJOR_PROGRAM_ALREADY_EXISTS);
    }

    MajorProgram majorProgram =
        MajorProgram.builder()
            .major(major)
            .entryYear(entryYear)
            .totalRequiredCredit(0)
            .totalOptionalCredit(0)
            .build();

    majorProgramRepository.persist(majorProgram);

    return majorProgramMapper.toResponse(majorProgram);
  }

  @Override
  public List<MajorProgramResponse> getAllMajorPrograms(Long majorId) {
    majorService.getMajorById(majorId);
    return majorProgramMapper.toResponseList(majorProgramRepository.listByMajorId(majorId));
  }

  @Override
  @Transactional
  public MajorProgramReportResponse getMajorProgramReport(Long majorId, Long id) {
    MajorProgram program = getMajorProgramByMajorIdAndId(majorId, id);

    List<CourseMajor> rows = courseMajorService.listProgramCurriculumWithDetails(id);

    // Group by recommended semester; TreeMap keeps semesters in ascending order.
    Map<Integer, List<CourseMajor>> bySemester =
        rows.stream()
            .collect(
                Collectors.groupingBy(cm -> cm.programSemester, TreeMap::new, Collectors.toList()));

    List<SemesterReportResponse> semesters =
        bySemester.entrySet().stream()
            .map(
                entry -> {
                  List<CourseReportResponse> courses =
                      entry.getValue().stream()
                          .sorted(CURRICULUM_ORDER)
                          .map(majorProgramReportMapper::toCourseReport)
                          .toList();
                  return new SemesterReportResponse(entry.getKey(), courses);
                })
            .toList();

    return new MajorProgramReportResponse(
        program.id,
        program.major.id,
        program.major.majorCode,
        program.major.name,
        program.entryYear,
        program.totalRequiredCredit,
        program.totalOptionalCredit,
        semesters);
  }

  @Override
  @Transactional
  public void recalculateCredits(Long majorProgramId) {
    MajorProgram program = getMajorProgramById(majorProgramId);
    List<CourseMajor> rows = courseMajorService.listProgramCurriculum(majorProgramId);

    int required =
        rows.stream()
            .filter(cm -> Boolean.TRUE.equals(cm.isRequired))
            .mapToInt(cm -> cm.credits)
            .sum();
    int optional =
        rows.stream()
            .filter(cm -> Boolean.FALSE.equals(cm.isRequired))
            .mapToInt(cm -> cm.credits)
            .sum();

    // Managed entity: the new totals are flushed on transaction commit.
    program.totalRequiredCredit = required;
    program.totalOptionalCredit = optional;
  }

  @Override
  public MajorProgram getMajorProgramById(Long id) {
    return majorProgramRepository
        .findByIdOptional(id)
        .orElseThrow(() -> new ApiException(ErrorCode.MAJOR_PROGRAM_NOT_FOUND));
  }

  @Override
  public MajorProgram getMajorProgramByMajorIdAndId(Long majorId, Long id) {
    MajorProgram program = getMajorProgramById(id);
    if (!program.major.id.equals(majorId)) {
      throw new ApiException(ErrorCode.MAJOR_PROGRAM_NOT_FOUND);
    }
    return program;
  }
}
