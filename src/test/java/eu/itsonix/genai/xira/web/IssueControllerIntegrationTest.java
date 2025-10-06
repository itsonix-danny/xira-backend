package eu.itsonix.genai.xira.web;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import eu.itsonix.genai.xira.web.model.*;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.matchesPattern;

class IssueControllerIntegrationTest extends BaseIntegrationTest {

    @Test
    void givenValidRequest_whenCreateIssue_thenReturns201WithLocation() {
        registerUser("user@example.com", "password", "John", "Doe");
        final String token = login("user@example.com", "password");
        createProject(token);

        final CreateIssueRequest request = new CreateIssueRequest().title("Test Issue")
                .description("Test Description")
                .issueType(CreateIssueRequest.IssueTypeEnum.BUG)
                .priority(CreateIssueRequest.PriorityEnum.HIGH);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(request)
                .when()
                .post("/projects/{key}/issues", "XIRA")
                .then()
                .statusCode(201)
                .header(HttpHeaders.LOCATION, matchesPattern(".*/projects/XIRA/issues/XIRA-1"));
    }

    @Test
    void givenMultipleIssues_whenCreateIssue_thenIncrementsSeqNo() {
        registerUser("user@example.com", "password", "John", "Doe");
        final String token = login("user@example.com", "password");
        createProject(token);

        final CreateIssueRequest request1 = new CreateIssueRequest().title("First Issue")
                .issueType(CreateIssueRequest.IssueTypeEnum.TASK)
                .priority(CreateIssueRequest.PriorityEnum.MEDIUM);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(request1)
                .when()
                .post("/projects/{key}/issues", "XIRA")
                .then()
                .statusCode(201);

        final CreateIssueRequest request2 = new CreateIssueRequest().title("Second Issue")
                .issueType(CreateIssueRequest.IssueTypeEnum.STORY)
                .priority(CreateIssueRequest.PriorityEnum.LOW);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(request2)
                .when()
                .post("/projects/{key}/issues", "XIRA")
                .then()
                .statusCode(201)
                .header(HttpHeaders.LOCATION, matchesPattern(".*/projects/XIRA/issues/XIRA-2"));
    }

    @Test
    void givenNoDescription_whenCreateIssue_thenReturns201() {
        registerUser("user@example.com", "password", "John", "Doe");
        final String token = login("user@example.com", "password");
        createProject(token);

        final CreateIssueRequest request = new CreateIssueRequest().title("Test Issue")
                .issueType(CreateIssueRequest.IssueTypeEnum.BUG)
                .priority(CreateIssueRequest.PriorityEnum.CRITICAL);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(request)
                .when()
                .post("/projects/{key}/issues", "XIRA")
                .then()
                .statusCode(201);
    }

    @Test
    void givenNonMember_whenCreateIssue_thenReturns403() {
        registerUser("owner@example.com", "password", "Owner", "User");
        final String ownerToken = login("owner@example.com", "password");
        createProject(ownerToken);

        registerUser("nonmember@example.com", "password", "Non", "Member");
        final String nonMemberToken = login("nonmember@example.com", "password");

        final CreateIssueRequest request = new CreateIssueRequest().title("Test Issue")
                .issueType(CreateIssueRequest.IssueTypeEnum.BUG)
                .priority(CreateIssueRequest.PriorityEnum.HIGH);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", nonMemberToken))
                .body(request)
                .when()
                .post("/projects/{key}/issues", "XIRA")
                .then()
                .statusCode(403);
    }

