package com.example.studentcoursemanagement.major.dao;

import com.example.studentcoursemanagement.major.entity.MajorProgram;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MajorProgramRepository implements PanacheRepository<MajorProgram> {

  public boolean existsByMajorIdAndEntryYear(Long majorId, String entryYear) {
    return count("major.id = ?1 and entryYear = ?2", majorId, entryYear) > 0;
  }
}
