package eu.itsonix.genai.xira.web;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import eu.itsonix.genai.xira.web.model.AddBoardRequest;
import eu.itsonix.genai.xira.web.model.AddSprintRequest;
import eu.itsonix.genai.xira.web.model.RegisterRequest;
import eu.itsonix.genai.xira.web.model.UpdateBoardRequest;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;

class BoardControllerIntegrationTest extends BaseIntegrationTest {

    private static final String EMAIL = "owner@example.com";
    private static final String PASSWORD = "password";

    @Test
    void givenAdmin_whenAddKanbanBoard_thenReturnsCreated() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);

        createProject(token);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new AddBoardRequest().name("Development Board").type(AddBoardRequest.TypeEnum.KANBAN))
                .when()
                .post("/projects/XIRA/boards")
                .then()
                .statusCode(201)
                .header(HttpHeaders.LOCATION, matchesPattern("^/projects/XIRA/boards/.+$"));
    }

    @Test
    void givenExistingScrumBoard_whenAddAnotherScrumBoard_thenReturnsConflict() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);

        createProject(token);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new AddBoardRequest().name("Scrum Board").type(AddBoardRequest.TypeEnum.SCRUM))
                .when()
                .post("/projects/XIRA/boards")
                .then()
                .statusCode(201);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new AddBoardRequest().name("Another Scrum Board").type(AddBoardRequest.TypeEnum.SCRUM))
                .when()
                .post("/projects/XIRA/boards")
                .then()
                .statusCode(409)
                .body("detail", equalTo("Project already has a Scrum board"));
    }

    @Test
    void givenNonAdmin_whenAddBoard_thenReturnsForbidden() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String ownerToken = login(EMAIL, PASSWORD);
        createProject(ownerToken);

        given().contentType(ContentType.JSON)
                .body(new RegisterRequest().email("member@example.com")
                        .password(PASSWORD)
                        .firstName("Member")
                        .lastName("User"))
                .when()
                .post("/auth/register")
                .then()
                .statusCode(201);

        final String memberToken = login("member@example.com", PASSWORD);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", memberToken))
                .body(new AddBoardRequest().name("Team Board").type(AddBoardRequest.TypeEnum.KANBAN))
                .when()
                .post("/projects/XIRA/boards")
                .then()
                .statusCode(403);
    }

    @Test
    void givenUnauthenticated_whenAddBoard_thenReturnsUnauthorized() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);
        createProject(token);

        given().contentType(ContentType.JSON)
                .body(new AddBoardRequest().name("Board").type(AddBoardRequest.TypeEnum.KANBAN))
                .when()
                .post("/projects/XIRA/boards")
                .then()
                .statusCode(401);
    }

    @Test
    void givenNonExistentProject_whenAddBoard_thenReturnsForbidden() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new AddBoardRequest().name("Board").type(AddBoardRequest.TypeEnum.KANBAN))
                .when()
                .post("/projects/NONEXISTENT/boards")
                .then()
                .statusCode(403);
    }

    @Test
    void givenEmptyName_whenAddBoard_thenReturnsBadRequest() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);
        createProject(token);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new AddBoardRequest().name("").type(AddBoardRequest.TypeEnum.KANBAN))
                .when()
                .post("/projects/XIRA/boards")
                .then()
                .statusCode(400);
    }

    @Test
    void givenTooLongName_whenAddBoard_thenReturnsBadRequest() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);
        createProject(token);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new AddBoardRequest().name("A".repeat(101)).type(AddBoardRequest.TypeEnum.KANBAN))
                .when()
                .post("/projects/XIRA/boards")
                .then()
                .statusCode(400);
    }

    @Test
    void givenMultipleBoards_whenAddBoards_thenBoardNumbersIncrement() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);
        createProject(token);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new AddBoardRequest().name("First Board").type(AddBoardRequest.TypeEnum.KANBAN))
                .when()
                .post("/projects/XIRA/boards")
                .then()
                .statusCode(201)
                .header(HttpHeaders.LOCATION, equalTo("/projects/XIRA/boards/1"));

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new AddBoardRequest().name("Scrum Board").type(AddBoardRequest.TypeEnum.SCRUM))
                .when()
                .post("/projects/XIRA/boards")
                .then()
                .statusCode(201)
                .header(HttpHeaders.LOCATION, equalTo("/projects/XIRA/boards/2"));

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new AddBoardRequest().name("Third Board").type(AddBoardRequest.TypeEnum.KANBAN))
                .when()
                .post("/projects/XIRA/boards")
                .then()
                .statusCode(201)
                .header(HttpHeaders.LOCATION, equalTo("/projects/XIRA/boards/3"));
    }

    @Test
    void givenAdmin_whenUpdateBoard_thenReturnsSuccess() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);
        createProject(token);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new AddBoardRequest().name("Original Board").type(AddBoardRequest.TypeEnum.KANBAN))
                .when()
                .post("/projects/XIRA/boards")
                .then()
                .statusCode(201);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new UpdateBoardRequest().name("Updated Board"))
                .when()
                .patch("/projects/XIRA/boards/1")
                .then()
                .statusCode(200);
    }

    @Test
    void givenNonAdmin_whenUpdateBoard_thenReturnsForbidden() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String ownerToken = login(EMAIL, PASSWORD);
        createProject(ownerToken);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(new AddBoardRequest().name("Board").type(AddBoardRequest.TypeEnum.KANBAN))
                .when()
                .post("/projects/XIRA/boards")
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

        final String memberToken = login("member@example.com", PASSWORD);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", memberToken))
                .body(new UpdateBoardRequest().name("Updated Board"))
                .when()
                .patch("/projects/XIRA/boards/1")
                .then()
                .statusCode(403);
    }

    @Test
    void givenUnauthenticated_whenUpdateBoard_thenReturnsUnauthorized() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);
        createProject(token);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new AddBoardRequest().name("Board").type(AddBoardRequest.TypeEnum.KANBAN))
                .when()
                .post("/projects/XIRA/boards")
                .then()
                .statusCode(201);

        given().contentType(ContentType.JSON)
                .body(new UpdateBoardRequest().name("Updated Board"))
                .when()
                .patch("/projects/XIRA/boards/1")
                .then()
                .statusCode(401);
    }

    @Test
    void givenNonExistentBoard_whenUpdateBoard_thenReturnsNotFound() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);
        createProject(token);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new UpdateBoardRequest().name("Updated Board"))
                .when()
                .patch("/projects/XIRA/boards/99")
                .then()
                .statusCode(404);
    }

    @Test
    void givenEmptyName_whenUpdateBoard_thenReturnsBadRequest() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);
        createProject(token);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new AddBoardRequest().name("Board").type(AddBoardRequest.TypeEnum.KANBAN))
                .when()
                .post("/projects/XIRA/boards")
                .then()
                .statusCode(201);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new UpdateBoardRequest().name(""))
                .when()
                .patch("/projects/XIRA/boards/1")
                .then()
                .statusCode(400);
    }

    @Test
    void givenTooLongName_whenUpdateBoard_thenReturnsBadRequest() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);
        createProject(token);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new AddBoardRequest().name("Board").type(AddBoardRequest.TypeEnum.KANBAN))
                .when()
                .post("/projects/XIRA/boards")
                .then()
                .statusCode(201);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new UpdateBoardRequest().name("A".repeat(101)))
                .when()
                .patch("/projects/XIRA/boards/1")
                .then()
                .statusCode(400);
    }

    @Test
    void givenMissingRequiredFields_whenAddBoard_thenReturnsBadRequest() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);
        createProject(token);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body("{}")
                .when()
                .post("/projects/XIRA/boards")
                .then()
                .statusCode(400);
    }

    @Test
    void givenInvalidBoardType_whenAddBoard_thenReturnsBadRequest() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);
        createProject(token);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body("{\"name\":\"Board\",\"type\":\"INVALID\"}")
                .when()
                .post("/projects/XIRA/boards")
                .then()
                .statusCode(400);
    }

    @Test
    void givenNonExistentProject_whenUpdateBoard_thenReturnsForbidden() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new UpdateBoardRequest().name("Updated Board"))
                .when()
                .patch("/projects/NONEXISTENT/boards/1")
                .then()
                .statusCode(403);
    }

    @Test
    void givenInvalidBoardNumber_whenUpdateBoard_thenReturnsBadRequest() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);
        createProject(token);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new UpdateBoardRequest().name("Updated Board"))
                .when()
                .patch("/projects/XIRA/boards/abc")
                .then()
                .statusCode(400);
    }

    @Test
    void givenScrumBoardWithData_whenGetBoardDetails_thenReturns200WithCorrectStructure() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);
        createProject(token);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new AddBoardRequest().name("Scrum Board").type(AddBoardRequest.TypeEnum.SCRUM))
                .when()
                .post("/projects/XIRA/boards")
                .then()
                .statusCode(201);

        given().header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .when()
                .get("/projects/XIRA/boards/1")
                .then()
                .statusCode(200)
                .body("name", equalTo("Scrum Board"))
                .body("type", equalTo("SCRUM"))
                .body("backlog", equalTo(List.of()));
    }

    @Test
    void givenKanbanBoardWithIssues_whenGetBoardDetails_thenReturns200WithColumns() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);
        createProject(token);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new AddBoardRequest().name("Kanban Board").type(AddBoardRequest.TypeEnum.KANBAN))
                .when()
                .post("/projects/XIRA/boards")
                .then()
                .statusCode(201);

        given().header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .when()
                .get("/projects/XIRA/boards/1")
                .then()
                .statusCode(200)
                .body("name", equalTo("Kanban Board"))
                .body("type", equalTo("KANBAN"))
                .body("columns.size()", equalTo(3))
                .body("columns[0].statuses.size()", equalTo(1))
                .body("columns[1].statuses.size()", equalTo(1))
                .body("columns[2].statuses.size()", equalTo(1))
                .body("columns[0].statuses[0].name", equalTo("To Do"))
                .body("columns[1].statuses[0].name", equalTo("In Progress"))
                .body("columns[2].statuses[0].name", equalTo("Done"));
    }

    @Test
    void givenNonExistentBoard_whenGetBoardDetails_thenReturns404() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);
        createProject(token);

        given().header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .when()
                .get("/projects/XIRA/boards/99")
                .then()
                .statusCode(404);
    }

    @Test
    void givenNonMember_whenGetBoardDetails_thenReturns403() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String ownerToken = login(EMAIL, PASSWORD);
        createProject(ownerToken);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(new AddBoardRequest().name("Board").type(AddBoardRequest.TypeEnum.KANBAN))
                .when()
                .post("/projects/XIRA/boards")
                .then()
                .statusCode(201);

        given().contentType(ContentType.JSON)
                .body(new RegisterRequest().email("outsider@example.com")
                        .password(PASSWORD)
                        .firstName("Outsider")
                        .lastName("User"))
                .when()
                .post("/auth/register")
                .then()
                .statusCode(201);

        final String outsiderToken = login("outsider@example.com", PASSWORD);

        given().header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", outsiderToken))
                .when()
                .get("/projects/XIRA/boards/1")
                .then()
                .statusCode(403);
    }

    @Test
    void givenUnauthenticated_whenGetBoardDetails_thenReturns401() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);
        createProject(token);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new AddBoardRequest().name("Board").type(AddBoardRequest.TypeEnum.KANBAN))
                .when()
                .post("/projects/XIRA/boards")
                .then()
                .statusCode(201);

        given().when().get("/projects/XIRA/boards/1").then().statusCode(401);
    }

    @Test
    void givenScrumBoardWithoutActiveSprint_whenGetActiveSprint_thenReturns204() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);
        createProject(token);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new AddBoardRequest().name("Scrum Board").type(AddBoardRequest.TypeEnum.SCRUM))
                .when()
                .post("/projects/XIRA/boards")
                .then()
                .statusCode(201);

        given().header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .when()
                .get("/projects/XIRA/boards/1/active-sprint")
                .then()
                .statusCode(204);
    }

    @Test
    void givenKanbanBoard_whenGetActiveSprint_thenReturns404() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);
        createProject(token);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new AddBoardRequest().name("Kanban Board").type(AddBoardRequest.TypeEnum.KANBAN))
                .when()
                .post("/projects/XIRA/boards")
                .then()
                .statusCode(201);

        given().header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .when()
                .get("/projects/XIRA/boards/1/active-sprint")
                .then()
                .statusCode(404)
                .body("detail", equalTo("Board not found or not a SCRUM board"));
    }

    @Test
    void givenNonExistentBoard_whenGetActiveSprint_thenReturns404() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);
        createProject(token);

        given().header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .when()
                .get("/projects/XIRA/boards/99/active-sprint")
                .then()
                .statusCode(404);
    }

    @Test
    void givenUnauthenticated_whenGetActiveSprint_thenReturns401() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);
        createProject(token);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new AddBoardRequest().name("Scrum Board").type(AddBoardRequest.TypeEnum.SCRUM))
                .when()
                .post("/projects/XIRA/boards")
                .then()
                .statusCode(201);

        given().when().get("/projects/XIRA/boards/1/active-sprint").then().statusCode(401);
    }

    @Test
    void givenNonMember_whenGetActiveSprint_thenReturns403() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String ownerToken = login(EMAIL, PASSWORD);
        createProject(ownerToken);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(new AddBoardRequest().name("Scrum Board").type(AddBoardRequest.TypeEnum.SCRUM))
                .when()
                .post("/projects/XIRA/boards")
                .then()
                .statusCode(201);

        given().contentType(ContentType.JSON)
                .body(new RegisterRequest().email("outsider@example.com")
                        .password(PASSWORD)
                        .firstName("Outsider")
                        .lastName("User"))
                .when()
                .post("/auth/register")
                .then()
                .statusCode(201);

        final String outsiderToken = login("outsider@example.com", PASSWORD);

        given().header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", outsiderToken))
                .when()
                .get("/projects/XIRA/boards/1/active-sprint")
                .then()
                .statusCode(403);
    }

    @Test
    void givenScrumBoardWithActiveSprint_whenGetActiveSprint_thenReturns200WithColumnsAndStatuses() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);
        createProject(token);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new AddBoardRequest().name("Scrum Board").type(AddBoardRequest.TypeEnum.SCRUM))
                .when()
                .post("/projects/XIRA/boards")
                .then()
                .statusCode(201);

        final String sprintLocation = given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new AddSprintRequest().name("Sprint 1").goal("First Sprint"))
                .when()
                .post("/projects/XIRA/sprints")
                .then()
                .statusCode(201)
                .extract()
                .header(HttpHeaders.LOCATION);

        final String sprintId = extractIdFromLocation(sprintLocation);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .when()
                .post("/projects/XIRA/sprints/{sprintId}/start", UUID.fromString(sprintId))
                .then()
                .statusCode(204);

        given().header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .when()
                .get("/projects/XIRA/boards/1/active-sprint")
                .then()
                .statusCode(200)
                .body("name", equalTo("Sprint 1"))
                .body("state", equalTo("ACTIVE"))
                .body("columns.size()", equalTo(3))
                .body("columns[0].statuses.size()", equalTo(1))
                .body("columns[1].statuses.size()", equalTo(1))
                .body("columns[2].statuses.size()", equalTo(1))
                .body("columns[0].statuses[0].name", equalTo("To Do"))
                .body("columns[1].statuses[0].name", equalTo("In Progress"))
                .body("columns[2].statuses[0].name", equalTo("Done"));
    }

    @Test
    void givenAdmin_whenDeleteBoard_thenReturnsNoContent() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);
        createProject(token);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new AddBoardRequest().name("Board to Delete").type(AddBoardRequest.TypeEnum.KANBAN))
                .when()
                .post("/projects/XIRA/boards")
                .then()
                .statusCode(201);

        given().header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .when()
                .delete("/projects/XIRA/boards/1")
                .then()
                .statusCode(204);
    }

    @Test
    void givenNonAdmin_whenDeleteBoard_thenReturnsForbidden() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String ownerToken = login(EMAIL, PASSWORD);
        createProject(ownerToken);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", ownerToken))
                .body(new AddBoardRequest().name("Board").type(AddBoardRequest.TypeEnum.KANBAN))
                .when()
                .post("/projects/XIRA/boards")
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

        final String memberToken = login("member@example.com", PASSWORD);

        given().header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", memberToken))
                .when()
                .delete("/projects/XIRA/boards/1")
                .then()
                .statusCode(403);
    }

    @Test
    void givenUnauthenticated_whenDeleteBoard_thenReturnsUnauthorized() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);
        createProject(token);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new AddBoardRequest().name("Board").type(AddBoardRequest.TypeEnum.KANBAN))
                .when()
                .post("/projects/XIRA/boards")
                .then()
                .statusCode(201);

        given().when().delete("/projects/XIRA/boards/1").then().statusCode(401);
    }

    @Test
    void givenNonExistentBoard_whenDeleteBoard_thenReturnsNotFound() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);
        createProject(token);

        given().header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .when()
                .delete("/projects/XIRA/boards/99")
                .then()
                .statusCode(404);
    }

    @Test
    void givenNonExistentProject_whenDeleteBoard_thenReturnsForbidden() {
        registerUser(EMAIL, PASSWORD, "Owner", "Example");
        final String token = login(EMAIL, PASSWORD);

        given().header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .when()
                .delete("/projects/NONEXISTENT/boards/1")
                .then()
                .statusCode(403);
    }

}
