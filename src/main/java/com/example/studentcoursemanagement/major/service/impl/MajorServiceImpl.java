package com.example.studentcoursemanagement.major.service.impl;

import com.example.studentcoursemanagement.common.exception.ApiException;
import com.example.studentcoursemanagement.common.exception.ErrorCode;
import com.example.studentcoursemanagement.major.dao.MajorRepository;
import com.example.studentcoursemanagement.major.dto.request.CreateMajorRequest;
import com.example.studentcoursemanagement.major.dto.request.UpdateMajorCreditsRequest;
import com.example.studentcoursemanagement.major.dto.response.MajorResponse;
import com.example.studentcoursemanagement.major.entity.Major;
import com.example.studentcoursemanagement.major.mapper.MajorMapper;
import com.example.studentcoursemanagement.major.service.MajorService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class MajorServiceImpl implements MajorService {

  @Inject MajorRepository majorRepository;
  @Inject MajorMapper majorMapper;

  @Override
  @Transactional
  public MajorResponse createMajor(CreateMajorRequest request) {
    String majorCode = request.majorCode().trim().toUpperCase();
    String entryYear = request.entryYear().trim();

    if (majorRepository.existsByMajorCodeAndEntryYear(majorCode, entryYear)) {
      throw new ApiException(ErrorCode.MAJOR_ALREADY_EXISTS);
    }

    Major major =
        Major.builder()
            .majorCode(majorCode)
            .entryYear(entryYear)
            .name(request.name().trim())
            .totalRequiredCredit(0)
            .totalOptionalCredit(0)
            .build();

    majorRepository.persist(major);

    return majorMapper.toResponse(major);
  }

  @Override
  @Transactional
  public MajorResponse updateMajorCredits(Long id, UpdateMajorCreditsRequest request) {
    Major major = getMajorById(id);
    majorMapper.updateCredits(major, request);
    return majorMapper.toResponse(major);
  }

  @Override
  public List<MajorResponse> getAllMajors() {
    return majorMapper.toResponseList(majorRepository.listAll());
  }

  @Override
  public Major getMajorById(Long id) {
    return majorRepository
        .findByIdOptional(id)
        .orElseThrow(() -> new ApiException(ErrorCode.MAJOR_NOT_FOUND));
  }
}
