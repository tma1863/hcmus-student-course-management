package com.example.studentcoursemanagement.course.entity;

import com.example.studentcoursemanagement.common.util.BaseEntity;
import com.example.studentcoursemanagement.course.enums.EOpenCourseStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "open_courses")
@Builder(builderMethodName = "builder")
@NoArgsConstructor
@AllArgsConstructor
public class OpenCourse extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "course_major_id", nullable = false)
  public CourseMajor courseMajor;

  @Column(nullable = false)
  @NotNull
  @Min(1)
  @Max(2)
  public Integer semester;

  @Column(name = "academic_year", length = 9, nullable = false)
  @NotNull
  public String academicYear;

  @Column(name = "max_students", nullable = false)
  @NotNull
  @Min(1)
  public Integer maxStudents;

  @Column(name = "enrolled_count", nullable = false)
  @Builder.Default
  public Integer enrolledCount = 0;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @NotNull
  @Builder.Default
  public EOpenCourseStatus status = EOpenCourseStatus.OPEN;
}
