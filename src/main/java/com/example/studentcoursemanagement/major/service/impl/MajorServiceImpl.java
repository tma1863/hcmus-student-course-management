package com.example.studentcoursemanagement.major.service.impl;

import com.example.studentcoursemanagement.common.exception.ApiException;
import com.example.studentcoursemanagement.common.exception.ErrorCode;
import com.example.studentcoursemanagement.major.dao.MajorRepository;
import com.example.studentcoursemanagement.major.dto.request.CreateMajorRequest;
import com.example.studentcoursemanagement.major.dto.response.MajorResponse;
import com.example.studentcoursemanagement.major.entity.Major;
import com.example.studentcoursemanagement.major.service.MajorService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class MajorServiceImpl implements MajorService {

  @Inject MajorRepository majorRepository;

  @Override
  @Transactional
  public MajorResponse createMajor(CreateMajorRequest request) {
    String majorCode = request.majorCode().trim().toUpperCase();
    String entryYear = request.entryYear().trim();

    if (majorRepository.existsByMajorCodeAndEntryYear(majorCode, entryYear)) {
      throw new ApiException(ErrorCode.MAJOR_ALREADY_EXISTS);
    }

    Major major = new Major();
    major.majorCode = majorCode;
    major.entryYear = entryYear;
    major.name = request.name().trim();
    major.totalRequiredCredit = 0;
    major.totalOptionalCredit = 0;

    majorRepository.persist(major);

    return new MajorResponse(major.id, major.majorCode, major.entryYear, major.name);
  }
}

