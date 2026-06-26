package com.example.studentcoursemanagement.course.mapper;

import com.example.studentcoursemanagement.course.dto.response.CourseMajorResponse;
import com.example.studentcoursemanagement.course.entity.Course;
import com.example.studentcoursemanagement.course.entity.CourseMajor;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "jakarta-cdi")
public interface CourseMajorMapper {

  @Mapping(target = "majorProgramId", source = "majorProgram.id")
  @Mapping(target = "courseId", source = "course.courseId")
  @Mapping(target = "courseName", source = "course.name")
  @Mapping(target = "prerequisiteCourseIds", source = "prerequisites")
  CourseMajorResponse toResponse(CourseMajor courseMajor);

  /** Flattens prerequisite courses to their sorted business keys. */
  default List<String> toCourseIds(List<Course> prerequisites) {
    if (prerequisites == null) {
      return List.of();
    }
    return prerequisites.stream().map(course -> course.courseId).sorted().toList();
  }
}
