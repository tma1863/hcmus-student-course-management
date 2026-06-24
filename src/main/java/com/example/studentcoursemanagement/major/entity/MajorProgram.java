package com.example.studentcoursemanagement.major.entity;

import com.example.studentcoursemanagement.common.util.BaseEntity;
import com.example.studentcoursemanagement.student.entity.Student;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

/**
 * One entry-year edition of a {@link Major}. Holds the year-specific credit requirements; a {@link
 * Student} belongs to a program (major + entry year) rather than to the bare major.
 */
@Entity
@Table(
    name = "major_programs",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"major_id", "entry_year"})})
@Builder(builderMethodName = "builder")
@NoArgsConstructor
@AllArgsConstructor
public class MajorProgram extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "major_id", nullable = false, updatable = false)
  public Major major;

  @Column(name = "entry_year", length = 4, nullable = false, updatable = false)
  @NotNull
  public String entryYear;

  @Column(name = "total_required_credit", nullable = false)
  public Integer totalRequiredCredit;

  @Column(name = "total_optional_credit", nullable = false)
  public Integer totalOptionalCredit;

  @OneToMany(mappedBy = "majorProgram", fetch = FetchType.LAZY)
  public List<Student> students;
}
