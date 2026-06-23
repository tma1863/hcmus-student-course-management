package com.example.studentcoursemanagement.major.mapper;

import com.example.studentcoursemanagement.major.dto.request.UpdateMajorCreditsRequest;
import com.example.studentcoursemanagement.major.dto.response.MajorResponse;
import com.example.studentcoursemanagement.major.entity.Major;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "jakarta-cdi")
public interface MajorMapper {

  MajorResponse toResponse(Major major);

  List<MajorResponse> toResponseList(List<Major> majors);

  void updateCredits(@MappingTarget Major major, UpdateMajorCreditsRequest request);
}
