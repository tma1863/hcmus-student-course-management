package com.example.studentcoursemanagement.course.entity;

import com.example.studentcoursemanagement.common.util.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "courses")
@Builder(builderMethodName = "builder")
@NoArgsConstructor
@AllArgsConstructor
public class Course extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  // Business Key (e.g. "MTH00001"): 3 uppercase letters + 5-digit sequence.
  // Immutable and unique, mirroring Student.studentId.
  @Column(name = "course_id", length = 8, nullable = false, updatable = false, unique = true)
  @NotNull
  public String courseId;

  @Column(nullable = false)
  @NotNull
  public String name;
}
