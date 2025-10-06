package eu.itsonix.genai.xira.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import eu.itsonix.genai.xira.web.model.ProjectMemberRole;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

class WorkflowControllerIntegrationTest extends BaseIntegrationTest {

    @Test
    void givenProjectMember_whenGetWorkflowStatuses_thenReturns200WithStatuses() {
        registerUser("user@example.com", "password", "John", "Doe");
        final String token = login("user@example.com", "password");
        createProject(token);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .when()
                .get("/projects/{key}/workflows/statuses", "XIRA")
                .then()
                .statusCode(200)
                .body("$.size()", is(3))
                .body("[0].name", is("To Do"))
                .body("[0].category", is("TODO"))
                .body("[1].name", is("In Progress"))
                .body("[1].category", is("IN_PROGRESS"))
                .body("[2].name", is("Done"))
                .body("[2].category", is("DONE"));
    }

    @Test
    void givenNonMember_whenGetWorkflowStatuses_thenReturns403() {
        registerUser("owner@example.com", "password", "Owner", "User");
        final String ownerToken = login("owner@example.com", "password");
        createProject(ownerToken);

        registerUser("nonmember@example.com", "password", "Non", "Member");
        final String nonMemberToken = login("nonmember@example.com", "password");

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", nonMemberToken))
                .when()
                .get("/projects/{key}/workflows/statuses", "XIRA")
                .then()
                .statusCode(403);
    }

    @Test
    void givenDeveloper_whenGetWorkflowStatuses_thenReturns200WithStatuses() {
        registerUser("admin@example.com", "password", "Admin", "User");
        final String adminToken = login("admin@example.com", "password");
        createProject(adminToken);

        registerUser("developer@example.com", "password", "Dev", "User");
        final String developerToken = login("developer@example.com", "password");
        final String developerId = getUserIdByEmail(adminToken, "developer@example.com");
        addProjectMember(adminToken, developerId, ProjectMemberRole.DEVELOPER);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", developerToken))
                .when()
                .get("/projects/{key}/workflows/statuses", "XIRA")
                .then()
                .statusCode(200)
                .body("$.size()", is(3));
    }

    @Test
    void givenUnauthenticated_whenGetWorkflowStatuses_thenReturns401() {
        given().contentType(ContentType.JSON)
                .when()
                .get("/projects/{key}/workflows/statuses", "XIRA")
                .then()
                .statusCode(401);
    }

    @Test
    void givenInvalidProjectKey_whenGetWorkflowStatuses_thenReturns403() {
        registerUser("user@example.com", "password", "John", "Doe");
        final String token = login("user@example.com", "password");

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .when()
                .get("/projects/{key}/workflows/statuses", "INVALID")
                .then()
                .statusCode(403);
    }
}