    @Test
    void givenDeveloper_whenCreateIssue_thenReturns201() {
        registerUser("admin@example.com", "password", "Admin", "User");
        final String adminToken = login("admin@example.com", "password");
        createProject(adminToken);

        registerUser("developer@example.com", "password", "Dev", "User");
        final String developerToken = login("developer@example.com", "password");
        final String developerId = getUserIdByEmail(adminToken, "developer@example.com");
        addProjectMember(adminToken, developerId, ProjectMemberRole.DEVELOPER);

        final CreateIssueRequest request = new CreateIssueRequest().title("Test Issue")
                .issueType(CreateIssueRequest.IssueTypeEnum.TASK)
                .priority(CreateIssueRequest.PriorityEnum.MEDIUM);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", developerToken))
                .body(request)
                .when()
                .post("/projects/{key}/issues", "XIRA")
                .then()
                .statusCode(201);
    }

    @Test
    void givenUnauthenticated_whenCreateIssue_thenReturns401() {
        final CreateIssueRequest request = new CreateIssueRequest().title("Test Issue")
                .issueType(CreateIssueRequest.IssueTypeEnum.BUG)
                .priority(CreateIssueRequest.PriorityEnum.HIGH);

        given().contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/projects/{key}/issues", "XIRA")
                .then()
                .statusCode(401);
    }

    @Test
    void givenValidRequest_whenUpdateIssue_thenReturns200() {
        registerUser("user@example.com", "password", "John", "Doe");
        final String token = login("user@example.com", "password");
        createProject(token);

        final CreateIssueRequest createRequest = new CreateIssueRequest().title("Original Title")
                .issueType(CreateIssueRequest.IssueTypeEnum.BUG)
                .priority(CreateIssueRequest.PriorityEnum.HIGH);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(createRequest)
                .post("/projects/{key}/issues", "XIRA");

        final UpdateIssueRequest updateRequest = new UpdateIssueRequest().title("Updated Title")
                .description("Updated Description");

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(updateRequest)
                .when()
                .patch("/projects/{key}/issues/{issueKey}", "XIRA", "XIRA-1")
                .then()
                .statusCode(200);
    }

    @Test
    void givenNonMember_whenUpdateIssue_thenReturns403() {
        registerUser("owner@example.com", "password", "Owner", "User");
        final String ownerToken = login("owner@example.com", "password");
        createProject(ownerToken);

        final CreateIssueRequest createRequest = new CreateIssueRequest().title("Original Title")
                .issueType(CreateIssueRequest.IssueTypeEnum.BUG)
                .priority(CreateIssueRequest.PriorityEnum.HIGH);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(createRequest)
                .post("/projects/{key}/issues", "XIRA");

        registerUser("nonmember@example.com", "password", "Non", "Member");
        final String nonMemberToken = login("nonmember@example.com", "password");

        final UpdateIssueRequest updateRequest = new UpdateIssueRequest().title("Updated Title");

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", nonMemberToken))
                .body(updateRequest)
                .when()
                .patch("/projects/{key}/issues/{issueKey}", "XIRA", "XIRA-1")
                .then()
                .statusCode(403);
    }

    @Test
    void givenValidRequest_whenAddAssignee_thenReturns204() {
        registerUser("owner@example.com", "password", "Owner", "User");
        final String ownerToken = login("owner@example.com", "password");
        createProject(ownerToken);

        registerUser("developer@example.com", "password", "Dev", "User");
        final String developerId = getUserIdByEmail(ownerToken, "developer@example.com");
        addProjectMember(ownerToken, developerId, ProjectMemberRole.DEVELOPER);

        final CreateIssueRequest createRequest = new CreateIssueRequest().title("Test Issue")
                .issueType(CreateIssueRequest.IssueTypeEnum.BUG)
                .priority(CreateIssueRequest.PriorityEnum.HIGH);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(createRequest)
                .post("/projects/{key}/issues", "XIRA");

        final AddAssigneeRequest request = new AddAssigneeRequest().userId(UUID.fromString(developerId));

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(request)
                .when()
                .post("/projects/{key}/issues/{issueKey}/assignees", "XIRA", "XIRA-1")
                .then()
                .statusCode(204);
    }

