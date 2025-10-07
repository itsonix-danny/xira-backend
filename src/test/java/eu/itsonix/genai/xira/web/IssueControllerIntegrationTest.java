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
    void givenPriorityUpdate_whenUpdateIssue_thenReturns200() {
        registerUser("user@example.com", "password", "John", "Doe");
        final String token = login("user@example.com", "password");
        createProject(token);

        final CreateIssueRequest createRequest = new CreateIssueRequest().title("Test Issue")
                .issueType(CreateIssueRequest.IssueTypeEnum.BUG)
                .priority(CreateIssueRequest.PriorityEnum.LOW);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(createRequest)
                .post("/projects/{key}/issues", "XIRA");

        final UpdateIssueRequest updateRequest = new UpdateIssueRequest()
                .priority(UpdateIssueRequest.PriorityEnum.CRITICAL);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(updateRequest)
                .when()
                .patch("/projects/{key}/issues/{issueKey}", "XIRA", "XIRA-1")
                .then()
                .statusCode(200);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .when()
                .get("/projects/{key}/issues/{issueKey}", "XIRA", "XIRA-1")
                .then()
                .statusCode(200)
                .body("priority", org.hamcrest.Matchers.equalTo("CRITICAL"));
    }

    @Test
    void givenIssueTypeUpdate_whenUpdateIssue_thenReturns200() {
        registerUser("user@example.com", "password", "John", "Doe");
        final String token = login("user@example.com", "password");
        createProject(token);

        final CreateIssueRequest createRequest = new CreateIssueRequest().title("Test Issue")
                .issueType(CreateIssueRequest.IssueTypeEnum.TASK)
                .priority(CreateIssueRequest.PriorityEnum.MEDIUM);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(createRequest)
                .post("/projects/{key}/issues", "XIRA");

        final UpdateIssueRequest updateRequest = new UpdateIssueRequest()
                .issueType(UpdateIssueRequest.IssueTypeEnum.STORY);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(updateRequest)
                .when()
                .patch("/projects/{key}/issues/{issueKey}", "XIRA", "XIRA-1")
                .then()
                .statusCode(200);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .when()
                .get("/projects/{key}/issues/{issueKey}", "XIRA", "XIRA-1")
                .then()
                .statusCode(200)
                .body("issueType", org.hamcrest.Matchers.equalTo("STORY"));
    }

    @Test
    void givenAllFieldsUpdate_whenUpdateIssue_thenReturns200() {
        registerUser("user@example.com", "password", "John", "Doe");
        final String token = login("user@example.com", "password");
        createProject(token);

        final CreateIssueRequest createRequest = new CreateIssueRequest().title("Original Title")
                .issueType(CreateIssueRequest.IssueTypeEnum.TASK)
                .priority(CreateIssueRequest.PriorityEnum.LOW);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(createRequest)
                .post("/projects/{key}/issues", "XIRA");

        final UpdateIssueRequest updateRequest = new UpdateIssueRequest().title("Updated Title")
                .description("Updated Description")
                .issueType(UpdateIssueRequest.IssueTypeEnum.BUG)
                .priority(UpdateIssueRequest.PriorityEnum.HIGH);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(updateRequest)
                .when()
                .patch("/projects/{key}/issues/{issueKey}", "XIRA", "XIRA-1")
                .then()
                .statusCode(200);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .when()
                .get("/projects/{key}/issues/{issueKey}", "XIRA", "XIRA-1")
                .then()
                .statusCode(200)
                .body("title", org.hamcrest.Matchers.equalTo("Updated Title"))
                .body("description", org.hamcrest.Matchers.equalTo("Updated Description"))
                .body("issueType", org.hamcrest.Matchers.equalTo("BUG"))
                .body("priority", org.hamcrest.Matchers.equalTo("HIGH"));
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

        final String statusId = getWorkflowStatusId(token, "In Progress");
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

        final String statusId = getWorkflowStatusId(ownerToken, "In Progress");
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

        final String location = given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(addRequest)
                .post("/projects/{key}/issues/{issueKey}/comments", "XIRA", "XIRA-1")
                .then()
                .extract()
                .header(HttpHeaders.LOCATION);
        final String commentId = extractIdFromLocation(location);

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

        final String location = given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(addRequest)
                .post("/projects/{key}/issues/{issueKey}/comments", "XIRA", "XIRA-1")
                .then()
                .extract()
                .header(HttpHeaders.LOCATION);
        final String commentId = extractIdFromLocation(location);

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

        final String location = given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(addRequest)
                .post("/projects/{key}/issues/{issueKey}/comments", "XIRA", "XIRA-1")
                .then()
                .extract()
                .header(HttpHeaders.LOCATION);
        final String commentId = extractIdFromLocation(location);

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

        final String location = given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", developerToken))
                .body(addRequest)
                .post("/projects/{key}/issues/{issueKey}/comments", "XIRA", "XIRA-1")
                .then()
                .extract()
                .header(HttpHeaders.LOCATION);
        final String commentId = extractIdFromLocation(location);

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

        final String location = given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", developer1Token))
                .body(addRequest)
                .post("/projects/{key}/issues/{issueKey}/comments", "XIRA", "XIRA-1")
                .then()
                .extract()
                .header(HttpHeaders.LOCATION);
        final String commentId = extractIdFromLocation(location);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", developer2Token))
                .when()
                .delete("/projects/{key}/issues/{issueKey}/comments/{commentId}", "XIRA", "XIRA-1", commentId)
                .then()
                .statusCode(403);
    }

    @Test
    void givenNoFilters_whenGetIssues_thenReturnsAllUnfinishedIssuesFromUserProjects() {
        registerUser("user@example.com", "password", "John", "Doe");
        final String token = login("user@example.com", "password");
        createProject(token);

        createIssue(token, "XIRA", "Issue 1", CreateIssueRequest.IssueTypeEnum.BUG);
        createIssue(token, "XIRA", "Issue 2", CreateIssueRequest.IssueTypeEnum.TASK);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .when()
                .get("/issues")
                .then()
                .statusCode(200)
                .body("size()", org.hamcrest.Matchers.equalTo(2));
    }

    @Test
    void givenProjectKeyFilter_whenGetIssues_thenReturnsOnlyIssuesFromThatProject() {
        registerUser("user@example.com", "password", "John", "Doe");
        final String token = login("user@example.com", "password");
        createProject(token);

        createIssue(token, "XIRA", "XIRA Issue", CreateIssueRequest.IssueTypeEnum.BUG);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .queryParam("projectKey", "XIRA")
                .when()
                .get("/issues")
                .then()
                .statusCode(200)
                .body("size()", org.hamcrest.Matchers.equalTo(1))
                .body("[0].key", org.hamcrest.Matchers.equalTo("XIRA-1"));
    }

    @Test
    void givenAssigneeFilter_whenGetIssues_thenReturnsOnlyAssignedIssues() {
        registerUser("admin@example.com", "password", "Admin", "User");
        final String adminToken = login("admin@example.com", "password");
        createProject(adminToken);

        registerUser("developer@example.com", "password", "Dev", "User");
        final String developerId = getUserIdByEmail(adminToken, "developer@example.com");
        addProjectMember(adminToken, developerId, ProjectMemberRole.DEVELOPER);

        createIssue(adminToken, "XIRA", "Unassigned Issue", CreateIssueRequest.IssueTypeEnum.BUG);
        final String issueKey = createIssue(adminToken, "XIRA", "Assigned Issue",
                CreateIssueRequest.IssueTypeEnum.TASK);

        final AddAssigneeRequest request = new AddAssigneeRequest().userId(UUID.fromString(developerId));
        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", adminToken))
                .body(request)
                .post("/projects/{key}/issues/{issueKey}/assignees", "XIRA", issueKey);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", adminToken))
                .queryParam("assigneeId", developerId)
                .when()
                .get("/issues")
                .then()
                .statusCode(200)
                .body("size()", org.hamcrest.Matchers.equalTo(1))
                .body("[0].title", org.hamcrest.Matchers.equalTo("Assigned Issue"));
    }

    @Test
    void givenIncludeFinishedFalse_whenGetIssues_thenReturnsOnlyUnfinished() {
        registerUser("user@example.com", "password", "John", "Doe");
        final String token = login("user@example.com", "password");
        createProject(token);

        createIssue(token, "XIRA", "Open Issue", CreateIssueRequest.IssueTypeEnum.BUG);
        final String finishedIssueKey = createIssue(token, "XIRA", "Finished Issue",
                CreateIssueRequest.IssueTypeEnum.TASK);

        final String doneStatusId = getWorkflowStatusId(token, "Done");
        final SetIssueStatusRequest statusRequest = new SetIssueStatusRequest().statusId(UUID.fromString(doneStatusId));
        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(statusRequest)
                .patch("/projects/{key}/issues/{issueKey}/status", "XIRA", finishedIssueKey);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .queryParam("includeFinished", false)
                .when()
                .get("/issues")
                .then()
                .statusCode(200)
                .body("size()", org.hamcrest.Matchers.equalTo(1))
                .body("[0].title", org.hamcrest.Matchers.equalTo("Open Issue"));
    }

    @Test
    void givenIncludeFinishedTrue_whenGetIssues_thenReturnsAllIssues() {
        registerUser("user@example.com", "password", "John", "Doe");
        final String token = login("user@example.com", "password");
        createProject(token);

        createIssue(token, "XIRA", "Open Issue", CreateIssueRequest.IssueTypeEnum.BUG);
        final String finishedIssueKey = createIssue(token, "XIRA", "Finished Issue",
                CreateIssueRequest.IssueTypeEnum.TASK);

        final String doneStatusId = getWorkflowStatusId(token, "Done");
        final SetIssueStatusRequest statusRequest = new SetIssueStatusRequest().statusId(UUID.fromString(doneStatusId));
        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(statusRequest)
                .patch("/projects/{key}/issues/{issueKey}/status", "XIRA", finishedIssueKey);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .queryParam("includeFinished", true)
                .when()
                .get("/issues")
                .then()
                .statusCode(200)
                .body("size()", org.hamcrest.Matchers.equalTo(2));
    }

    @Test
    void givenSearchFilter_whenGetIssues_thenReturnsMatchingIssues() {
        registerUser("user@example.com", "password", "John", "Doe");
        final String token = login("user@example.com", "password");
        createProject(token);

        createIssue(token, "XIRA", "Bug in login", CreateIssueRequest.IssueTypeEnum.BUG);
        createIssue(token, "XIRA", "Feature request", CreateIssueRequest.IssueTypeEnum.TASK);
        createIssue(token, "XIRA", "Login form styling", CreateIssueRequest.IssueTypeEnum.STORY);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .queryParam("search", "login")
                .when()
                .get("/issues")
                .then()
                .statusCode(200)
                .body("size()", org.hamcrest.Matchers.equalTo(2));
    }

    @Test
    void givenMultipleProjects_whenGetIssues_thenReturnsIssuesFromAllUserProjects() {
        registerUser("user@example.com", "password", "John", "Doe");
        final String token = login("user@example.com", "password");

        createProject(token);
        createIssue(token, "XIRA", "XIRA Issue", CreateIssueRequest.IssueTypeEnum.BUG);

        createProject(token, "TEST", "Test Project");
        createIssue(token, "TEST", "TEST Issue", CreateIssueRequest.IssueTypeEnum.TASK);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .when()
                .get("/issues")
                .then()
                .statusCode(200)
                .body("size()", org.hamcrest.Matchers.equalTo(2));
    }

    @Test
    void givenIssuesSortedBySeqNo_whenGetIssues_thenReturnsInOrder() {
        registerUser("user@example.com", "password", "John", "Doe");
        final String token = login("user@example.com", "password");
        createProject(token);

        createIssue(token, "XIRA", "First Issue", CreateIssueRequest.IssueTypeEnum.BUG);
        createIssue(token, "XIRA", "Second Issue", CreateIssueRequest.IssueTypeEnum.TASK);
        createIssue(token, "XIRA", "Third Issue", CreateIssueRequest.IssueTypeEnum.STORY);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .when()
                .get("/issues")
                .then()
                .statusCode(200)
                .body("size()", org.hamcrest.Matchers.equalTo(3))
                .body("[0].key", org.hamcrest.Matchers.equalTo("XIRA-1"))
                .body("[1].key", org.hamcrest.Matchers.equalTo("XIRA-2"))
                .body("[2].key", org.hamcrest.Matchers.equalTo("XIRA-3"));
    }

    @Test
    void givenUnauthenticated_whenGetIssues_thenReturns401() {
        given().contentType(ContentType.JSON).when().get("/issues").then().statusCode(401);
    }

    @Test
    void givenValidIssueKey_whenGetIssueByKey_thenReturns200WithDetails() {
        registerUser("user@example.com", "password", "John", "Doe");
        final String token = login("user@example.com", "password");
        createProject(token);

        final String issueKey = createIssue(token, "XIRA", "Test Issue", CreateIssueRequest.IssueTypeEnum.BUG);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .when()
                .get("/projects/{key}/issues/{issueKey}", "XIRA", issueKey)
                .then()
                .statusCode(200)
                .body("key", org.hamcrest.Matchers.equalTo(issueKey))
                .body("title", org.hamcrest.Matchers.equalTo("Test Issue"))
                .body("issueType", org.hamcrest.Matchers.equalTo("BUG"))
                .body("priority", org.hamcrest.Matchers.equalTo("HIGH"))
                .body("status.name", org.hamcrest.Matchers.equalTo("To Do"))
                .body("status.category", org.hamcrest.Matchers.equalTo("TODO"))
                .body("reporter.email", org.hamcrest.Matchers.equalTo("user@example.com"))
                .body("createdAt", org.hamcrest.Matchers.notNullValue())
                .body("closedAt", org.hamcrest.Matchers.nullValue())
                .body("sprintName", org.hamcrest.Matchers.nullValue())
                .body("assignees", org.hamcrest.Matchers.hasSize(0))
                .body("comments", org.hamcrest.Matchers.hasSize(0));
    }

    @Test
    void givenIssueWithAssignees_whenGetIssueByKey_thenReturnsAssignees() {
        registerUser("owner@example.com", "password", "Owner", "User");
        final String ownerToken = login("owner@example.com", "password");
        createProject(ownerToken);

        registerUser("developer@example.com", "password", "Dev", "User");
        final String developerId = getUserIdByEmail(ownerToken, "developer@example.com");
        addProjectMember(ownerToken, developerId, ProjectMemberRole.DEVELOPER);

        final String issueKey = createIssue(ownerToken, "XIRA", "Test Issue", CreateIssueRequest.IssueTypeEnum.TASK);

        final AddAssigneeRequest request = new AddAssigneeRequest().userId(UUID.fromString(developerId));
        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(request)
                .post("/projects/{key}/issues/{issueKey}/assignees", "XIRA", issueKey);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .when()
                .get("/projects/{key}/issues/{issueKey}", "XIRA", issueKey)
                .then()
                .statusCode(200)
                .body("assignees", org.hamcrest.Matchers.hasSize(1))
                .body("assignees[0].email", org.hamcrest.Matchers.equalTo("developer@example.com"));
    }

    @Test
    void givenClosedIssue_whenGetIssueByKey_thenReturnsClosedAt() {
        registerUser("user@example.com", "password", "John", "Doe");
        final String token = login("user@example.com", "password");
        createProject(token);

        final String issueKey = createIssue(token, "XIRA", "Test Issue", CreateIssueRequest.IssueTypeEnum.BUG);

        final String doneStatusId = getWorkflowStatusId(token, "Done");
        final SetIssueStatusRequest statusRequest = new SetIssueStatusRequest().statusId(UUID.fromString(doneStatusId));
        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(statusRequest)
                .patch("/projects/{key}/issues/{issueKey}/status", "XIRA", issueKey);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .when()
                .get("/projects/{key}/issues/{issueKey}", "XIRA", issueKey)
                .then()
                .statusCode(200)
                .body("status.name", org.hamcrest.Matchers.equalTo("Done"))
                .body("status.category", org.hamcrest.Matchers.equalTo("DONE"))
                .body("closedAt", org.hamcrest.Matchers.notNullValue());
    }

    @Test
    void givenIssueWithDescription_whenGetIssueByKey_thenReturnsDescription() {
        registerUser("user@example.com", "password", "John", "Doe");
        final String token = login("user@example.com", "password");
        createProject(token);

        final CreateIssueRequest createRequest = new CreateIssueRequest().title("Test Issue")
                .description("Detailed description of the issue")
                .issueType(CreateIssueRequest.IssueTypeEnum.BUG)
                .priority(CreateIssueRequest.PriorityEnum.HIGH);

        final String location = given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(createRequest)
                .post("/projects/{key}/issues", "XIRA")
                .then()
                .extract()
                .header(HttpHeaders.LOCATION);

        final String issueKey = location.substring(location.lastIndexOf('/') + 1);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .when()
                .get("/projects/{key}/issues/{issueKey}", "XIRA", issueKey)
                .then()
                .statusCode(200)
                .body("description", org.hamcrest.Matchers.equalTo("Detailed description of the issue"));
    }

    @Test
    void givenNonMember_whenGetIssueByKey_thenReturns403() {
        registerUser("owner@example.com", "password", "Owner", "User");
        final String ownerToken = login("owner@example.com", "password");
        createProject(ownerToken);

        final String issueKey = createIssue(ownerToken, "XIRA", "Test Issue", CreateIssueRequest.IssueTypeEnum.BUG);

        registerUser("nonmember@example.com", "password", "Non", "Member");
        final String nonMemberToken = login("nonmember@example.com", "password");

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", nonMemberToken))
                .when()
                .get("/projects/{key}/issues/{issueKey}", "XIRA", issueKey)
                .then()
                .statusCode(403);
    }

    @Test
    void givenInvalidIssueKey_whenGetIssueByKey_thenReturns404() {
        registerUser("user@example.com", "password", "John", "Doe");
        final String token = login("user@example.com", "password");
        createProject(token);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .when()
                .get("/projects/{key}/issues/{issueKey}", "XIRA", "XIRA-999")
                .then()
                .statusCode(404);
    }

    @Test
    void givenUnauthenticated_whenGetIssueByKey_thenReturns401() {
        given().contentType(ContentType.JSON)
                .when()
                .get("/projects/{key}/issues/{issueKey}", "XIRA", "XIRA-1")
                .then()
                .statusCode(401);
    }

    @Test
    void givenIssueWithComments_whenGetIssueByKey_thenReturnsComments() {
        registerUser("user@example.com", "password", "John", "Doe");
        final String token = login("user@example.com", "password");
        createProject(token);

        final String issueKey = createIssue(token, "XIRA", "Test Issue", CreateIssueRequest.IssueTypeEnum.BUG);

        final AddCommentRequest comment1 = new AddCommentRequest().content("First comment");
        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(comment1)
                .post("/projects/{key}/issues/{issueKey}/comments", "XIRA", issueKey);

        final AddCommentRequest comment2 = new AddCommentRequest().content("Second comment");
        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(comment2)
                .post("/projects/{key}/issues/{issueKey}/comments", "XIRA", issueKey);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .when()
                .get("/projects/{key}/issues/{issueKey}", "XIRA", issueKey)
                .then()
                .statusCode(200)
                .body("comments", org.hamcrest.Matchers.hasSize(2))
                .body("comments[0].content", org.hamcrest.Matchers.notNullValue())
                .body("comments[0].author.email", org.hamcrest.Matchers.equalTo("user@example.com"))
                .body("comments[0].createdAt", org.hamcrest.Matchers.notNullValue())
                .body("comments[0].updatedAt", org.hamcrest.Matchers.notNullValue())
                .body("comments[1].content", org.hamcrest.Matchers.notNullValue())
                .body("comments[1].author.email", org.hamcrest.Matchers.equalTo("user@example.com"));
    }
}
