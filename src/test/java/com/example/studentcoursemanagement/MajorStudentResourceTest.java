package com.example.studentcoursemanagement;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class MajorStudentResourceTest {

  @Test
  void shouldCreateMajor() {
    String entryYear = "2026";
    String majorCode = "27";

    given()
        .contentType("application/json")
        .body(
            """
            {
              "majorCode": "%s",
              "entryYear": "%s",
              "name": "Computer Science"
            }
            """
                .formatted(majorCode, entryYear))
        .when()
        .post("/api/majors")
        .then()
        .statusCode(201)
        .body("id", notNullValue())
        .body("majorCode", equalTo(majorCode))
        .body("entryYear", equalTo(entryYear))
        .body("name", equalTo("Computer Science"));
  }

  @Test
  void shouldCreateStudentWithDefaultStatusAndGpa() {
    String entryYear = "2026";
    String majorCode = "28";

    given()
        .contentType("application/json")
        .body(
            """
            {
              "majorCode": "%s",
              "entryYear": "%s",
              "name": "Information Technology"
            }
            """
                .formatted(majorCode, entryYear))
        .when()
        .post("/api/majors")
        .then()
        .statusCode(201);

    given()
        .contentType("application/json")
        .body(
            """
            {
              "name": "Nguyen Van A",
              "gender": "MALE",
              "entryYear": "%s",
              "majorCode": "%s"
            }
            """
                .formatted(entryYear, majorCode))
        .when()
        .post("/api/students")
        .then()
        .statusCode(201)
        .body("id", notNullValue())
        .body("studentId", notNullValue())
        .body("name", equalTo("Nguyen Van A"))
        .body("status", equalTo("STUDYING"))
        .body("gpa", equalTo(0.0F));
  }
}

