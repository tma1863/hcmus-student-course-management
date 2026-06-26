package com.example.studentcoursemanagement.enrollment.service.impl;

import com.example.studentcoursemanagement.common.util.GradeScale;
import com.example.studentcoursemanagement.enrollment.dao.EnrollmentRepository;
import com.example.studentcoursemanagement.enrollment.dto.response.StudentReportResponse;
import com.example.studentcoursemanagement.enrollment.dto.response.TranscriptEntryResponse;
import com.example.studentcoursemanagement.enrollment.entity.Enrollment;
import com.example.studentcoursemanagement.enrollment.enums.EEnrollmentStatus;
import com.example.studentcoursemanagement.enrollment.service.StudentReportService;
import com.example.studentcoursemanagement.student.entity.Student;
import com.example.studentcoursemanagement.student.service.StudentService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class StudentReportServiceImpl implements StudentReportService {

  private static final BigDecimal GPA_ZERO = BigDecimal.ZERO.setScale(2);

  /** Orders attempts of one course chronologically: academic year, then term, then attempt no. */
  private static final Comparator<Enrollment> ATTEMPT_ORDER =
      Comparator.<Enrollment, String>comparing(e -> e.openCourse.academicYear)
          .thenComparing(e -> e.openCourse.semester)
          .thenComparing(e -> e.attemptNumber);

  /** Lists courses by business key, then attempt order. */
  private static final Comparator<Enrollment> DISPLAY_ORDER =
      Comparator.<Enrollment, String>comparing(e -> e.openCourse.courseMajor.course.courseId)
          .thenComparing(e -> e.attemptNumber);

  @Inject EnrollmentRepository enrollmentRepository;
  @Inject StudentService studentService;

  @Override
  @Transactional
  public StudentReportResponse getReport(UUID studentId) {
    Student student = studentService.getStudentById(studentId);

    List<Enrollment> enrollments = enrollmentRepository.listByStudentIdWithDetails(studentId);

    Set<Long> countedIds = countedEnrollmentIds(enrollments);

    List<TranscriptEntryResponse> courses =
        enrollments.stream()
            .sorted(DISPLAY_ORDER)
            .map(e -> toEntry(e, countedIds.contains(e.id)))
            .toList();

    List<Enrollment> counted = enrollments.stream().filter(e -> countedIds.contains(e.id)).toList();

    return new StudentReportResponse(
        student.id,
        student.studentId,
        student.name,
        student.majorProgram.major.majorCode,
        student.majorProgram.major.name,
        student.majorProgram.entryYear,
        student.status,
        gpaOf(counted),
        creditsEarned(counted),
        courses);
  }

  /** The latest graded attempt per course — the set of enrollments that count toward the GPA. */
  private Set<Long> countedEnrollmentIds(List<Enrollment> enrollments) {
    Map<Long, Enrollment> latestPerCourse = new HashMap<>();
    for (Enrollment enrollment : enrollments) {
      if (enrollment.score == null) {
        continue;
      }
      Long courseId = enrollment.openCourse.courseMajor.course.id;
      latestPerCourse.merge(
          courseId,
          enrollment,
          (existing, candidate) ->
              ATTEMPT_ORDER.compare(candidate, existing) >= 0 ? candidate : existing);
    }
    return latestPerCourse.values().stream().map(e -> e.id).collect(Collectors.toSet());
  }

  private TranscriptEntryResponse toEntry(Enrollment enrollment, boolean counted) {
    BigDecimal score = enrollment.score;
    return new TranscriptEntryResponse(
        enrollment.openCourse.courseMajor.course.courseId,
        enrollment.openCourse.courseMajor.course.name,
        enrollment.openCourse.courseMajor.credits,
        enrollment.attemptNumber,
        score,
        score == null ? null : GradeScale.letterOf(score),
        score == null ? null : GradeScale.gradePointOf(score),
        enrollment.status,
        counted);
  }

  /** Credit-weighted 4.0 GPA over the given counted enrollments; 0.00 when there are none. */
  private BigDecimal gpaOf(Collection<Enrollment> counted) {
    BigDecimal weightedPoints = BigDecimal.ZERO;
    int totalCredits = 0;
    for (Enrollment enrollment : counted) {
      int credits = enrollment.openCourse.courseMajor.credits;
      weightedPoints =
          weightedPoints.add(
              GradeScale.gradePointOf(enrollment.score).multiply(BigDecimal.valueOf(credits)));
      totalCredits += credits;
    }
    return totalCredits == 0
        ? GPA_ZERO
        : weightedPoints.divide(BigDecimal.valueOf(totalCredits), 2, RoundingMode.HALF_UP);
  }

  private int creditsEarned(Collection<Enrollment> counted) {
    return counted.stream()
        .filter(e -> e.status == EEnrollmentStatus.COMPLETED)
        .mapToInt(e -> e.openCourse.courseMajor.credits)
        .sum();
  }
}
