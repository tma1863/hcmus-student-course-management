package com.example.studentcoursemanagement;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
class MajorStudentResourceTest {

  private long createMajor(String majorCode, String name) {
    return given()
        .contentType("application/json")
        .body(
            """
            {
              "majorCode": "%s",
              "name": "%s"
            }
            """
                .formatted(majorCode, name))
        .when()
        .post("/api/majors")
        .then()
        .statusCode(201)
        .body("id", notNullValue())
        .body("majorCode", equalTo(majorCode))
        .body("name", equalTo(name))
        .extract()
        .jsonPath()
        .getLong("id");
  }

  private long createProgram(long majorId, String entryYear) {
    return given()
        .contentType("application/json")
        .body(
            """
            {
              "majorId": %d,
              "entryYear": "%s"
            }
            """
                .formatted(majorId, entryYear))
        .when()
        .post("/api/major-programs")
        .then()
        .statusCode(201)
        .body("id", notNullValue())
        .body("majorId", equalTo((int) majorId))
        .body("entryYear", equalTo(entryYear))
        .extract()
        .jsonPath()
        .getLong("id");
  }

  /** Convenience: create a major and one entry-year program, returning the program id. */
  private long createMajorProgram(String majorCode, String entryYear, String name) {
    return createProgram(createMajor(majorCode, name), entryYear);
  }

  private String createStudent(long majorProgramId, String name) {
    return given()
        .contentType("application/json")
        .body(
            """
            {
              "name": "%s",
              "gender": "MALE",
              "majorProgramId": %d
            }
            """
                .formatted(name, majorProgramId))
        .when()
        .post("/api/students")
        .then()
        .statusCode(201)
        .body("id", notNullValue())
        .body("studentId", notNullValue())
        .body("name", equalTo(name))
        .body("status", equalTo("STUDYING"))
        .body("gpa", equalTo(0.0F))
        .extract()
        .jsonPath()
        .getString("studentId");
  }

  @Test
  void shouldCreateMajor() {
    createMajor("10", "Computer Science");
  }

  @Test
  void shouldRejectDuplicateMajor() {
    createMajor("11", "Software Engineering");

    given()
        .contentType("application/json")
        .body(
            """
            {
              "majorCode": "11",
              "name": "Software Engineering"
            }
            """)
        .when()
        .post("/api/majors")
        .then()
        .statusCode(409);
  }

  @Test
  void shouldRejectDuplicateProgram() {
    long majorId = createMajor("12", "Cybersecurity");
    createProgram(majorId, "2026");

    given()
        .contentType("application/json")
        .body(
            """
            {
              "majorId": %d,
              "entryYear": "2026"
            }
            """
                .formatted(majorId))
        .when()
        .post("/api/major-programs")
        .then()
        .statusCode(409);
  }

  @Test
  void shouldCreateStudentWithGeneratedCode() {
    long programId = createMajorProgram("28", "2026", "Information Technology");

    // Generated code = entryYear[2:] + majorCode + 4-digit sequence -> 2628XXXX
    String studentId = createStudent(programId, "Nguyen Van A");
    Assertions.assertTrue(studentId.matches("2628\\d{4}"));
  }

  @Test
  void shouldIncrementSequenceWithinSameMajor() {
    long programId = createMajorProgram("30", "2026", "Data Science");

    String first = createStudent(programId, "Student One");
    String second = createStudent(programId, "Student Two");

    int firstSeq = Integer.parseInt(first.substring(4));
    int secondSeq = Integer.parseInt(second.substring(4));
    Assertions.assertEquals(firstSeq + 1, secondSeq);
  }

  @Test
  void shouldUseIndependentSequencePerMajor() {
    long programA = createMajorProgram("31", "2026", "Major A");
    long programB = createMajorProgram("32", "2026", "Major B");

    String studentA = createStudent(programA, "Student A");
    String studentB = createStudent(programB, "Student B");

    Assertions.assertTrue(studentA.matches("2631\\d{4}"));
    Assertions.assertTrue(studentB.matches("2632\\d{4}"));
  }

  @Test
  void shouldReturn404WhenMajorProgramIdDoesNotExist() {
    given()
        .contentType("application/json")
        .body(
            """
            {
              "name": "Ghost Student",
              "gender": "FEMALE",
              "majorProgramId": 999999999
            }
            """)
        .when()
        .post("/api/students")
        .then()
        .statusCode(404);
  }

  @Test
  void shouldReturn400WhenStudentRequestIsInvalid() {
    given()
        .contentType("application/json")
        .body(
            """
            {
              "gender": "MALE",
              "majorProgramId": 1
            }
            """)
        .when()
        .post("/api/students")
        .then()
        .statusCode(400);
  }
}
