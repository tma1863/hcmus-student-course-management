package com.example.studentcoursemanagement.course.entity;

import com.example.studentcoursemanagement.common.util.BaseEntity;
import com.example.studentcoursemanagement.major.entity.Major;
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
    uniqueConstraints =
        @UniqueConstraint(columnNames = {"course_id", "major_id", "academic_year"}))
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
  @JoinColumn(name = "major_id", nullable = false, updatable = false)
  public Major major;

  @Column(nullable = false)
  @NotNull
  @Min(1)
  @Max(10)
  public Integer credits;

  @Column(nullable = false)
  @NotNull
  @Min(1)
  @Max(2)
  public Integer semester;

  @Column(name = "academic_year", length = 9, nullable = false)
  @NotNull
  public String academicYear;

  // Prerequisite Courses for THIS curriculum entry (major-scoped).
  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "course_major_prerequisites",
      joinColumns = @JoinColumn(name = "course_major_id"),
      inverseJoinColumns = @JoinColumn(name = "prerequisite_course_id"))
  @Builder.Default
  public List<Course> prerequisites = new ArrayList<>();
}
