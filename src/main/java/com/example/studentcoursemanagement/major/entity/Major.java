package com.example.studentcoursemanagement.major.entity;

import com.example.studentcoursemanagement.common.util.BaseEntity;
import com.example.studentcoursemanagement.student.entity.Student;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Entity
@Table(
    name = "majors",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"major_code", "entry_year"})})
public class Major extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "major_code", length = 2, nullable = false)
  @NotNull
  public String majorCode;

  @Column(nullable = false)
  @NotNull
  public String name;

  @Column(name = "entry_year", length = 4, nullable = false)
  @NotNull
  public String entryYear;

  @Column(name = "total_required_credit", nullable = false)
  public Integer totalRequiredCredit;

  @Column(name = "total_optional_credit", nullable = false)
  public Integer totalOptionalCredit;

  @OneToMany(mappedBy = "major", fetch = FetchType.LAZY)
  public List<Student> students;
}
