package com.example.studentcoursemanagement.major.mapper;

import com.example.studentcoursemanagement.major.dto.request.UpdateMajorProgramCreditsRequest;
import com.example.studentcoursemanagement.major.dto.response.MajorProgramResponse;
import com.example.studentcoursemanagement.major.entity.MajorProgram;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "jakarta-cdi")
public interface MajorProgramMapper {

  @Mapping(target = "majorId", source = "major.id")
  @Mapping(target = "majorCode", source = "major.majorCode")
  @Mapping(target = "name", source = "major.name")
  MajorProgramResponse toResponse(MajorProgram majorProgram);

  List<MajorProgramResponse> toResponseList(List<MajorProgram> majorPrograms);

  void updateCredits(
      @MappingTarget MajorProgram majorProgram, UpdateMajorProgramCreditsRequest request);
}
