package com.example.studentcoursemanagement.major.dao;

import com.example.studentcoursemanagement.major.entity.Major;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

@ApplicationScoped
public class MajorRepository implements PanacheRepository<Major> {

  public boolean existsByMajorCodeAndEntryYear(String majorCode, String entryYear) {
    return count("majorCode = ?1 and entryYear = ?2", majorCode, entryYear) > 0;
  }

  public Optional<Major> findByMajorCodeAndEntryYear(String majorCode, String entryYear) {
    return find("majorCode = ?1 and entryYear = ?2", majorCode, entryYear).firstResultOptional();
  }
}

