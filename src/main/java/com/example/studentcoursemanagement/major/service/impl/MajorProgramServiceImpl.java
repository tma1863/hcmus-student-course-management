package com.example.studentcoursemanagement.major.service.impl;

import com.example.studentcoursemanagement.common.exception.ApiException;
import com.example.studentcoursemanagement.common.exception.ErrorCode;
import com.example.studentcoursemanagement.major.dao.MajorProgramRepository;
import com.example.studentcoursemanagement.major.dto.request.CreateMajorProgramRequest;
import com.example.studentcoursemanagement.major.dto.request.UpdateMajorProgramCreditsRequest;
import com.example.studentcoursemanagement.major.dto.response.MajorProgramResponse;
import com.example.studentcoursemanagement.major.entity.Major;
import com.example.studentcoursemanagement.major.entity.MajorProgram;
import com.example.studentcoursemanagement.major.mapper.MajorProgramMapper;
import com.example.studentcoursemanagement.major.service.MajorProgramService;
import com.example.studentcoursemanagement.major.service.MajorService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class MajorProgramServiceImpl implements MajorProgramService {

  @Inject MajorProgramRepository majorProgramRepository;
  @Inject MajorProgramMapper majorProgramMapper;
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
  @Transactional
  public MajorProgramResponse updateCredits(
      Long majorId, Long id, UpdateMajorProgramCreditsRequest request) {
    MajorProgram majorProgram = getMajorProgramById(id);
    if (!majorProgram.major.id.equals(majorId)) {
      throw new ApiException(ErrorCode.MAJOR_PROGRAM_NOT_FOUND);
    }
    majorProgramMapper.updateCredits(majorProgram, request);
    return majorProgramMapper.toResponse(majorProgram);
  }

  @Override
  public List<MajorProgramResponse> getAllMajorPrograms(Long majorId) {
    majorService.getMajorById(majorId);
    return majorProgramMapper.toResponseList(majorProgramRepository.listByMajorId(majorId));
  }

  @Override
  public MajorProgram getMajorProgramById(Long id) {
    return majorProgramRepository
        .findByIdOptional(id)
        .orElseThrow(() -> new ApiException(ErrorCode.MAJOR_PROGRAM_NOT_FOUND));
  }
}
