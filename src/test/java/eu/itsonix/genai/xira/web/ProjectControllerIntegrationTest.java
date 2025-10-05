package eu.itsonix.genai.xira.web;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import eu.itsonix.genai.xira.jpa.repository.WorkflowRepository;
import eu.itsonix.genai.xira.jpa.repository.WorkflowStatusRepository;
import eu.itsonix.genai.xira.web.model.*;
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
        registerUser(EMAIL, "Owner", "Example");
    }

    private void registerUser(final String email, final String firstName, final String lastName) {
        given().contentType(ContentType.JSON)
                .body(new RegisterRequest().email(email)
                        .password(ProjectControllerIntegrationTest.PASSWORD)
                        .firstName(firstName)
                        .lastName(lastName))
                .when()
                .post("/auth/register")
                .then()
                .statusCode(201);
    }

    private String login() {
        return login(EMAIL);
    }

    private String login(final String email) {
        return given().contentType(ContentType.JSON)
                .body(new LoginRequest().email(email).password(ProjectControllerIntegrationTest.PASSWORD))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("access_token");
    }

    private String getUserIdByEmail(final String token, final String email) {
        return given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .queryParam("email", email)
                .when()
                .get("/users")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("id");
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

    @Test
    void givenAdmin_whenAddProjectMemberAsAdmin_thenMemberCanUpdateProject() {
        registerUser();
        final String ownerToken = login();

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(new CreateProjectRequest().key("XIRA").name("Xira Project"))
                .when()
                .post("/projects")
                .then()
                .statusCode(201);

        final String adminEmail = "admin.member@example.com";
        registerUser(adminEmail, "Admin", "Member");
        final String adminToken = login(adminEmail);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", adminToken))
                .body(new UpdateProjectRequest().description("Attempt"))
                .when()
                .patch("/projects/XIRA")
                .then()
                .statusCode(403);

        final String adminUserId = getUserIdByEmail(ownerToken, adminEmail);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(new AddProjectMemberRequest().userId(UUID.fromString(adminUserId)).role(ProjectMemberRole.ADMIN))
                .when()
                .post("/projects/XIRA/members")
                .then()
                .statusCode(201);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", adminToken))
                .body(new UpdateProjectRequest().name("Updated By Admin"))
                .when()
                .patch("/projects/XIRA")
                .then()
                .statusCode(200);
    }

    @Test
    void givenAdmin_whenAddExistingProjectMember_thenReturnsConflict() {
        registerUser();
        final String ownerToken = login();

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(new CreateProjectRequest().key("XIRA").name("Xira Project"))
                .when()
                .post("/projects")
                .then()
                .statusCode(201);

        final String memberEmail = "member@example.com";
        registerUser(memberEmail, "Member", "User");

        final String memberUserId = getUserIdByEmail(ownerToken, memberEmail);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(new AddProjectMemberRequest().userId(UUID.fromString(memberUserId)).role(ProjectMemberRole.DEVELOPER))
                .when()
                .post("/projects/XIRA/members")
                .then()
                .statusCode(201);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(new AddProjectMemberRequest().userId(UUID.fromString(memberUserId)).role(ProjectMemberRole.DEVELOPER))
                .when()
                .post("/projects/XIRA/members")
                .then()
                .statusCode(409);
    }

    @Test
    void givenAdmin_whenRemoveProjectMember_thenMemberCannotUpdateProject() {
        registerUser();
        final String ownerToken = login();

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(new CreateProjectRequest().key("XIRA").name("Xira Project"))
                .when()
                .post("/projects")
                .then()
                .statusCode(201);

        final String adminEmail = "admin.remove@example.com";
        registerUser(adminEmail, "Remove", "Admin");
        final String adminToken = login(adminEmail);

        final String adminUserId = getUserIdByEmail(ownerToken, adminEmail);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(new AddProjectMemberRequest().userId(UUID.fromString(adminUserId)).role(ProjectMemberRole.ADMIN))
                .when()
                .post("/projects/XIRA/members")
                .then()
                .statusCode(201);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .when()
                .delete("/projects/XIRA/members/{userId}", adminUserId)
                .then()
                .statusCode(204);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", adminToken))
                .body(new UpdateProjectRequest().description("Attempt After Removal"))
                .when()
                .patch("/projects/XIRA")
                .then()
                .statusCode(403);
    }

    @Test
    void givenAdmin_whenUpdateProjectMemberRole_thenMemberCanUpdateProject() {
        registerUser();
        final String ownerToken = login();

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(new CreateProjectRequest().key("XIRA").name("Xira Project"))
                .when()
                .post("/projects")
                .then()
                .statusCode(201);

        final String developerEmail = "developer.role@example.com";
        registerUser(developerEmail, "Role", "Developer");
        final String developerToken = login(developerEmail);

        final String developerUserId = getUserIdByEmail(ownerToken, developerEmail);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(new AddProjectMemberRequest().userId(UUID.fromString(developerUserId)).role(ProjectMemberRole.DEVELOPER))
                .when()
                .post("/projects/XIRA/members")
                .then()
                .statusCode(201);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", developerToken))
                .body(new UpdateProjectRequest().description("Attempt"))
                .when()
                .patch("/projects/XIRA")
                .then()
                .statusCode(403);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(new UpdateProjectMemberRoleRequest().role(ProjectMemberRole.ADMIN))
                .when()
                .patch("/projects/XIRA/members/{userId}", developerUserId)
                .then()
                .statusCode(200);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", developerToken))
                .body(new UpdateProjectRequest().name("Updated After Promotion"))
                .when()
                .patch("/projects/XIRA")
                .then()
                .statusCode(200);
    }

    @Test
    void givenOwner_whenUpdateOwnRoleToDeveloper_thenReturnsConflict() {
        registerUser();
        final String ownerToken = login();

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(new CreateProjectRequest().key("XIRA").name("Xira Project"))
                .when()
                .post("/projects")
                .then()
                .statusCode(201);

        final String ownerUserId = getUserIdByEmail(ownerToken, EMAIL);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(new UpdateProjectMemberRoleRequest().role(ProjectMemberRole.DEVELOPER))
                .when()
                .patch("/projects/XIRA/members/{userId}", ownerUserId)
                .then()
                .statusCode(409);
    }

    @Test
    void givenNonAdmin_whenUpdateProjectMemberRole_thenReturnsForbidden() {
        registerUser();
        final String ownerToken = login();

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(new CreateProjectRequest().key("XIRA").name("Xira Project"))
                .when()
                .post("/projects")
                .then()
                .statusCode(201);

        final String developerEmail = "developer.update.role@example.com";
        registerUser(developerEmail, "Update", "Developer");
        final String developerUserId = getUserIdByEmail(ownerToken, developerEmail);
        final String developerToken = login(developerEmail);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(new AddProjectMemberRequest().userId(UUID.fromString(developerUserId)).role(ProjectMemberRole.DEVELOPER))
                .when()
                .post("/projects/XIRA/members")
                .then()
                .statusCode(201);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", developerToken))
                .body(new UpdateProjectMemberRoleRequest().role(ProjectMemberRole.ADMIN))
                .when()
                .patch("/projects/XIRA/members/{userId}", developerUserId)
                .then()
                .statusCode(403);
    }

    @Test
    void givenNonAdmin_whenAddProjectMember_thenReturnsForbidden() {
        registerUser();
        final String ownerToken = login();

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(new CreateProjectRequest().key("XIRA").name("Xira Project"))
                .when()
                .post("/projects")
                .then()
                .statusCode(201);

        final String developerEmail = "developer@example.com";
        registerUser(developerEmail, "Dev", "User");

        final String developerUserId = getUserIdByEmail(ownerToken, developerEmail);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(new AddProjectMemberRequest().userId(UUID.fromString(developerUserId)).role(ProjectMemberRole.DEVELOPER))
                .when()
                .post("/projects/XIRA/members")
                .then()
                .statusCode(201);

        final String developerToken = login(developerEmail);

        final String newMemberEmail = "new.member@example.com";
        registerUser(newMemberEmail, "New", "Member");

        final String newMemberUserId = getUserIdByEmail(ownerToken, newMemberEmail);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", developerToken))
                .body(new AddProjectMemberRequest().userId(UUID.fromString(newMemberUserId)).role(ProjectMemberRole.DEVELOPER))
                .when()
                .post("/projects/XIRA/members")
                .then()
                .statusCode(403);
    }

    @Test
    void givenNonAdmin_whenRemoveProjectMember_thenReturnsForbidden() {
        registerUser();
        final String ownerToken = login();

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(new CreateProjectRequest().key("XIRA").name("Xira Project"))
                .when()
                .post("/projects")
                .then()
                .statusCode(201);

        final String developerEmail = "developer.remove@example.com";
        registerUser(developerEmail, "Dev", "Remove");

        final String developerUserId = getUserIdByEmail(ownerToken, developerEmail);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(new AddProjectMemberRequest().userId(UUID.fromString(developerUserId)).role(ProjectMemberRole.DEVELOPER))
                .when()
                .post("/projects/XIRA/members")
                .then()
                .statusCode(201);

        final String developerToken = login(developerEmail);

        final String targetEmail = "target@example.com";
        registerUser(targetEmail, "Target", "User");

        final String targetUserId = getUserIdByEmail(ownerToken, targetEmail);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(new AddProjectMemberRequest().userId(UUID.fromString(targetUserId)).role(ProjectMemberRole.DEVELOPER))
                .when()
                .post("/projects/XIRA/members")
                .then()
                .statusCode(201);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", developerToken))
                .when()
                .delete("/projects/XIRA/members/{userId}", targetUserId)
                .then()
                .statusCode(403);
    }

    @Test
    void givenUnknownUser_whenAddProjectMember_thenReturnsNotFound() {
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
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(new AddProjectMemberRequest().userId(UUID.fromString("550e8400-e29b-41d4-a716-446655440000")).role(ProjectMemberRole.DEVELOPER))
                .when()
                .post("/projects/XIRA/members")
                .then()
                .statusCode(404);
    }

    @Test
    void givenAdmin_whenRemoveNonExistingProjectMember_thenReturnsNotFound() {
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
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .when()
                .delete("/projects/XIRA/members/{userId}", "550e8400-e29b-41d4-a716-446655440000")
                .then()
                .statusCode(404);
    }
}
