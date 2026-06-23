package com.example.studentcoursemanagement.major.mapper;

import com.example.studentcoursemanagement.major.dto.response.MajorResponse;
import com.example.studentcoursemanagement.major.entity.Major;
import org.mapstruct.Mapper;

@Mapper(componentModel = "jakarta-cdi")
public interface MajorMapper {

  MajorResponse toResponse(Major major);
}
