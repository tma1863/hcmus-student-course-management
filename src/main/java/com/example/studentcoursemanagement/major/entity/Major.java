package com.example.studentcoursemanagement.major.entity;

import com.example.studentcoursemanagement.common.util.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "majors",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"major_code"})})
@Builder(builderMethodName = "builder")
@NoArgsConstructor
@AllArgsConstructor
public class Major extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "major_code", length = 2, nullable = false, updatable = false, unique = true)
  @NotNull
  public String majorCode;

  @Column(nullable = false)
  @NotNull
  public String name;

  @OneToMany(mappedBy = "major", fetch = FetchType.LAZY)
  public List<MajorProgram> programs;
}
