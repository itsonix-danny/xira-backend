package eu.itsonix.genai.xira.web;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import eu.itsonix.genai.xira.web.model.AddSprintRequest;
import eu.itsonix.genai.xira.web.model.ProjectMemberRole;
import eu.itsonix.genai.xira.web.model.UpdateSprintRequest;
import io.restassured.http.ContentType;

class SprintControllerIntegrationTest extends BaseIntegrationTest {

    private static final String PROJECT_KEY = "XIRA";
    private static final String OWNER_EMAIL = "owner@example.com";
    private static final String OWNER_PASSWORD = "password";

    @Test
    void givenProjectMember_whenCreateSprint_thenReturnsCreated() {
        final String ownerToken = prepareProjectOwner();

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                .body(new AddSprintRequest().name("Sprint 1").goal("Deliver MVP"))
                .when()
                .post("/projects/{key}/sprints", PROJECT_KEY)
                .then()
                .statusCode(201)
                .header(HttpHeaders.LOCATION, matchesPattern("^/projects/XIRA/sprints/.+$"))
                .body("name", equalTo("Sprint 1"))
                .body("goal", equalTo("Deliver MVP"))
                .body("state", equalTo("PLANNED"))
                .body("startedAt", nullValue())
                .body("finishedAt", nullValue());
    }

    @Test
    void givenNonMember_whenCreateSprint_thenReturnsForbidden() {
        prepareProjectOwner();

        registerUser("member@example.com", OWNER_PASSWORD, "Member", "User");
        final String memberToken = login("member@example.com", OWNER_PASSWORD);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(memberToken))
                .body(new AddSprintRequest().name("Sprint 1").goal("Deliver MVP"))
                .when()
                .post("/projects/{key}/sprints", PROJECT_KEY)
                .then()
                .statusCode(403);
    }

    @Test
    void givenProjectMember_whenUpdateSprint_thenReturnsUpdated() {
        final String ownerToken = prepareProjectOwner();
        final String sprintId = createSprint(ownerToken, "Sprint 1", "Deliver MVP");

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                .body(new UpdateSprintRequest().name("Updated Sprint").goal("Updated goal"))
                .when()
                .patch("/projects/{key}/sprints/{sprintId}", PROJECT_KEY, UUID.fromString(sprintId))
                .then()
                .statusCode(200)
                .body("name", equalTo("Updated Sprint"))
                .body("goal", equalTo("Updated goal"));
    }

    @Test
    void givenFinishedSprint_whenUpdateSprint_thenReturnsConflict() {
        final String ownerToken = prepareProjectOwner();
        final String sprintId = createSprint(ownerToken, "Sprint 1", "Deliver MVP");
        startSprint(ownerToken, sprintId);
        finishSprint(ownerToken, sprintId);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                .body(new UpdateSprintRequest().name("Updated Sprint"))
                .when()
                .patch("/projects/{key}/sprints/{sprintId}", PROJECT_KEY, UUID.fromString(sprintId))
                .then()
                .statusCode(409)
                .body("detail", equalTo("Finished sprints cannot be updated"));
    }

    @Test
    void givenProjectMember_whenDeleteSprint_thenReturnsNoContent() {
        final String ownerToken = prepareProjectOwner();
        final String sprintId = createSprint(ownerToken, "Sprint 1", "Deliver MVP");

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                .when()
                .delete("/projects/{key}/sprints/{sprintId}", PROJECT_KEY, UUID.fromString(sprintId))
                .then()
                .statusCode(204);
    }

    @Test
    void givenActiveSprint_whenDeleteSprint_thenReturnsConflict() {
        final String ownerToken = prepareProjectOwner();
        final String sprintId = createSprint(ownerToken, "Sprint 1", "Deliver MVP");
        startSprint(ownerToken, sprintId);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                .when()
                .delete("/projects/{key}/sprints/{sprintId}", PROJECT_KEY, UUID.fromString(sprintId))
                .then()
                .statusCode(409)
                .body("detail", equalTo("Only planned sprints can be deleted"));
    }

    @Test
    void givenProjectMember_whenStartSprint_thenReturnsActive() {
        final String ownerToken = prepareProjectOwner();
        final String sprintId = createSprint(ownerToken, "Sprint 1", "Deliver MVP");

        registerUser("developer@example.com", OWNER_PASSWORD, "Dev", "User");
        final String developerToken = login("developer@example.com", OWNER_PASSWORD);
        final String developerId = getUserIdByEmail(ownerToken, "developer@example.com");
        addProjectMember(ownerToken, developerId, ProjectMemberRole.DEVELOPER);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(developerToken))
                .when()
                .post("/projects/{key}/sprints/{sprintId}/start", PROJECT_KEY, UUID.fromString(sprintId))
                .then()
                .statusCode(200)
                .body("state", equalTo("ACTIVE"))
                .body("startedAt", notNullValue())
                .body("finishedAt", nullValue());
    }

    @Test
    void givenFinishedSprint_whenStartSprint_thenReturnsConflict() {
        final String ownerToken = prepareProjectOwner();
        final String sprintId = createSprint(ownerToken, "Sprint 1", "Deliver MVP");
        startSprint(ownerToken, sprintId);
        finishSprint(ownerToken, sprintId);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                .when()
                .post("/projects/{key}/sprints/{sprintId}/start", PROJECT_KEY, UUID.fromString(sprintId))
                .then()
                .statusCode(409)
                .body("detail", equalTo("Finished sprints cannot be started"));
    }

    @Test
    void givenProjectMember_whenFinishSprint_thenReturnsClosed() {
        final String ownerToken = prepareProjectOwner();
        final String sprintId = createSprint(ownerToken, "Sprint 1", "Deliver MVP");
        startSprint(ownerToken, sprintId);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                .when()
                .post("/projects/{key}/sprints/{sprintId}/finish", PROJECT_KEY, UUID.fromString(sprintId))
                .then()
                .statusCode(200)
                .body("state", equalTo("CLOSED"))
                .body("finishedAt", notNullValue());
    }

    @Test
    void givenNonActiveSprint_whenFinishSprint_thenReturnsConflict() {
        final String ownerToken = prepareProjectOwner();
        final String sprintId = createSprint(ownerToken, "Sprint 1", "Deliver MVP");

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                .when()
                .post("/projects/{key}/sprints/{sprintId}/finish", PROJECT_KEY, UUID.fromString(sprintId))
                .then()
                .statusCode(409)
                .body("detail", equalTo("Only active sprints can be finished"));
    }

    private String prepareProjectOwner() {
        registerUser(OWNER_EMAIL, OWNER_PASSWORD, "Owner", "User");
        final String ownerToken = login(OWNER_EMAIL, OWNER_PASSWORD);
        createProject(ownerToken);
        return ownerToken;
    }

    private String bearer(final String token) {
        return String.format("Bearer %s", token);
    }

    private String createSprint(final String token, final String name, final String goal) {
        return given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .body(new AddSprintRequest().name(name).goal(goal))
                .when()
                .post("/projects/{key}/sprints", PROJECT_KEY)
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getString("id");
    }

    private void startSprint(final String token, final String sprintId) {
        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .when()
                .post("/projects/{key}/sprints/{sprintId}/start", PROJECT_KEY, UUID.fromString(sprintId))
                .then()
                .statusCode(200);
    }

    private void finishSprint(final String token, final String sprintId) {
        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .when()
                .post("/projects/{key}/sprints/{sprintId}/finish", PROJECT_KEY, UUID.fromString(sprintId))
                .then()
                .statusCode(200);
    }
}
