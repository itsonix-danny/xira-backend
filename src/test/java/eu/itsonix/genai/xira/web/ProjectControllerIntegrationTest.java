package eu.itsonix.genai.xira.web;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import eu.itsonix.genai.xira.web.model.*;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

class ProjectControllerIntegrationTest extends BaseIntegrationTest {

    private static final String EMAIL = "owner@example.com";
    private static final String PASSWORD = "password";

    @Test
    void givenAuthenticatedUser_whenCreateProject_thenReturnsCreatedProject() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);

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
    }

    @Test
    void givenDuplicateProjectKey_whenCreateProject_thenReturnsConflict() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);

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
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);

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
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);

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
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new CreateProjectRequest().key("VERYLONGKEY").name("Xira Project"))
                .when()
                .post("/projects")
                .then()
                .statusCode(400);
    }

    @Test
    void givenAdmin_whenUpdateProject_thenReturnsSuccess() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);

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
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String ownerToken = login(EMAIL, PASSWORD);

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
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);

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
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);

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
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);

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
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);

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
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);

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
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);

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
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);

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
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);

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
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String ownerToken = login(EMAIL, PASSWORD);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(new CreateProjectRequest().key("XIRA").name("Xira Project"))
                .when()
                .post("/projects")
                .then()
                .statusCode(201);

        final String adminEmail = "admin.member@example.com";
        registerUser(adminEmail, PASSWORD, "Admin", "Member");
        final String adminToken = login(adminEmail, PASSWORD);

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
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String ownerToken = login(EMAIL, PASSWORD);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(new CreateProjectRequest().key("XIRA").name("Xira Project"))
                .when()
                .post("/projects")
                .then()
                .statusCode(201);

        final String memberEmail = "member@example.com";
        registerUser(memberEmail, PASSWORD, "Member", "User");

        final String memberUserId = getUserIdByEmail(ownerToken, memberEmail);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(new AddProjectMemberRequest().userId(UUID.fromString(memberUserId))
                        .role(ProjectMemberRole.DEVELOPER))
                .when()
                .post("/projects/XIRA/members")
                .then()
                .statusCode(201);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(new AddProjectMemberRequest().userId(UUID.fromString(memberUserId))
                        .role(ProjectMemberRole.DEVELOPER))
                .when()
                .post("/projects/XIRA/members")
                .then()
                .statusCode(409);
    }

    @Test
    void givenAdmin_whenRemoveProjectMember_thenMemberCannotUpdateProject() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String ownerToken = login(EMAIL, PASSWORD);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(new CreateProjectRequest().key("XIRA").name("Xira Project"))
                .when()
                .post("/projects")
                .then()
                .statusCode(201);

        final String adminEmail = "admin.remove@example.com";
        registerUser(adminEmail, PASSWORD, "Remove", "Admin");
        final String adminToken = login(adminEmail, PASSWORD);

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
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String ownerToken = login(EMAIL, PASSWORD);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(new CreateProjectRequest().key("XIRA").name("Xira Project"))
                .when()
                .post("/projects")
                .then()
                .statusCode(201);

        final String developerEmail = "developer.role@example.com";
        registerUser(developerEmail, PASSWORD, "Role", "Developer");
        final String developerToken = login(developerEmail, PASSWORD);

        final String developerUserId = getUserIdByEmail(ownerToken, developerEmail);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(new AddProjectMemberRequest().userId(UUID.fromString(developerUserId))
                        .role(ProjectMemberRole.DEVELOPER))
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
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String ownerToken = login(EMAIL, PASSWORD);

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
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String ownerToken = login(EMAIL, PASSWORD);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(new CreateProjectRequest().key("XIRA").name("Xira Project"))
                .when()
                .post("/projects")
                .then()
                .statusCode(201);

        final String developerEmail = "developer.update.role@example.com";
        registerUser(developerEmail, PASSWORD, "Update", "Developer");
        final String developerUserId = getUserIdByEmail(ownerToken, developerEmail);
        final String developerToken = login(developerEmail, PASSWORD);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(new AddProjectMemberRequest().userId(UUID.fromString(developerUserId))
                        .role(ProjectMemberRole.DEVELOPER))
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
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String ownerToken = login(EMAIL, PASSWORD);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(new CreateProjectRequest().key("XIRA").name("Xira Project"))
                .when()
                .post("/projects")
                .then()
                .statusCode(201);

        final String developerEmail = "developer@example.com";
        registerUser(developerEmail, PASSWORD, "Dev", "User");

        final String developerUserId = getUserIdByEmail(ownerToken, developerEmail);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(new AddProjectMemberRequest().userId(UUID.fromString(developerUserId))
                        .role(ProjectMemberRole.DEVELOPER))
                .when()
                .post("/projects/XIRA/members")
                .then()
                .statusCode(201);

        final String developerToken = login(developerEmail, PASSWORD);

        final String newMemberEmail = "new.member@example.com";
        registerUser(newMemberEmail, PASSWORD, "New", "Member");

        final String newMemberUserId = getUserIdByEmail(ownerToken, newMemberEmail);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", developerToken))
                .body(new AddProjectMemberRequest().userId(UUID.fromString(newMemberUserId))
                        .role(ProjectMemberRole.DEVELOPER))
                .when()
                .post("/projects/XIRA/members")
                .then()
                .statusCode(403);
    }

    @Test
    void givenNonAdmin_whenRemoveProjectMember_thenReturnsForbidden() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String ownerToken = login(EMAIL, PASSWORD);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(new CreateProjectRequest().key("XIRA").name("Xira Project"))
                .when()
                .post("/projects")
                .then()
                .statusCode(201);

        final String developerEmail = "developer.remove@example.com";
        registerUser(developerEmail, PASSWORD, "Dev", "Remove");

        final String developerUserId = getUserIdByEmail(ownerToken, developerEmail);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(new AddProjectMemberRequest().userId(UUID.fromString(developerUserId))
                        .role(ProjectMemberRole.DEVELOPER))
                .when()
                .post("/projects/XIRA/members")
                .then()
                .statusCode(201);

        final String developerToken = login(developerEmail, PASSWORD);

        final String targetEmail = "target@example.com";
        registerUser(targetEmail, PASSWORD, "Target", "User");

        final String targetUserId = getUserIdByEmail(ownerToken, targetEmail);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(new AddProjectMemberRequest().userId(UUID.fromString(targetUserId))
                        .role(ProjectMemberRole.DEVELOPER))
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
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String ownerToken = login(EMAIL, PASSWORD);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(new CreateProjectRequest().key("XIRA").name("Xira Project"))
                .when()
                .post("/projects")
                .then()
                .statusCode(201);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(new AddProjectMemberRequest().userId(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
                        .role(ProjectMemberRole.DEVELOPER))
                .when()
                .post("/projects/XIRA/members")
                .then()
                .statusCode(404);
    }

    @Test
    void givenAdmin_whenRemoveNonExistingProjectMember_thenReturnsNotFound() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String ownerToken = login(EMAIL, PASSWORD);

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

    @Test
    void givenProjectMember_whenGetProjectMembers_thenReturnsAllMembers() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String ownerToken = login(EMAIL, PASSWORD);

        createProject(ownerToken);

        final String member1Email = "member1@example.com";
        registerUser(member1Email, PASSWORD, "Member", "One");
        final String member1UserId = getUserIdByEmail(ownerToken, member1Email);

        final String member2Email = "member2@example.com";
        registerUser(member2Email, PASSWORD, "Member", "Two");
        final String member2UserId = getUserIdByEmail(ownerToken, member2Email);

        addProjectMember(ownerToken, member1UserId, ProjectMemberRole.DEVELOPER);
        addProjectMember(ownerToken, member2UserId, ProjectMemberRole.ADMIN);

        final var members = given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .when()
                .get("/projects/XIRA/members")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList(".", ProjectMemberResponse.class);

        assertThat(members).hasSize(3);
        assertThat(members).extracting("email").containsExactlyInAnyOrder(EMAIL, member1Email, member2Email);
        assertThat(members).extracting("firstName").containsExactlyInAnyOrder("Owner", "Member", "Member");
        assertThat(members).extracting("lastName").containsExactlyInAnyOrder("Example", "One", "Two");

        final var owner = members.stream().filter(m -> m.getEmail().equals(EMAIL)).findFirst().orElseThrow();
        assertThat(owner.getRole()).isEqualTo(ProjectMemberRole.ADMIN);
        assertThat(owner.getIsOwner()).isTrue();

        final var member1 = members.stream().filter(m -> m.getEmail().equals(member1Email)).findFirst().orElseThrow();
        assertThat(member1.getRole()).isEqualTo(ProjectMemberRole.DEVELOPER);
        assertThat(member1.getIsOwner()).isFalse();

        final var member2 = members.stream().filter(m -> m.getEmail().equals(member2Email)).findFirst().orElseThrow();
        assertThat(member2.getRole()).isEqualTo(ProjectMemberRole.ADMIN);
        assertThat(member2.getIsOwner()).isFalse();
    }

    @Test
    void givenNonMember_whenGetProjectMembers_thenReturnsForbidden() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String ownerToken = login(EMAIL, PASSWORD);

        createProject(ownerToken);

        final String nonMemberEmail = "nonmember@example.com";
        registerUser(nonMemberEmail, PASSWORD, "Non", "Member");
        final String nonMemberToken = login(nonMemberEmail, PASSWORD);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", nonMemberToken))
                .when()
                .get("/projects/XIRA/members")
                .then()
                .statusCode(403);
    }

    @Test
    void givenUnauthenticated_whenGetProjectMembers_thenReturnsUnauthorized() {
        given().contentType(ContentType.JSON).when().get("/projects/XIRA/members").then().statusCode(401);
    }

    @Test
    void givenNonExistentProject_whenGetProjectMembers_thenReturnsForbidden() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String ownerToken = login(EMAIL, PASSWORD);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .when()
                .get("/projects/NONEXISTENT/members")
                .then()
                .statusCode(403);
    }

    @Test
    void givenAuthenticatedUser_whenGetProjects_thenReturnsUserProjects() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String ownerToken = login(EMAIL, PASSWORD);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(new CreateProjectRequest().key("PROJA").name("Project A"))
                .when()
                .post("/projects")
                .then()
                .statusCode(201);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(new CreateProjectRequest().key("PROJB").name("Project B"))
                .when()
                .post("/projects")
                .then()
                .statusCode(201);

        final var projects = given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .when()
                .get("/projects")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList(".", ProjectDetailsResponse.class);

        assertThat(projects).hasSize(2);
        assertThat(projects).extracting("key").containsExactlyInAnyOrder("PROJA", "PROJB");
        assertThat(projects).extracting("isOwner").containsOnly(true);
        assertThat(projects).extracting("isAdmin").containsOnly(true);
        assertThat(projects).allSatisfy(project -> assertThat(project.getBoards()).isNotNull());
    }

    @Test
    void givenMemberOfMultipleProjects_whenGetProjects_thenReturnsOnlyUserProjects() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String ownerToken = login(EMAIL, PASSWORD);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(new CreateProjectRequest().key("OWNED").name("Owned Project"))
                .when()
                .post("/projects")
                .then()
                .statusCode(201);

        final String memberEmail = "member@example.com";
        registerUser(memberEmail, PASSWORD, "Member", "User");
        final String memberToken = login(memberEmail, PASSWORD);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", memberToken))
                .body(new CreateProjectRequest().key("OTHER").name("Other Project"))
                .when()
                .post("/projects")
                .then()
                .statusCode(201);

        final String memberUserId = getUserIdByEmail(ownerToken, memberEmail);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(new AddProjectMemberRequest().userId(UUID.fromString(memberUserId))
                        .role(ProjectMemberRole.DEVELOPER))
                .when()
                .post("/projects/OWNED/members")
                .then()
                .statusCode(201);

        final var memberProjects = given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", memberToken))
                .when()
                .get("/projects")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList(".", ProjectDetailsResponse.class);

        assertThat(memberProjects).hasSize(2);
        assertThat(memberProjects).extracting("key").containsExactlyInAnyOrder("OWNED", "OTHER");

        final var ownedProject = memberProjects.stream()
                .filter(p -> p.getKey().equals("OWNED"))
                .findFirst()
                .orElseThrow();
        assertThat(ownedProject.getIsOwner()).isFalse();
        assertThat(ownedProject.getIsAdmin()).isFalse();
        assertThat(ownedProject.getBoards()).isNotNull();

        final var otherProject = memberProjects.stream()
                .filter(p -> p.getKey().equals("OTHER"))
                .findFirst()
                .orElseThrow();
        assertThat(otherProject.getIsOwner()).isTrue();
        assertThat(otherProject.getIsAdmin()).isTrue();
        assertThat(otherProject.getBoards()).isNotNull();
    }

    @Test
    void givenUnauthenticated_whenGetProjects_thenReturnsUnauthorized() {
        given().contentType(ContentType.JSON).when().get("/projects").then().statusCode(401);
    }

    @Test
    void givenProjectMember_whenGetProjectByKey_thenReturnsProjectDetails() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String ownerToken = login(EMAIL, PASSWORD);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(new CreateProjectRequest().key("XIRA").name("Xira Project").description("Test description"))
                .when()
                .post("/projects")
                .then()
                .statusCode(201);

        final var projectDetails = given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .when()
                .get("/projects/XIRA")
                .then()
                .statusCode(200)
                .extract()
                .as(ProjectDetailsResponse.class);

        assertThat(projectDetails.getKey()).isEqualTo("XIRA");
        assertThat(projectDetails.getName()).isEqualTo("Xira Project");
        assertThat(projectDetails.getDescription()).isEqualTo("Test description");
        assertThat(projectDetails.getIsOwner()).isTrue();
        assertThat(projectDetails.getIsAdmin()).isTrue();
        assertThat(projectDetails.getBoards()).isEmpty();
    }

    @Test
    void givenNonMember_whenGetProjectByKey_thenReturnsNotFound() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String ownerToken = login(EMAIL, PASSWORD);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(new CreateProjectRequest().key("XIRA").name("Xira Project"))
                .when()
                .post("/projects")
                .then()
                .statusCode(201);

        final String nonMemberEmail = "nonmember@example.com";
        registerUser(nonMemberEmail, PASSWORD, "Non", "Member");
        final String nonMemberToken = login(nonMemberEmail, PASSWORD);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", nonMemberToken))
                .when()
                .get("/projects/XIRA")
                .then()
                .statusCode(404);
    }

    @Test
    void givenNonExistentProject_whenGetProjectByKey_thenReturnsNotFound() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String ownerToken = login(EMAIL, PASSWORD);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .when()
                .get("/projects/NONEXISTENT")
                .then()
                .statusCode(404);
    }

    @Test
    void givenUnauthenticated_whenGetProjectByKey_thenReturnsUnauthorized() {
        given().contentType(ContentType.JSON).when().get("/projects/XIRA").then().statusCode(401);
    }

    @Test
    void givenUnauthenticated_whenAddProjectMember_thenReturnsUnauthorized() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new CreateProjectRequest().key("XIRA").name("Xira Project"))
                .when()
                .post("/projects")
                .then()
                .statusCode(201);

        given().contentType(ContentType.JSON)
                .body(new AddProjectMemberRequest().userId(UUID.randomUUID()).role(ProjectMemberRole.DEVELOPER))
                .when()
                .post("/projects/XIRA/members")
                .then()
                .statusCode(401);
    }

    @Test
    void givenNonExistentProject_whenAddProjectMember_thenReturnsForbidden() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new AddProjectMemberRequest().userId(UUID.randomUUID()).role(ProjectMemberRole.DEVELOPER))
                .when()
                .post("/projects/NONEXISTENT/members")
                .then()
                .statusCode(403);
    }

    @Test
    void givenInvalidUserId_whenAddProjectMember_thenReturnsBadRequest() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new CreateProjectRequest().key("XIRA").name("Xira Project"))
                .when()
                .post("/projects")
                .then()
                .statusCode(201);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body("{\"userId\":\"invalid-uuid\",\"role\":\"DEVELOPER\"}")
                .when()
                .post("/projects/XIRA/members")
                .then()
                .statusCode(400);
    }

    @Test
    void givenMissingFields_whenAddProjectMember_thenReturnsBadRequest() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new CreateProjectRequest().key("XIRA").name("Xira Project"))
                .when()
                .post("/projects")
                .then()
                .statusCode(201);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body("{}")
                .when()
                .post("/projects/XIRA/members")
                .then()
                .statusCode(400);
    }

    @Test
    void givenUnauthenticated_whenUpdateProjectMemberRole_thenReturnsUnauthorized() {
        given().contentType(ContentType.JSON)
                .body(new UpdateProjectMemberRoleRequest().role(ProjectMemberRole.ADMIN))
                .when()
                .patch("/projects/XIRA/members/{userId}", UUID.randomUUID())
                .then()
                .statusCode(401);
    }

    @Test
    void givenNonExistentProject_whenUpdateProjectMemberRole_thenReturnsForbidden() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new UpdateProjectMemberRoleRequest().role(ProjectMemberRole.ADMIN))
                .when()
                .patch("/projects/NONEXISTENT/members/{userId}", UUID.randomUUID())
                .then()
                .statusCode(403);
    }

    @Test
    void givenNonExistentMember_whenUpdateProjectMemberRole_thenReturnsNotFound() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new CreateProjectRequest().key("XIRA").name("Xira Project"))
                .when()
                .post("/projects")
                .then()
                .statusCode(201);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new UpdateProjectMemberRoleRequest().role(ProjectMemberRole.ADMIN))
                .when()
                .patch("/projects/XIRA/members/{userId}", UUID.randomUUID())
                .then()
                .statusCode(404);
    }

    @Test
    void givenInvalidUserId_whenUpdateProjectMemberRole_thenReturnsBadRequest() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new CreateProjectRequest().key("XIRA").name("Xira Project"))
                .when()
                .post("/projects")
                .then()
                .statusCode(201);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new UpdateProjectMemberRoleRequest().role(ProjectMemberRole.ADMIN))
                .when()
                .patch("/projects/XIRA/members/invalid-uuid")
                .then()
                .statusCode(400);
    }

    @Test
    void givenMissingRole_whenUpdateProjectMemberRole_thenReturnsBadRequest() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new CreateProjectRequest().key("XIRA").name("Xira Project"))
                .when()
                .post("/projects")
                .then()
                .statusCode(201);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body("{}")
                .when()
                .patch("/projects/XIRA/members/{userId}", UUID.randomUUID())
                .then()
                .statusCode(400);
    }

    @Test
    void givenUnauthenticated_whenRemoveProjectMember_thenReturnsUnauthorized() {
        given().contentType(ContentType.JSON)
                .when()
                .delete("/projects/XIRA/members/{userId}", UUID.randomUUID())
                .then()
                .statusCode(401);
    }

    @Test
    void givenNonExistentProject_whenRemoveProjectMember_thenReturnsForbidden() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .when()
                .delete("/projects/NONEXISTENT/members/{userId}", UUID.randomUUID())
                .then()
                .statusCode(403);
    }

    @Test
    void givenOwnerRemovingSelf_whenRemoveProjectMember_thenReturnsConflict() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new CreateProjectRequest().key("XIRA").name("Xira Project"))
                .when()
                .post("/projects")
                .then()
                .statusCode(201);

        final String ownerUserId = getUserIdByEmail(token, EMAIL);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .when()
                .delete("/projects/XIRA/members/{userId}", ownerUserId)
                .then()
                .statusCode(409);
    }

    @Test
    void givenMissingRequiredFields_whenCreateProject_thenReturnsBadRequest() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body("{}")
                .when()
                .post("/projects")
                .then()
                .statusCode(400);
    }

    @Test
    void givenEmptyName_whenCreateProject_thenReturnsBadRequest() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new CreateProjectRequest().key("XIRA").name(""))
                .when()
                .post("/projects")
                .then()
                .statusCode(400);
    }

    @Test
    void givenSpecialCharsInKey_whenCreateProject_thenReturnsBadRequest() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new CreateProjectRequest().key("AB-CD").name("Project"))
                .when()
                .post("/projects")
                .then()
                .statusCode(400);
    }

    @Test
    void givenNoProjects_whenGetProjects_thenReturnsEmptyArray() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);

        final var projects = given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .when()
                .get("/projects")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList(".", ProjectDetailsResponse.class);

        assertThat(projects).isEmpty();
    }

    @Test
    void givenProjectWithBoards_whenGetProjectByKey_thenReturnsBoardsList() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);

        createProject(token);
        createBoard(token);

        final var projectDetails = given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .when()
                .get("/projects/XIRA")
                .then()
                .statusCode(200)
                .extract()
                .as(ProjectDetailsResponse.class);

        assertThat(projectDetails.getBoards()).hasSize(1);
        assertThat(projectDetails.getBoards().getFirst().getName()).isEqualTo("Development Board");
        assertThat(projectDetails.getBoards().getFirst().getType()).isEqualTo(ProjectBoardResponse.TypeEnum.KANBAN);
    }

    @Test
    void givenDeveloperMember_whenGetProjectByKey_thenReturnsCorrectFlags() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String ownerToken = login(EMAIL, PASSWORD);

        createProject(ownerToken);

        final String developerEmail = "developer@example.com";
        registerUser(developerEmail, PASSWORD, "Developer", "User");
        final String developerUserId = getUserIdByEmail(ownerToken, developerEmail);

        addProjectMember(ownerToken, developerUserId, ProjectMemberRole.DEVELOPER);

        final String developerToken = login(developerEmail, PASSWORD);

        final var projectDetails = given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", developerToken))
                .when()
                .get("/projects/XIRA")
                .then()
                .statusCode(200)
                .extract()
                .as(ProjectDetailsResponse.class);

        assertThat(projectDetails.getIsOwner()).isFalse();
        assertThat(projectDetails.getIsAdmin()).isFalse();
    }

    @Test
    void givenAdmin_whenRemovingOwner_thenReturnsConflict() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String ownerToken = login(EMAIL, PASSWORD);
        createProject(ownerToken);

        final String adminEmail = "admin@example.com";
        registerUser(adminEmail, PASSWORD, "Admin", "User");
        final String adminUserId = getUserIdByEmail(ownerToken, adminEmail);
        addProjectMember(ownerToken, adminUserId, ProjectMemberRole.ADMIN);

        final String adminToken = login(adminEmail, PASSWORD);
        final String ownerUserId = getUserIdByEmail(ownerToken, EMAIL);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", adminToken))
                .when()
                .delete("/projects/XIRA/members/{userId}", ownerUserId)
                .then()
                .statusCode(409);
    }
}