    @Test
    void givenUserNotProjectMember_whenAddAssignee_thenReturns409() {
        registerUser("owner@example.com", "password", "Owner", "User");
        final String ownerToken = login("owner@example.com", "password");
        createProject(ownerToken);

        registerUser("nonmember@example.com", "password", "Non", "Member");
        final String nonMemberId = getUserIdByEmail(ownerToken, "nonmember@example.com");

        final CreateIssueRequest createRequest = new CreateIssueRequest().title("Test Issue")
                .issueType(CreateIssueRequest.IssueTypeEnum.BUG)
                .priority(CreateIssueRequest.PriorityEnum.HIGH);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(createRequest)
                .post("/projects/{key}/issues", "XIRA");

        final AddAssigneeRequest request = new AddAssigneeRequest().userId(UUID.fromString(nonMemberId));

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(request)
                .when()
                .post("/projects/{key}/issues/{issueKey}/assignees", "XIRA", "XIRA-1")
                .then()
                .statusCode(409);
    }

    @Test
    void givenNonMember_whenAddAssignee_thenReturns403() {
        registerUser("owner@example.com", "password", "Owner", "User");
        final String ownerToken = login("owner@example.com", "password");
        final String ownerId = getUserIdByEmail(ownerToken, "owner@example.com");
        createProject(ownerToken);

        final CreateIssueRequest createRequest = new CreateIssueRequest().title("Test Issue")
                .issueType(CreateIssueRequest.IssueTypeEnum.BUG)
                .priority(CreateIssueRequest.PriorityEnum.HIGH);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(createRequest)
                .post("/projects/{key}/issues", "XIRA");

        registerUser("nonmember@example.com", "password", "Non", "Member");
        final String nonMemberToken = login("nonmember@example.com", "password");

        final AddAssigneeRequest request = new AddAssigneeRequest().userId(UUID.fromString(ownerId));

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", nonMemberToken))
                .body(request)
                .when()
                .post("/projects/{key}/issues/{issueKey}/assignees", "XIRA", "XIRA-1")
                .then()
                .statusCode(403);
    }

    @Test
    void givenValidRequest_whenRemoveAssignee_thenReturns204() {
        registerUser("owner@example.com", "password", "Owner", "User");
        final String ownerToken = login("owner@example.com", "password");
        createProject(ownerToken);

        registerUser("developer@example.com", "password", "Dev", "User");
        final String developerId = getUserIdByEmail(ownerToken, "developer@example.com");
        addProjectMember(ownerToken, developerId, ProjectMemberRole.DEVELOPER);

        final CreateIssueRequest createRequest = new CreateIssueRequest().title("Test Issue")
                .issueType(CreateIssueRequest.IssueTypeEnum.BUG)
                .priority(CreateIssueRequest.PriorityEnum.HIGH);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(createRequest)
                .post("/projects/{key}/issues", "XIRA");

        final AddAssigneeRequest addRequest = new AddAssigneeRequest().userId(UUID.fromString(developerId));

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(addRequest)
                .post("/projects/{key}/issues/{issueKey}/assignees", "XIRA", "XIRA-1");

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .when()
                .delete("/projects/{key}/issues/{issueKey}/assignees/{userId}", "XIRA", "XIRA-1", developerId)
                .then()
                .statusCode(204);
    }

    @Test
    void givenNonMember_whenRemoveAssignee_thenReturns403() {
        registerUser("owner@example.com", "password", "Owner", "User");
        final String ownerToken = login("owner@example.com", "password");
        createProject(ownerToken);

        registerUser("developer@example.com", "password", "Dev", "User");
        final String developerId = getUserIdByEmail(ownerToken, "developer@example.com");
        addProjectMember(ownerToken, developerId, ProjectMemberRole.DEVELOPER);

        final CreateIssueRequest createRequest = new CreateIssueRequest().title("Test Issue")
                .issueType(CreateIssueRequest.IssueTypeEnum.BUG)
                .priority(CreateIssueRequest.PriorityEnum.HIGH);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(createRequest)
                .post("/projects/{key}/issues", "XIRA");

        final AddAssigneeRequest addRequest = new AddAssigneeRequest().userId(UUID.fromString(developerId));

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(addRequest)
                .post("/projects/{key}/issues/{issueKey}/assignees", "XIRA", "XIRA-1");

        registerUser("nonmember@example.com", "password", "Non", "Member");
        final String nonMemberToken = login("nonmember@example.com", "password");

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", nonMemberToken))
                .when()
                .delete("/projects/{key}/issues/{issueKey}/assignees/{userId}", "XIRA", "XIRA-1", developerId)
                .then()
                .statusCode(403);
    }

