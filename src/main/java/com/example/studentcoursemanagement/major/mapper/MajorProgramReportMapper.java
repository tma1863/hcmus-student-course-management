package com.example.studentcoursemanagement.major.mapper;

import com.example.studentcoursemanagement.course.entity.Course;
import com.example.studentcoursemanagement.course.entity.CourseMajor;
import com.example.studentcoursemanagement.major.dto.response.CourseReportResponse;
import com.example.studentcoursemanagement.major.dto.response.PrerequisiteResponse;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Leaf mappings for the curriculum report; semester grouping/ordering stays in the service. */
@Mapper(componentModel = "jakarta-cdi")
public interface MajorProgramReportMapper {

  @Mapping(target = "courseId", source = "course.courseId")
  @Mapping(target = "name", source = "course.name")
  CourseReportResponse toCourseReport(CourseMajor courseMajor);

  PrerequisiteResponse toPrerequisite(Course course);

  List<PrerequisiteResponse> toPrerequisites(List<Course> prerequisites);
}
