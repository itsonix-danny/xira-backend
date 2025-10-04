package eu.itsonix.genai.xira.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import eu.itsonix.genai.xira.jpa.repository.WorkflowRepository;
import eu.itsonix.genai.xira.jpa.repository.WorkflowStatusRepository;
import eu.itsonix.genai.xira.web.model.CreateProjectRequest;
import eu.itsonix.genai.xira.web.model.LoginRequest;
import eu.itsonix.genai.xira.web.model.RegisterRequest;
import eu.itsonix.genai.xira.web.model.UpdateProjectRequest;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

class ProjectControllerIntegrationTest extends BaseIntegrationTest {

    private static final String EMAIL = "owner@example.com";
    private static final String PASSWORD = "password";

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private WorkflowStatusRepository workflowStatusRepository;

    @Test
    void givenAuthenticatedUser_whenCreateProject_thenReturnsCreatedProject() {
        registerUser();
        final String token = login();

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new CreateProjectRequest().key("XIRA")
                        .name("Xira Project")
                        .description("Test project description"))
                .when()
                .post("/projects")
                .then()
                .statusCode(201)
                .header(HttpHeaders.LOCATION, equalTo("/projects/XIRA"));

        final var workflows = workflowRepository.findAll();
        assertThat(workflows).hasSize(1);

