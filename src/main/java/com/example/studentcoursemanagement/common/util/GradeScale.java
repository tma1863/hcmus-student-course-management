package com.example.studentcoursemanagement.common.util;

import java.math.BigDecimal;
import java.util.List;

/**
 * Single source of truth for converting a raw 0–10 course score to the HCMUS 4.0 grading scale.
 *
 * <p>The seed generators must mirror this exact table so seeded GPAs match what the API computes.
 *
 * <pre>
 *   raw score   letter   grade point   pass?
 *   8.5 – 10.0    A          4.0         yes
 *   8.0 – 8.4     B+         3.5         yes
 *   7.0 – 7.9     B          3.0         yes
 *   6.5 – 6.9     C+         2.5         yes
 *   5.5 – 6.4     C          2.0         yes
 *   5.0 – 5.4     D+         1.5         yes
 *   4.0 – 4.9     D          1.0         yes
 *   0.0 – 3.9     F          0.0         no
 * </pre>
 */
public final class GradeScale {

  private GradeScale() {}

  /** Minimum grade point that counts as a pass (a {@code D}); anything below is an {@code F}. */
  public static final BigDecimal PASS_GRADE_POINT = new BigDecimal("1.0");

  /** One grading band: applies when {@code score >= minScore}. Ordered high → low. */
  private record Band(BigDecimal minScore, String letter, BigDecimal gradePoint) {}

  private static final List<Band> BANDS =
      List.of(
          new Band(new BigDecimal("8.5"), "A", new BigDecimal("4.0")),
          new Band(new BigDecimal("8.0"), "B+", new BigDecimal("3.5")),
          new Band(new BigDecimal("7.0"), "B", new BigDecimal("3.0")),
          new Band(new BigDecimal("6.5"), "C+", new BigDecimal("2.5")),
          new Band(new BigDecimal("5.5"), "C", new BigDecimal("2.0")),
          new Band(new BigDecimal("5.0"), "D+", new BigDecimal("1.5")),
          new Band(new BigDecimal("4.0"), "D", new BigDecimal("1.0")),
          new Band(new BigDecimal("0.0"), "F", new BigDecimal("0.0")));

  private static Band bandOf(BigDecimal score) {
    if (score == null) {
      throw new IllegalArgumentException("score must not be null");
    }
    for (Band band : BANDS) {
      if (score.compareTo(band.minScore()) >= 0) {
        return band;
      }
    }
    // score < 0 — outside the valid range; treated as F.
    return BANDS.get(BANDS.size() - 1);
  }

  /** Letter grade (e.g. {@code "B+"}) for a raw 0–10 score. */
  public static String letterOf(BigDecimal score) {
    return bandOf(score).letter();
  }

  /** 4.0-scale grade point for a raw 0–10 score. */
  public static BigDecimal gradePointOf(BigDecimal score) {
    return bandOf(score).gradePoint();
  }

  /** Whether a raw 0–10 score is a passing grade (grade point ≥ {@link #PASS_GRADE_POINT}). */
  public static boolean isPass(BigDecimal score) {
    return score != null && gradePointOf(score).compareTo(PASS_GRADE_POINT) >= 0;
  }
}
