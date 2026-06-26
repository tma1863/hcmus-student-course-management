package com.example.studentcoursemanagement.enrollment.mapper;

import com.example.studentcoursemanagement.common.util.GradeScale;
import com.example.studentcoursemanagement.course.entity.CourseMajor;
import com.example.studentcoursemanagement.course.entity.OpenCourse;
import com.example.studentcoursemanagement.enrollment.dto.response.EnrollmentResponse;
import com.example.studentcoursemanagement.enrollment.entity.Enrollment;
import jakarta.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;

/**
 * Hand-written mapper (like {@code StudentMapper}) because the response carries grade fields derived
 * from the score via {@link GradeScale}, not plain field copies.
 */
@ApplicationScoped
public class EnrollmentMapper {

  public EnrollmentResponse toResponse(Enrollment enrollment) {
    OpenCourse openCourse = enrollment.openCourse;
    CourseMajor courseMajor = openCourse.courseMajor;

    BigDecimal score = enrollment.score;
    String letter = score == null ? null : GradeScale.letterOf(score);
    BigDecimal gradePoint = score == null ? null : GradeScale.gradePointOf(score);

    return new EnrollmentResponse(
        enrollment.id,
        enrollment.student.id,
        enrollment.student.studentId,
        openCourse.id,
        courseMajor.course.courseId,
        courseMajor.course.name,
        courseMajor.credits,
        courseMajor.programSemester,
        openCourse.academicYear,
        openCourse.semester,
        score,
        letter,
        gradePoint,
        enrollment.status,
        enrollment.attemptNumber);
  }
}
