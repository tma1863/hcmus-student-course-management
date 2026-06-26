package com.example.studentcoursemanagement;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import com.example.studentcoursemanagement.course.dao.CourseRepository;
import com.example.studentcoursemanagement.course.entity.Course;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class CourseMajorResourceTest {

  @Inject CourseRepository courseRepository;

  // --- helpers ------------------------------------------------------------

  /** Courses have no creation API (they are seeded), so insert them directly in a committed tx. */
  private void seedCourse(String courseId, String name) {
    QuarkusTransaction.requiringNew()
        .run(
            () -> courseRepository.persist(Course.builder().courseId(courseId).name(name).build()));
  }

  private long createMajor(String majorCode, String name) {
    return given()
        .contentType("application/json")
        .body("{\"majorCode\":\"%s\",\"name\":\"%s\"}".formatted(majorCode, name))
        .when()
        .post("/api/majors")
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");
  }

  private long createProgram(long majorId, String entryYear) {
    return given()
        .contentType("application/json")
        .body("{\"entryYear\":\"%s\"}".formatted(entryYear))
        .when()
        .post("/api/majors/" + majorId + "/major-programs")
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");
  }

  private String addCoursePath(long majorId, long programId) {
    return "/api/majors/" + majorId + "/major-programs/" + programId + "/courses";
  }

  private String reportPath(long majorId, long programId) {
    return "/api/majors/" + majorId + "/major-programs/" + programId + "/report";
  }

  /**
   * Builds the add-course JSON body; pass {@code prereqs} as already-quoted JSON array contents.
   */
  private String addBody(
      String courseId, int credits, int semester, boolean required, String prereqsJson) {
    return """
        {
          "courseId": "%s",
          "credits": %d,
          "programSemester": %d,
          "isRequired": %b,
          "prerequisiteCourseIds": [%s]
        }
        """
        .formatted(courseId, credits, semester, required, prereqsJson);
  }

  // --- tests --------------------------------------------------------------

  @Test
  void shouldAddCoursesAndReturnReportGroupedAndOrderedBySemester() {
    long majorId = createMajor("40", "Data Science");
    long programId = createProgram(majorId, "2025");

    seedCourse("DSC01009", "Required Foundations"); // required, but higher code than the elective
    seedCourse("DSC01001", "Elective Seminar");
    seedCourse("DSC01005", "Advanced Topics");

    // Semester 1: one required (credits 4) + one elective (credits 2).
    given()
        .contentType("application/json")
        .body(addBody("DSC01009", 4, 1, true, ""))
        .when()
        .post(addCoursePath(majorId, programId))
        .then()
        .statusCode(201)
        .body("courseId", equalTo("DSC01009"))
        .body("majorProgramId", equalTo((int) programId));

    given()
        .contentType("application/json")
        .body(addBody("DSC01001", 2, 1, false, ""))
        .when()
        .post(addCoursePath(majorId, programId))
        .then()
        .statusCode(201);

    // Semester 2: required (credits 3) with a prerequisite on the semester-1 foundations course.
    given()
        .contentType("application/json")
        .body(addBody("DSC01005", 3, 2, true, "\"DSC01009\""))
        .when()
        .post(addCoursePath(majorId, programId))
        .then()
        .statusCode(201)
        .body("prerequisiteCourseIds", hasSize(1))
        .body("prerequisiteCourseIds[0]", equalTo("DSC01009"));

    given()
        .when()
        .get(reportPath(majorId, programId))
        .then()
        .statusCode(200)
        .body("id", equalTo((int) programId))
        .body("majorId", equalTo((int) majorId))
        .body("majorCode", equalTo("40"))
        .body("name", equalTo("Data Science"))
        .body("entryYear", equalTo("2025"))
        // Totals are backend-derived: required 4+3=7, optional 2.
        .body("totalRequiredCredit", equalTo(7))
        .body("totalOptionalCredit", equalTo(2))
        .body("semesters", hasSize(2))
        // Ascending semester order.
        .body("semesters[0].semester", equalTo(1))
        .body("semesters[1].semester", equalTo(2))
        // Within semester 1: required-first (DSC01009) before the elective (DSC01001),
        // even though DSC01001 sorts earlier by code.
        .body("semesters[0].courses", hasSize(2))
        .body("semesters[0].courses[0].courseId", equalTo("DSC01009"))
        .body("semesters[0].courses[0].isRequired", equalTo(true))
        .body("semesters[0].courses[1].courseId", equalTo("DSC01001"))
        .body("semesters[0].courses[1].isRequired", equalTo(false))
        // Semester 2 course carries its prerequisite.
        .body("semesters[1].courses[0].courseId", equalTo("DSC01005"))
        .body("semesters[1].courses[0].prerequisites", hasSize(1))
        .body("semesters[1].courses[0].prerequisites[0].courseId", equalTo("DSC01009"))
        .body("semesters[1].courses[0].prerequisites[0].name", equalTo("Required Foundations"));
  }

  @Test
  void shouldRecalculateCreditsWhenCourseRemoved() {
    long majorId = createMajor("41", "Information Systems");
    long programId = createProgram(majorId, "2025");

    seedCourse("DEL01001", "Required Course");
    seedCourse("DEL01002", "Elective Course");

    given()
        .contentType("application/json")
        .body(addBody("DEL01001", 4, 1, true, ""))
        .when()
        .post(addCoursePath(majorId, programId))
        .then()
        .statusCode(201);
    given()
        .contentType("application/json")
        .body(addBody("DEL01002", 2, 1, false, ""))
        .when()
        .post(addCoursePath(majorId, programId))
        .then()
        .statusCode(201);

    given()
        .when()
        .get(reportPath(majorId, programId))
        .then()
        .statusCode(200)
        .body("totalRequiredCredit", equalTo(4))
        .body("totalOptionalCredit", equalTo(2));

    // Remove the elective -> optional total recomputed to 0.
    given().when().delete(addCoursePath(majorId, programId) + "/DEL01002").then().statusCode(204);

    given()
        .when()
        .get(reportPath(majorId, programId))
        .then()
        .statusCode(200)
        .body("totalRequiredCredit", equalTo(4))
        .body("totalOptionalCredit", equalTo(0))
        .body("semesters[0].courses", hasSize(1))
        .body("semesters[0].courses[0].courseId", equalTo("DEL01001"));
  }

  @Test
  void shouldRejectCyclicPrerequisite() {
    long majorId = createMajor("42", "Cyber");
    long programId = createProgram(majorId, "2025");

    seedCourse("CYC01001", "Course A");
    seedCourse("CYC01002", "Course B");
    seedCourse("CYC01003", "Course C");

    // A requires B (B not yet in the program is fine).
    given()
        .contentType("application/json")
        .body(addBody("CYC01001", 3, 1, true, "\"CYC01002\""))
        .when()
        .post(addCoursePath(majorId, programId))
        .then()
        .statusCode(201);

    // Adding B requiring A closes the loop A -> B -> A -> ... : rejected.
    given()
        .contentType("application/json")
        .body(addBody("CYC01002", 3, 2, true, "\"CYC01001\""))
        .when()
        .post(addCoursePath(majorId, programId))
        .then()
        .statusCode(409);

    // A course requiring itself is a trivial cycle.
    given()
        .contentType("application/json")
        .body(addBody("CYC01003", 3, 1, true, "\"CYC01003\""))
        .when()
        .post(addCoursePath(majorId, programId))
        .then()
        .statusCode(409);
  }

  @Test
  void shouldRejectDuplicateAndUnknownCourse() {
    long majorId = createMajor("43", "Mathematics");
    long programId = createProgram(majorId, "2025");

    seedCourse("DUP01001", "Algebra");

    given()
        .contentType("application/json")
        .body(addBody("DUP01001", 3, 1, true, ""))
        .when()
        .post(addCoursePath(majorId, programId))
        .then()
        .statusCode(201);

    // Same course twice in one program -> 409.
    given()
        .contentType("application/json")
        .body(addBody("DUP01001", 3, 1, true, ""))
        .when()
        .post(addCoursePath(majorId, programId))
        .then()
        .statusCode(409);

    // Unknown course business key -> 404.
    given()
        .contentType("application/json")
        .body(addBody("ZZZ99999", 3, 1, true, ""))
        .when()
        .post(addCoursePath(majorId, programId))
        .then()
        .statusCode(404);
  }

  @Test
  void shouldReturn404WhenReportRequestedForWrongMajor() {
    long majorId = createMajor("44", "Physics");
    long programId = createProgram(majorId, "2025");
    long otherMajorId = createMajor("45", "Chemistry");

    given().when().get(reportPath(otherMajorId, programId)).then().statusCode(404);
  }

  @Test
  void shouldReturnEmptyReportForProgramWithoutCourses() {
    long majorId = createMajor("46", "Linguistics");
    long programId = createProgram(majorId, "2025");

    given()
        .when()
        .get(reportPath(majorId, programId))
        .then()
        .statusCode(200)
        .body("id", notNullValue())
        .body("totalRequiredCredit", equalTo(0))
        .body("totalOptionalCredit", equalTo(0))
        .body("semesters", hasSize(0));
  }
}
