package com.example.studentcoursemanagement.major.mapper;

import com.example.studentcoursemanagement.major.dto.response.MajorResponse;
import com.example.studentcoursemanagement.major.entity.Major;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "jakarta-cdi")
public interface MajorMapper {

  MajorResponse toResponse(Major major);

  List<MajorResponse> toResponseList(List<Major> majors);
}