    @Test
    void givenValidRequest_whenSetIssueStatus_thenReturns200() {
        registerUser("user@example.com", "password", "John", "Doe");
        final String token = login("user@example.com", "password");
        createProject(token);

        final CreateIssueRequest createRequest = new CreateIssueRequest().title("Test Issue")
                .issueType(CreateIssueRequest.IssueTypeEnum.BUG)
                .priority(CreateIssueRequest.PriorityEnum.HIGH);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(createRequest)
                .post("/projects/{key}/issues", "XIRA");

        final String statusId = getWorkflowStatusId(token);
        final SetIssueStatusRequest request = new SetIssueStatusRequest().statusId(UUID.fromString(statusId));

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(request)
                .when()
                .patch("/projects/{key}/issues/{issueKey}/status", "XIRA", "XIRA-1")
                .then()
                .statusCode(200);
    }

    @Test
    void givenNonMember_whenSetIssueStatus_thenReturns403() {
        registerUser("owner@example.com", "password", "Owner", "User");
        final String ownerToken = login("owner@example.com", "password");
        createProject(ownerToken);

        final CreateIssueRequest createRequest = new CreateIssueRequest().title("Test Issue")
                .issueType(CreateIssueRequest.IssueTypeEnum.BUG)
                .priority(CreateIssueRequest.PriorityEnum.HIGH);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(createRequest)
                .post("/projects/{key}/issues", "XIRA");

        registerUser("nonmember@example.com", "password", "Non", "Member");
        final String nonMemberToken = login("nonmember@example.com", "password");

        final String statusId = getWorkflowStatusId(ownerToken);
        final SetIssueStatusRequest request = new SetIssueStatusRequest().statusId(UUID.fromString(statusId));

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", nonMemberToken))
                .body(request)
                .when()
                .patch("/projects/{key}/issues/{issueKey}/status", "XIRA", "XIRA-1")
                .then()
                .statusCode(403);
    }

    @Test
    void givenValidRequest_whenAddComment_thenReturns201() {
        registerUser("user@example.com", "password", "John", "Doe");
        final String token = login("user@example.com", "password");
        createProject(token);

        final CreateIssueRequest createRequest = new CreateIssueRequest().title("Test Issue")
                .issueType(CreateIssueRequest.IssueTypeEnum.BUG)
                .priority(CreateIssueRequest.PriorityEnum.HIGH);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(createRequest)
                .post("/projects/{key}/issues", "XIRA");

        final AddCommentRequest request = new AddCommentRequest().content("Test comment");

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(request)
                .when()
                .post("/projects/{key}/issues/{issueKey}/comments", "XIRA", "XIRA-1")
                .then()
                .statusCode(201);
    }

    @Test
    void givenNonMember_whenAddComment_thenReturns403() {
        registerUser("owner@example.com", "password", "Owner", "User");
        final String ownerToken = login("owner@example.com", "password");
        createProject(ownerToken);

        final CreateIssueRequest createRequest = new CreateIssueRequest().title("Test Issue")
                .issueType(CreateIssueRequest.IssueTypeEnum.BUG)
                .priority(CreateIssueRequest.PriorityEnum.HIGH);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(createRequest)
                .post("/projects/{key}/issues", "XIRA");

        registerUser("nonmember@example.com", "password", "Non", "Member");
        final String nonMemberToken = login("nonmember@example.com", "password");

        final AddCommentRequest request = new AddCommentRequest().content("Test comment");

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", nonMemberToken))
                .body(request)
                .when()
                .post("/projects/{key}/issues/{issueKey}/comments", "XIRA", "XIRA-1")
                .then()
                .statusCode(403);
    }

