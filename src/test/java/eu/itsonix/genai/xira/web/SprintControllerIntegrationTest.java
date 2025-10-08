package eu.itsonix.genai.xira.web;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import eu.itsonix.genai.xira.web.model.AddSprintRequest;
import eu.itsonix.genai.xira.web.model.CreateIssueRequest;
import eu.itsonix.genai.xira.web.model.UpdateSprintRequest;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

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
                .header(HttpHeaders.LOCATION, matchesPattern("^/projects/XIRA/sprints/.+$"));
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
        final String sprintId = createSprint(ownerToken);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                .body(new UpdateSprintRequest().name("Updated Sprint").goal("Updated goal"))
                .when()
                .patch("/projects/{key}/sprints/{sprintId}", PROJECT_KEY, UUID.fromString(sprintId))
                .then()
                .statusCode(200);
    }

    @Test
    void givenFinishedSprint_whenUpdateSprint_thenReturnsConflict() {
        final String ownerToken = prepareProjectOwner();
        final String sprintId = createSprint(ownerToken);
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
        final String sprintId = createSprint(ownerToken);

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
        final String sprintId = createSprint(ownerToken);
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
    void givenPlannedSprint_whenStartSprint_thenReturnsNoContent() {
        final String ownerToken = prepareProjectOwner();
        final String sprintId = createSprint(ownerToken);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                .when()
                .post("/projects/{key}/sprints/{sprintId}/start", PROJECT_KEY, UUID.fromString(sprintId))
                .then()
                .statusCode(204);
    }

    @Test
    void givenFinishedSprint_whenStartSprint_thenReturnsConflict() {
        final String ownerToken = prepareProjectOwner();
        final String sprintId = createSprint(ownerToken);
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
    void givenActiveSprint_whenFinishSprint_thenReturnsNoContent() {
        final String ownerToken = prepareProjectOwner();
        final String sprintId = createSprint(ownerToken);
        startSprint(ownerToken, sprintId);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                .when()
                .post("/projects/{key}/sprints/{sprintId}/finish", PROJECT_KEY, UUID.fromString(sprintId))
                .then()
                .statusCode(204);
    }

    @Test
    void givenNonActiveSprint_whenFinishSprint_thenReturnsConflict() {
        final String ownerToken = prepareProjectOwner();
        final String sprintId = createSprint(ownerToken);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                .when()
                .post("/projects/{key}/sprints/{sprintId}/finish", PROJECT_KEY, UUID.fromString(sprintId))
                .then()
                .statusCode(409)
                .body("detail", equalTo("Only active sprints can be finished"));
    }

    private String prepareProjectOwner() {
        return prepareProjectOwner(OWNER_EMAIL, OWNER_PASSWORD);
    }

    private String createSprint(final String token) {
        final String location = given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .body(new AddSprintRequest().name("Sprint 1").goal("Deliver MVP"))
                .when()
                .post("/projects/{key}/sprints", PROJECT_KEY)
                .then()
                .statusCode(201)
                .extract()
                .header(HttpHeaders.LOCATION);
        return extractIdFromLocation(location);
    }

    private void startSprint(final String token, final String sprintId) {
        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .when()
                .post("/projects/{key}/sprints/{sprintId}/start", PROJECT_KEY, UUID.fromString(sprintId))
                .then()
                .statusCode(204);
    }

    private void finishSprint(final String token, final String sprintId) {
        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .when()
                .post("/projects/{key}/sprints/{sprintId}/finish", PROJECT_KEY, UUID.fromString(sprintId))
                .then()
                .statusCode(204);
    }

    @Test
    void givenProjectMemberAndIssue_whenAddIssueToSprint_thenReturnsCreated() {
        final String ownerToken = prepareProjectOwner();
        final String sprintId = createSprint(ownerToken);
        final String issueKey = createIssue(ownerToken);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                .when()
                .post("/projects/{key}/sprints/{sprintId}/issues/{issueKey}", PROJECT_KEY, UUID.fromString(sprintId),
                        issueKey)
                .then()
                .statusCode(201);
    }

    @Test
    void givenFinishedSprint_whenAddIssueToSprint_thenReturnsConflict() {
        final String ownerToken = prepareProjectOwner();
        final String sprintId = createSprint(ownerToken);
        final String issueKey = createIssue(ownerToken);
        startSprint(ownerToken, sprintId);
        finishSprint(ownerToken, sprintId);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                .when()
                .post("/projects/{key}/sprints/{sprintId}/issues/{issueKey}", PROJECT_KEY, UUID.fromString(sprintId),
                        issueKey)
                .then()
                .statusCode(409)
                .body("detail", equalTo("Cannot add issues to a finished sprint"));
    }

    @Test
    void givenNonExistentIssue_whenAddIssueToSprint_thenReturnsNotFound() {
        final String ownerToken = prepareProjectOwner();
        final String sprintId = createSprint(ownerToken);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                .when()
                .post("/projects/{key}/sprints/{sprintId}/issues/{issueKey}", PROJECT_KEY, UUID.fromString(sprintId),
                        "XIRA-99")
                .then()
                .statusCode(404);
    }

    @Test
    void givenIssueAlreadyInSprint_whenAddIssueToSprint_thenReturnsConflict() {
        final String ownerToken = prepareProjectOwner();
        final String sprintId = createSprint(ownerToken);
        final String issueKey = createIssue(ownerToken);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                .when()
                .post("/projects/{key}/sprints/{sprintId}/issues/{issueKey}", PROJECT_KEY, UUID.fromString(sprintId),
                        issueKey)
                .then()
                .statusCode(201);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                .when()
                .post("/projects/{key}/sprints/{sprintId}/issues/{issueKey}", PROJECT_KEY, UUID.fromString(sprintId),
                        issueKey)
                .then()
                .statusCode(409)
                .body("detail", equalTo("Issue is already in this sprint"));
    }

    @Test
    void givenNonMember_whenAddIssueToSprint_thenReturnsForbidden() {
        final String ownerToken = prepareProjectOwner();
        final String sprintId = createSprint(ownerToken);
        final String issueKey = createIssue(ownerToken);

        registerUser("nonmember@example.com", OWNER_PASSWORD, "Non", "Member");
        final String nonMemberToken = login("nonmember@example.com", OWNER_PASSWORD);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(nonMemberToken))
                .when()
                .post("/projects/{key}/sprints/{sprintId}/issues/{issueKey}", PROJECT_KEY, UUID.fromString(sprintId),
                        issueKey)
                .then()
                .statusCode(403);
    }

    @Test
    void givenIssueInSprint_whenAddIssueToAnotherSprint_thenMovesIssue() {
        final String ownerToken = prepareProjectOwner();
        final String sprint1Id = createSprint(ownerToken);

        final String sprint2Location = given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                .body(new AddSprintRequest().name("Sprint 2").goal("Second sprint"))
                .when()
                .post("/projects/{key}/sprints", PROJECT_KEY)
                .then()
                .statusCode(201)
                .extract()
                .header(HttpHeaders.LOCATION);
        final String sprint2Id = extractIdFromLocation(sprint2Location);

        final String issueKey = createIssue(ownerToken);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                .when()
                .post("/projects/{key}/sprints/{sprintId}/issues/{issueKey}", PROJECT_KEY, UUID.fromString(sprint1Id),
                        issueKey)
                .then()
                .statusCode(201);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                .when()
                .post("/projects/{key}/sprints/{sprintId}/issues/{issueKey}", PROJECT_KEY, UUID.fromString(sprint2Id),
                        issueKey)
                .then()
                .statusCode(201);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                .when()
                .delete("/projects/{key}/sprints/{sprintId}/issues/{issueKey}", PROJECT_KEY, UUID.fromString(sprint1Id),
                        issueKey)
                .then()
                .statusCode(404)
                .body("detail", equalTo("Issue is not in this sprint"));
    }

    @Test
    void givenProjectMemberAndIssueInSprint_whenRemoveIssueFromSprint_thenReturnsNoContent() {
        final String ownerToken = prepareProjectOwner();
        final String sprintId = createSprint(ownerToken);
        final String issueKey = createIssue(ownerToken);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                .when()
                .post("/projects/{key}/sprints/{sprintId}/issues/{issueKey}", PROJECT_KEY, UUID.fromString(sprintId),
                        issueKey)
                .then()
                .statusCode(201);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                .when()
                .delete("/projects/{key}/sprints/{sprintId}/issues/{issueKey}", PROJECT_KEY, UUID.fromString(sprintId),
                        issueKey)
                .then()
                .statusCode(204);
    }

    @Test
    void givenFinishedSprint_whenRemoveIssueFromSprint_thenReturnsConflict() {
        final String ownerToken = prepareProjectOwner();
        final String sprintId = createSprint(ownerToken);
        final String issueKey = createIssue(ownerToken);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                .when()
                .post("/projects/{key}/sprints/{sprintId}/issues/{issueKey}", PROJECT_KEY, UUID.fromString(sprintId),
                        issueKey)
                .then()
                .statusCode(201);

        startSprint(ownerToken, sprintId);
        finishSprint(ownerToken, sprintId);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                .when()
                .delete("/projects/{key}/sprints/{sprintId}/issues/{issueKey}", PROJECT_KEY, UUID.fromString(sprintId),
                        issueKey)
                .then()
                .statusCode(409)
                .body("detail", equalTo("Cannot remove issues from a finished sprint"));
    }

    @Test
    void givenIssueNotInSprint_whenRemoveIssueFromSprint_thenReturnsNotFound() {
        final String ownerToken = prepareProjectOwner();
        final String sprintId = createSprint(ownerToken);
        final String issueKey = createIssue(ownerToken);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                .when()
                .delete("/projects/{key}/sprints/{sprintId}/issues/{issueKey}", PROJECT_KEY, UUID.fromString(sprintId),
                        issueKey)
                .then()
                .statusCode(404)
                .body("detail", equalTo("Issue is not in this sprint"));
    }

    private String createIssue(final String token) {
        final String location = given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .body(new CreateIssueRequest().title("Test Issue")
                        .issueType(CreateIssueRequest.IssueTypeEnum.TASK)
                        .priority(CreateIssueRequest.PriorityEnum.MEDIUM))
                .when()
                .post("/projects/{key}/issues", PROJECT_KEY)
                .then()
                .statusCode(201)
                .extract()
                .header(HttpHeaders.LOCATION);
        return location.substring(location.lastIndexOf('/') + 1);
    }
}
