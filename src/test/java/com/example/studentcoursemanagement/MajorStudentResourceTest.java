package com.example.studentcoursemanagement;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
class MajorStudentResourceTest {

  private long createMajor(String majorCode, String entryYear, String name) {
    return given()
        .contentType("application/json")
        .body(
            """
            {
              "majorCode": "%s",
              "entryYear": "%s",
              "name": "%s"
            }
            """
                .formatted(majorCode, entryYear, name))
        .when()
        .post("/api/majors")
        .then()
        .statusCode(201)
        .body("id", notNullValue())
        .body("majorCode", equalTo(majorCode))
        .body("entryYear", equalTo(entryYear))
        .body("name", equalTo(name))
        .extract()
        .jsonPath()
        .getLong("id");
  }

  private String createStudent(long majorId, String name) {
    return given()
        .contentType("application/json")
        .body(
            """
            {
              "name": "%s",
              "gender": "MALE",
              "majorId": %d
            }
            """
                .formatted(name, majorId))
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
    createMajor("10", "2026", "Computer Science");
  }

  @Test
  void shouldRejectDuplicateMajor() {
    createMajor("11", "2026", "Software Engineering");

    given()
        .contentType("application/json")
        .body(
            """
            {
              "majorCode": "11",
              "entryYear": "2026",
              "name": "Software Engineering"
            }
            """)
        .when()
        .post("/api/majors")
        .then()
        .statusCode(409);
  }

  @Test
  void shouldCreateStudentWithGeneratedCode() {
    long majorId = createMajor("28", "2026", "Information Technology");

    // Generated code = entryYear[2:] + majorCode + 4-digit sequence -> 2628XXXX
    String studentId = createStudent(majorId, "Nguyen Van A");
    Assertions.assertTrue(studentId.matches("2628\\d{4}"));
  }

  @Test
  void shouldIncrementSequenceWithinSameMajor() {
    long majorId = createMajor("30", "2026", "Data Science");

    String first = createStudent(majorId, "Student One");
    String second = createStudent(majorId, "Student Two");

    int firstSeq = Integer.parseInt(first.substring(4));
    int secondSeq = Integer.parseInt(second.substring(4));
    Assertions.assertEquals(firstSeq + 1, secondSeq);
  }

  @Test
  void shouldUseIndependentSequencePerMajor() {
    long majorA = createMajor("31", "2026", "Major A");
    long majorB = createMajor("32", "2026", "Major B");

    String studentA = createStudent(majorA, "Student A");
    String studentB = createStudent(majorB, "Student B");

    Assertions.assertTrue(studentA.matches("2631\\d{4}"));
    Assertions.assertTrue(studentB.matches("2632\\d{4}"));
  }

  @Test
  void shouldReturn404WhenMajorIdDoesNotExist() {
    given()
        .contentType("application/json")
        .body(
            """
            {
              "name": "Ghost Student",
              "gender": "FEMALE",
              "majorId": 999999999
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
              "majorId": 1
            }
            """)
        .when()
        .post("/api/students")
        .then()
        .statusCode(400);
  }
}
