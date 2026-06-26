package com.example.studentcoursemanagement.enrollment.enums;

/**
 * Lifecycle of a single {@code Enrollment}.
 *
 * <p>The status is <em>derived from the score</em> by the service, never set by the client, so it can
 * never disagree with the recorded score:
 *
 * <ul>
 *   <li>{@link #ENROLLED} — registered for an open course, no score recorded yet (score is {@code null}).
 *   <li>{@link #COMPLETED} — graded and passed (grade point ≥ pass threshold).
 *   <li>{@link #FAILED} — graded and failed (raw score below the pass threshold ⇒ {@code F}).
 * </ul>
 */
public enum EEnrollmentStatus {
  ENROLLED,
  COMPLETED,
  FAILED
}
