package com.example.studentcoursemanagement.course.entity;

import com.example.studentcoursemanagement.common.util.BaseEntity;
import com.example.studentcoursemanagement.major.entity.MajorProgram;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "course_majors",
    uniqueConstraints = @UniqueConstraint(columnNames = {"course_id", "major_program_id"}))
@Builder(builderMethodName = "builder")
@NoArgsConstructor
@AllArgsConstructor
public class CourseMajor extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "course_id", nullable = false, updatable = false)
  public Course course;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "major_program_id", nullable = false, updatable = false)
  public MajorProgram majorProgram;

  @Column(nullable = false)
  @NotNull
  @Min(1)
  @Max(10)
  public Integer credits;

  @Column(name = "program_semester", nullable = false)
  @NotNull
  @Min(1)
  @Max(10)
  public Integer programSemester;

  /**
   * Whether this course is mandatory ({@code true}) or an elective ({@code false}) within the
   * program. Required courses carry 3-4 credits; optional courses carry 1-2 credits.
   */
  @Column(name = "is_required", nullable = false)
  @NotNull
  public Boolean isRequired;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "course_major_prerequisites",
      joinColumns = @JoinColumn(name = "course_major_id"),
      inverseJoinColumns = @JoinColumn(name = "prerequisite_course_id"))
  @Builder.Default
  public List<Course> prerequisites = new ArrayList<>();
}