        final var workflowStatuses = workflowStatusRepository.findAll();
        assertThat(workflowStatuses).hasSize(3);
        assertThat(workflowStatuses.getFirst().getName()).isEqualTo("To Do");
        assertThat(workflowStatuses.get(1).getName()).isEqualTo("In Progress");
        assertThat(workflowStatuses.get(2).getName()).isEqualTo("Done");
    }

    @Test
    void givenDuplicateProjectKey_whenCreateProject_thenReturnsConflict() {
        registerUser();
        final String token = login();

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new CreateProjectRequest().key("XIRA").name("First Project"))
                .when()
                .post("/projects")
                .then()
                .statusCode(201);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new CreateProjectRequest().key("XIRA").name("Duplicate Project"))
                .when()
                .post("/projects")
                .then()
                .statusCode(409);
    }

    @Test
    void givenLowercaseKey_whenCreateProject_thenReturnsBadRequest() {
        registerUser();
        final String token = login();

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new CreateProjectRequest().key("xira").name("Xira Project"))
                .when()
                .post("/projects")
                .then()
                .statusCode(400);
    }

    @Test
    void givenTooShortKey_whenCreateProject_thenReturnsBadRequest() {
        registerUser();
        final String token = login();

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new CreateProjectRequest().key("X").name("Xira Project"))
                .when()
                .post("/projects")
                .then()
                .statusCode(400);
    }

    @Test
    void givenTooLongKey_whenCreateProject_thenReturnsBadRequest() {
        registerUser();
        final String token = login();

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new CreateProjectRequest().key("VERYLONGKEY").name("Xira Project"))
                .when()
                .post("/projects")
                .then()
                .statusCode(400);
    }

    private void registerUser() {
        given().contentType(ContentType.JSON)
                .body(new RegisterRequest().email(EMAIL).password(PASSWORD).firstName("Owner").lastName("Example"))
                .when()
                .post("/auth/register")
                .then()
                .statusCode(201);
    }

    private String login() {
        return given().contentType(ContentType.JSON)
                .body(new LoginRequest().email(EMAIL).password(PASSWORD))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("access_token");
    }

    @Test
    void givenAdmin_whenUpdateProject_thenReturnsSuccess() {
        registerUser();
        final String token = login();

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new CreateProjectRequest().key("XIRA").name("Xira Project").description("Original description"))
                .when()
                .post("/projects")
                .then()
                .statusCode(201);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new UpdateProjectRequest().name("Updated Project").description("Updated description"))
                .when()
                .patch("/projects/XIRA")
                .then()
                .statusCode(200);
    }

    @Test
    void givenNonAdmin_whenUpdateProject_thenReturnsForbidden() {
        registerUser();
        final String ownerToken = login();

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(new CreateProjectRequest().key("XIRA").name("Xira Project"))
                .when()
                .post("/projects")
                .then()
                .statusCode(201);

        given().contentType(ContentType.JSON)
                .body(new RegisterRequest().email("member@example.com")
                        .password(PASSWORD)
                        .firstName("Member")
                        .lastName("User"))
                .when()
                .post("/auth/register")
                .then()
                .statusCode(201);

        final String memberToken = given().contentType(ContentType.JSON)
                .body(new LoginRequest().email("member@example.com").password(PASSWORD))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("access_token");

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", memberToken))
                .body(new UpdateProjectRequest().name("Updated Project"))
                .when()
                .patch("/projects/XIRA")
                .then()
                .statusCode(403);
    }

    @Test
    void givenNonExistentProject_whenUpdateProject_thenReturnsForbidden() {
        registerUser();
        final String token = login();

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new UpdateProjectRequest().name("Updated Project"))
                .when()
                .patch("/projects/NONEXISTENT")
                .then()
                .statusCode(403);
    }

    @Test
    void givenTooLongName_whenUpdateProject_thenReturnsBadRequest() {
        registerUser();
        final String token = login();

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new CreateProjectRequest().key("XIRA").name("Xira Project"))
                .when()
                .post("/projects")
                .then()
                .statusCode(201);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new UpdateProjectRequest().name("A".repeat(101)))
                .when()
                .patch("/projects/XIRA")
                .then()
                .statusCode(400);
    }

    @Test
    void givenTooLongDescription_whenUpdateProject_thenReturnsBadRequest() {
        registerUser();
        final String token = login();

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new CreateProjectRequest().key("XIRA").name("Xira Project"))
                .when()
                .post("/projects")
                .then()
                .statusCode(201);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new UpdateProjectRequest().description("A".repeat(201)))
                .when()
                .patch("/projects/XIRA")
                .then()
                .statusCode(400);
    }

    @Test
    void givenTooLongDescription_whenCreateProject_thenReturnsBadRequest() {
        registerUser();
        final String token = login();

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new CreateProjectRequest().key("XIRA").name("Xira Project").description("A".repeat(201)))
                .when()
                .post("/projects")
                .then()
                .statusCode(400);
    }

    @Test
    void givenUnauthenticated_whenCreateProject_thenReturnsUnauthorized() {
        given().contentType(ContentType.JSON)
                .body(new CreateProjectRequest().key("XIRA").name("Xira Project"))
                .when()
                .post("/projects")
                .then()
                .statusCode(401);
    }

    @Test
    void givenUnauthenticated_whenUpdateProject_thenReturnsUnauthorized() {
        given().contentType(ContentType.JSON)
                .body(new UpdateProjectRequest().name("Updated Project"))
                .when()
                .patch("/projects/XIRA")
                .then()
                .statusCode(401);
    }

    @Test
    void givenOnlyName_whenUpdateProject_thenReturnsSuccess() {
        registerUser();
        final String token = login();

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new CreateProjectRequest().key("XIRA").name("Original Name").description("Original Description"))
                .when()
                .post("/projects")
                .then()
                .statusCode(201);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new UpdateProjectRequest().name("Updated Name"))
                .when()
                .patch("/projects/XIRA")
                .then()
                .statusCode(200);
    }

    @Test
    void givenEmptyName_whenUpdateProject_thenReturnsBadRequest() {
        registerUser();
        final String token = login();

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new CreateProjectRequest().key("XIRA").name("Xira Project"))
                .when()
                .post("/projects")
                .then()
                .statusCode(201);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new UpdateProjectRequest().name(""))
                .when()
                .patch("/projects/XIRA")
                .then()
                .statusCode(400);
    }

    @Test
    void givenEmptyDescription_whenUpdateProject_thenReturnsBadRequest() {
        registerUser();
        final String token = login();

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new CreateProjectRequest().key("XIRA").name("Xira Project"))
                .when()
                .post("/projects")
                .then()
                .statusCode(201);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new UpdateProjectRequest().description(""))
                .when()
                .patch("/projects/XIRA")
                .then()
                .statusCode(400);
    }

    @Test
    void givenOnlyDescription_whenUpdateProject_thenReturnsSuccess() {
        registerUser();
        final String token = login();

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new CreateProjectRequest().key("XIRA").name("Original Name").description("Original Description"))
                .when()
                .post("/projects")
                .then()
                .statusCode(201);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new UpdateProjectRequest().description("Updated Description"))
                .when()
                .patch("/projects/XIRA")
                .then()
                .statusCode(200);
    }
}