    @Test
    void givenValidRequest_whenUpdateComment_thenReturns200() {
        registerUser("user@example.com", "password", "John", "Doe");
        final String token = login("user@example.com", "password");
        createProject(token);

        final CreateIssueRequest createRequest = new CreateIssueRequest().title("Test Issue")
                .issueType(CreateIssueRequest.IssueTypeEnum.BUG)
                .priority(CreateIssueRequest.PriorityEnum.HIGH);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(createRequest)
                .post("/projects/{key}/issues", "XIRA");

        final AddCommentRequest addRequest = new AddCommentRequest().content("Original comment");

        final String commentId = given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(addRequest)
                .post("/projects/{key}/issues/{issueKey}/comments", "XIRA", "XIRA-1")
                .then()
                .extract()
                .header(HttpHeaders.LOCATION)
                .split("/")[6];

        final UpdateCommentRequest updateRequest = new UpdateCommentRequest().content("Updated comment");

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(updateRequest)
                .when()
                .patch("/projects/{key}/issues/{issueKey}/comments/{commentId}", "XIRA", "XIRA-1", commentId)
                .then()
                .statusCode(200);
    }

    @Test
    void givenNonAuthor_whenUpdateComment_thenReturns403() {
        registerUser("owner@example.com", "password", "Owner", "User");
        final String ownerToken = login("owner@example.com", "password");
        createProject(ownerToken);

        registerUser("developer@example.com", "password", "Dev", "User");
        final String developerToken = login("developer@example.com", "password");
        final String developerId = getUserIdByEmail(ownerToken, "developer@example.com");
        addProjectMember(ownerToken, developerId, ProjectMemberRole.DEVELOPER);

        final CreateIssueRequest createRequest = new CreateIssueRequest().title("Test Issue")
                .issueType(CreateIssueRequest.IssueTypeEnum.BUG)
                .priority(CreateIssueRequest.PriorityEnum.HIGH);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(createRequest)
                .post("/projects/{key}/issues", "XIRA");

        final AddCommentRequest addRequest = new AddCommentRequest().content("Original comment");

        final String commentId = given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(addRequest)
                .post("/projects/{key}/issues/{issueKey}/comments", "XIRA", "XIRA-1")
                .then()
                .extract()
                .header(HttpHeaders.LOCATION)
                .split("/")[6];

        final UpdateCommentRequest updateRequest = new UpdateCommentRequest().content("Updated comment");

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", developerToken))
                .body(updateRequest)
                .when()
                .patch("/projects/{key}/issues/{issueKey}/comments/{commentId}", "XIRA", "XIRA-1", commentId)
                .then()
                .statusCode(403);
    }

    @Test
    void givenAuthor_whenDeleteComment_thenReturns204() {
        registerUser("user@example.com", "password", "John", "Doe");
        final String token = login("user@example.com", "password");
        createProject(token);

        final CreateIssueRequest createRequest = new CreateIssueRequest().title("Test Issue")
                .issueType(CreateIssueRequest.IssueTypeEnum.BUG)
                .priority(CreateIssueRequest.PriorityEnum.HIGH);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(createRequest)
                .post("/projects/{key}/issues", "XIRA");

        final AddCommentRequest addRequest = new AddCommentRequest().content("Test comment");

        final String commentId = given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(addRequest)
                .post("/projects/{key}/issues/{issueKey}/comments", "XIRA", "XIRA-1")
                .then()
                .extract()
                .header(HttpHeaders.LOCATION)
                .split("/")[6];

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .when()
                .delete("/projects/{key}/issues/{issueKey}/comments/{commentId}", "XIRA", "XIRA-1", commentId)
                .then()
                .statusCode(204);
    }

