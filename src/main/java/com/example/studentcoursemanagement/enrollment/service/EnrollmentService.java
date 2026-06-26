package com.example.studentcoursemanagement.enrollment.service;

import com.example.studentcoursemanagement.enrollment.dto.request.CreateEnrollmentRequest;
import com.example.studentcoursemanagement.enrollment.dto.request.RecordScoreRequest;
import com.example.studentcoursemanagement.enrollment.dto.response.EnrollmentResponse;
import java.util.UUID;

public interface EnrollmentService {

  /** Enrolls a student in an open course (status {@code ENROLLED}, no score yet). */
  EnrollmentResponse enroll(CreateEnrollmentRequest request);

  /** Records/updates the score, derives pass/fail status, and recomputes the student's GPA. */
  EnrollmentResponse recordScore(Long enrollmentId, RecordScoreRequest request);

  /**
   * Recomputes and persists a student's cumulative GPA from their graded enrollments: each distinct
   * course counts once (latest attempt), credit-weighted on the 4.0 scale.
   */
  void recalculateGpa(UUID studentId);
}
