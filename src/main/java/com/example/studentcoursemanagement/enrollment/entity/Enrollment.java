package com.example.studentcoursemanagement.enrollment.entity;

import com.example.studentcoursemanagement.common.util.BaseEntity;
import com.example.studentcoursemanagement.course.entity.OpenCourse;
import com.example.studentcoursemanagement.enrollment.enums.EEnrollmentStatus;
import com.example.studentcoursemanagement.student.entity.Student;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

/**
 * One student's registration in a single offered class ({@link OpenCourse}) for a term, carrying
 * the score earned once the term has been studied.
 *
 * <p>A retake is a <em>separate</em> {@code Enrollment} row against a later same-parity open
 * course; the {@code (student_id, open_course_id)} uniqueness allows it because the term (and thus
 * the open course) differs. {@link #attemptNumber} distinguishes the first attempt from retakes.
 */
@Entity
@Table(
    name = "enrollments",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_enrollments_student_open_course",
            columnNames = {"student_id", "open_course_id"}))
@Builder(builderMethodName = "builder")
@NoArgsConstructor
@AllArgsConstructor
public class Enrollment extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "student_id", nullable = false)
  public Student student;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "open_course_id", nullable = false)
  public OpenCourse openCourse;

  /** Raw course score on the 0–10 scale. {@code null} while {@link EEnrollmentStatus#ENROLLED}. */
  @Column(precision = 4, scale = 2)
  @DecimalMin("0.0")
  @DecimalMax("10.0")
  public BigDecimal score;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @NotNull
  @Builder.Default
  public EEnrollmentStatus status = EEnrollmentStatus.ENROLLED;

  /** 1 for the first attempt, 2 for the first retake, and so on. */
  @Column(name = "attempt_number", nullable = false)
  @NotNull
  @Min(1)
  @Builder.Default
  public Integer attemptNumber = 1;
}