    @Test
    void givenAdmin_whenDeleteComment_thenReturns204() {
        registerUser("admin@example.com", "password", "Admin", "User");
        final String adminToken = login("admin@example.com", "password");
        createProject(adminToken);

        registerUser("developer@example.com", "password", "Dev", "User");
        final String developerToken = login("developer@example.com", "password");
        final String developerId = getUserIdByEmail(adminToken, "developer@example.com");
        addProjectMember(adminToken, developerId, ProjectMemberRole.DEVELOPER);

        final CreateIssueRequest createRequest = new CreateIssueRequest().title("Test Issue")
                .issueType(CreateIssueRequest.IssueTypeEnum.BUG)
                .priority(CreateIssueRequest.PriorityEnum.HIGH);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", adminToken))
                .body(createRequest)
                .post("/projects/{key}/issues", "XIRA");

        final AddCommentRequest addRequest = new AddCommentRequest().content("Test comment");

        final String commentId = given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", developerToken))
                .body(addRequest)
                .post("/projects/{key}/issues/{issueKey}/comments", "XIRA", "XIRA-1")
                .then()
                .extract()
                .header(HttpHeaders.LOCATION)
                .split("/")[6];

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", adminToken))
                .when()
                .delete("/projects/{key}/issues/{issueKey}/comments/{commentId}", "XIRA", "XIRA-1", commentId)
                .then()
                .statusCode(204);
    }

    @Test
    void givenNonAuthorNonAdmin_whenDeleteComment_thenReturns403() {
        registerUser("admin@example.com", "password", "Admin", "User");
        final String adminToken = login("admin@example.com", "password");
        createProject(adminToken);

        registerUser("developer1@example.com", "password", "Dev1", "User");
        final String developer1Token = login("developer1@example.com", "password");
        final String developer1Id = getUserIdByEmail(adminToken, "developer1@example.com");
        addProjectMember(adminToken, developer1Id, ProjectMemberRole.DEVELOPER);

        registerUser("developer2@example.com", "password", "Dev2", "User");
        final String developer2Token = login("developer2@example.com", "password");
        final String developer2Id = getUserIdByEmail(adminToken, "developer2@example.com");
        addProjectMember(adminToken, developer2Id, ProjectMemberRole.DEVELOPER);

        final CreateIssueRequest createRequest = new CreateIssueRequest().title("Test Issue")
                .issueType(CreateIssueRequest.IssueTypeEnum.BUG)
                .priority(CreateIssueRequest.PriorityEnum.HIGH);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", adminToken))
                .body(createRequest)
                .post("/projects/{key}/issues", "XIRA");

        final AddCommentRequest addRequest = new AddCommentRequest().content("Test comment");

        final String commentId = given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", developer1Token))
                .body(addRequest)
                .post("/projects/{key}/issues/{issueKey}/comments", "XIRA", "XIRA-1")
                .then()
                .extract()
                .header(HttpHeaders.LOCATION)
                .split("/")[6];

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", developer2Token))
                .when()
                .delete("/projects/{key}/issues/{issueKey}/comments/{commentId}", "XIRA", "XIRA-1", commentId)
                .then()
                .statusCode(403);
    }

    private String getWorkflowStatusId(final String token) {
        final WorkflowStatusResponse[] statuses = given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .when()
                .get("/projects/{key}/workflows/statuses", "XIRA")
                .then()
                .extract()
                .as(WorkflowStatusResponse[].class);

        for (final WorkflowStatusResponse status : statuses) {
            if (status.getName().equals("In Progress")) {
                return status.getId().toString();
            }
        }
        throw new IllegalStateException("Status not found: " + "In Progress");
    }
}
