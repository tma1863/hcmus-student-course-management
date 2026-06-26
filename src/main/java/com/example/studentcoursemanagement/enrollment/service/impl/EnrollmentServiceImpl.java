package com.example.studentcoursemanagement.enrollment.service.impl;

import com.example.studentcoursemanagement.common.exception.ApiException;
import com.example.studentcoursemanagement.common.exception.ErrorCode;
import com.example.studentcoursemanagement.common.util.GradeScale;
import com.example.studentcoursemanagement.course.dao.OpenCourseRepository;
import com.example.studentcoursemanagement.course.entity.OpenCourse;
import com.example.studentcoursemanagement.course.enums.EOpenCourseStatus;
import com.example.studentcoursemanagement.enrollment.dao.EnrollmentRepository;
import com.example.studentcoursemanagement.enrollment.dto.request.CreateEnrollmentRequest;
import com.example.studentcoursemanagement.enrollment.dto.request.RecordScoreRequest;
import com.example.studentcoursemanagement.enrollment.dto.response.EnrollmentResponse;
import com.example.studentcoursemanagement.enrollment.entity.Enrollment;
import com.example.studentcoursemanagement.enrollment.enums.EEnrollmentStatus;
import com.example.studentcoursemanagement.enrollment.mapper.EnrollmentMapper;
import com.example.studentcoursemanagement.enrollment.service.EnrollmentService;
import com.example.studentcoursemanagement.student.dao.StudentRepository;
import com.example.studentcoursemanagement.student.entity.Student;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class EnrollmentServiceImpl implements EnrollmentService {

  private static final BigDecimal GPA_ZERO = BigDecimal.ZERO.setScale(2);

  /** Orders attempts of one course chronologically: academic year, then term, then attempt no. */
  private static final Comparator<Enrollment> ATTEMPT_ORDER =
      Comparator.<Enrollment, String>comparing(e -> e.openCourse.academicYear)
          .thenComparing(e -> e.openCourse.semester)
          .thenComparing(e -> e.attemptNumber);

  @Inject EnrollmentRepository enrollmentRepository;
  @Inject OpenCourseRepository openCourseRepository;
  @Inject StudentRepository studentRepository;
  @Inject EnrollmentMapper enrollmentMapper;

  @Override
  @Transactional
  public EnrollmentResponse enroll(CreateEnrollmentRequest request) {
    Student student =
        studentRepository
            .findByIdOptional(request.studentId())
            .orElseThrow(() -> new ApiException(ErrorCode.STUDENT_NOT_FOUND));

    OpenCourse openCourse =
        openCourseRepository
            .findByIdOptional(request.openCourseId())
            .orElseThrow(() -> new ApiException(ErrorCode.OPEN_COURSE_NOT_FOUND));

    if (openCourse.status != EOpenCourseStatus.OPEN) {
      throw new ApiException(ErrorCode.OPEN_COURSE_NOT_OPEN);
    }
    if (openCourse.enrolledCount >= openCourse.maxStudents) {
      throw new ApiException(ErrorCode.OPEN_COURSE_FULL);
    }
    if (enrollmentRepository
        .findByStudentAndOpenCourse(student.id, openCourse.id)
        .isPresent()) {
      throw new ApiException(ErrorCode.ALREADY_ENROLLED);
    }

    long priorAttempts = enrollmentRepository.countAttempts(student.id, openCourse.courseMajor.course.id);

    Enrollment enrollment =
        Enrollment.builder()
            .student(student)
            .openCourse(openCourse)
            .status(EEnrollmentStatus.ENROLLED)
            .attemptNumber((int) priorAttempts + 1)
            .build();
    enrollmentRepository.persist(enrollment);

    // Managed entity: the seat bump (and FULL flip) is flushed on commit.
    openCourse.enrolledCount += 1;
    if (openCourse.enrolledCount >= openCourse.maxStudents) {
      openCourse.status = EOpenCourseStatus.FULL;
    }

    return enrollmentMapper.toResponse(enrollment);
  }

  @Override
  @Transactional
  public EnrollmentResponse recordScore(Long enrollmentId, RecordScoreRequest request) {
    Enrollment enrollment =
        enrollmentRepository
            .findByIdOptional(enrollmentId)
            .orElseThrow(() -> new ApiException(ErrorCode.ENROLLMENT_NOT_FOUND));

    BigDecimal score = request.score();
    if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(BigDecimal.TEN) > 0) {
      throw new ApiException(ErrorCode.INVALID_SCORE);
    }

    enrollment.score = score;
    enrollment.status =
        GradeScale.isPass(score) ? EEnrollmentStatus.COMPLETED : EEnrollmentStatus.FAILED;

    recalculateGpa(enrollment.student.id);

    return enrollmentMapper.toResponse(enrollment);
  }

  @Override
  @Transactional
  public void recalculateGpa(UUID studentId) {
    Student student =
        studentRepository
            .findByIdOptional(studentId)
            .orElseThrow(() -> new ApiException(ErrorCode.STUDENT_NOT_FOUND));

    List<Enrollment> graded = enrollmentRepository.listGradedByStudentIdWithDetails(studentId);

    // Each distinct course counts once, using its latest attempt.
    Map<Long, Enrollment> latestPerCourse = new HashMap<>();
    for (Enrollment enrollment : graded) {
      Long courseId = enrollment.openCourse.courseMajor.course.id;
      latestPerCourse.merge(
          courseId,
          enrollment,
          (existing, candidate) ->
              ATTEMPT_ORDER.compare(candidate, existing) >= 0 ? candidate : existing);
    }

    BigDecimal weightedPoints = BigDecimal.ZERO;
    int totalCredits = 0;
    for (Enrollment enrollment : latestPerCourse.values()) {
      int credits = enrollment.openCourse.courseMajor.credits;
      weightedPoints =
          weightedPoints.add(GradeScale.gradePointOf(enrollment.score).multiply(BigDecimal.valueOf(credits)));
      totalCredits += credits;
    }

    student.gpa =
        totalCredits == 0
            ? GPA_ZERO
            : weightedPoints.divide(BigDecimal.valueOf(totalCredits), 2, RoundingMode.HALF_UP);
  }
}
