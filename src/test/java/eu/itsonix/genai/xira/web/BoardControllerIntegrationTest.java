package eu.itsonix.genai.xira.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import eu.itsonix.genai.xira.jpa.entity.BoardColumnWorkflowStatus;
import eu.itsonix.genai.xira.jpa.repository.BoardColumnRepository;
import eu.itsonix.genai.xira.jpa.repository.BoardColumnWorkflowStatusRepository;
import eu.itsonix.genai.xira.jpa.repository.WorkflowStatusRepository;
import eu.itsonix.genai.xira.web.model.*;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;

class BoardControllerIntegrationTest extends BaseIntegrationTest {

    private static final String EMAIL = "owner@example.com";
    private static final String PASSWORD = "password";

    @Autowired
    private BoardColumnRepository boardColumnRepository;

    @Autowired
    private BoardColumnWorkflowStatusRepository boardColumnWorkflowStatusRepository;

    @Autowired
    private WorkflowStatusRepository workflowStatusRepository;

    @Test
    void givenAdmin_whenAddKanbanBoard_thenReturnsCreated() {
        registerUser();
        final String token = login(EMAIL);

        createProject(token);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new AddBoardRequest().name("Development Board").type(AddBoardRequest.TypeEnum.KANBAN))
                .when()
                .post("/projects/XIRA/boards")
                .then()
                .statusCode(201)
                .header(HttpHeaders.LOCATION, matchesPattern("^/projects/XIRA/boards/.+$"));

        final var boardColumns = boardColumnRepository.findAll();
        assertThat(boardColumns).hasSize(3);
        assertThat(boardColumns.getFirst().getName()).isEqualTo("To Do");
        assertThat(boardColumns.get(1).getName()).isEqualTo("In Progress");
        assertThat(boardColumns.get(2).getName()).isEqualTo("Done");

        final var columnWorkflowStatuses = boardColumnWorkflowStatusRepository.findAll();
        assertThat(columnWorkflowStatuses).hasSize(3);
        assertThat(columnWorkflowStatuses).allMatch(BoardColumnWorkflowStatus::getIsDefault);
    }

    @Test
    void givenExistingScrumBoard_whenAddAnotherScrumBoard_thenReturnsConflict() {
        registerUser();
        final String token = login(EMAIL);

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
        registerUser();
        final String ownerToken = login(EMAIL);
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

        final String memberToken = login("member@example.com");

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
        registerUser();
        final String token = login(EMAIL);
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
        registerUser();
        final String token = login(EMAIL);

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
        registerUser();
        final String token = login(EMAIL);
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
        registerUser();
        final String token = login(EMAIL);
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
    void givenMultipleKanbanBoards_whenAddBoards_thenAllSucceed() {
        registerUser();
        final String token = login(EMAIL);
        createProject(token);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new AddBoardRequest().name("Kanban Board 1").type(AddBoardRequest.TypeEnum.KANBAN))
                .when()
                .post("/projects/XIRA/boards")
                .then()
                .statusCode(201)
                .header(HttpHeaders.LOCATION, equalTo("/projects/XIRA/boards/1"));

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new AddBoardRequest().name("Kanban Board 2").type(AddBoardRequest.TypeEnum.KANBAN))
                .when()
                .post("/projects/XIRA/boards")
                .then()
                .statusCode(201)
                .header(HttpHeaders.LOCATION, equalTo("/projects/XIRA/boards/2"));
    }

    @Test
    void givenMultipleBoards_whenAddBoards_thenBoardNumbersIncrement() {
        registerUser();
        final String token = login(EMAIL);
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
        registerUser();
        final String token = login(EMAIL);
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
        registerUser();
        final String ownerToken = login(EMAIL);
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

        final String memberToken = login("member@example.com");

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
        registerUser();
        final String token = login(EMAIL);
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
        registerUser();
        final String token = login(EMAIL);
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
        registerUser();
        final String token = login(EMAIL);
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
        registerUser();
        final String token = login(EMAIL);
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
    void givenKanbanBoard_whenCreated_thenColumnsMapToCorrectWorkflowStatuses() {
        registerUser();
        final String token = login(EMAIL);
        createProject(token);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new AddBoardRequest().name("Dev Board").type(AddBoardRequest.TypeEnum.KANBAN))
                .when()
                .post("/projects/XIRA/boards")
                .then()
                .statusCode(201);

        final var workflowStatuses = workflowStatusRepository.findAll();
        final var todoStatus = workflowStatuses.stream()
                .filter(ws -> ws.getCategory() == eu.itsonix.genai.xira.jpa.entity.WorkflowStatusCategory.TODO)
                .findFirst()
                .orElseThrow();
        final var inProgressStatus = workflowStatuses.stream()
                .filter(ws -> ws.getCategory() == eu.itsonix.genai.xira.jpa.entity.WorkflowStatusCategory.IN_PROGRESS)
                .findFirst()
                .orElseThrow();
        final var doneStatus = workflowStatuses.stream()
                .filter(ws -> ws.getCategory() == eu.itsonix.genai.xira.jpa.entity.WorkflowStatusCategory.DONE)
                .findFirst()
                .orElseThrow();

        final var columnMappings = boardColumnWorkflowStatusRepository.findAll();
        assertThat(columnMappings).hasSize(3);

        final var todoMapping = columnMappings.stream()
                .filter(m -> m.getBoardColumn().getName().equals("To Do"))
                .findFirst()
                .orElseThrow();
        assertThat(todoMapping.getWorkflowStatusId()).isEqualTo(todoStatus.getId());

        final var inProgressMapping = columnMappings.stream()
                .filter(m -> m.getBoardColumn().getName().equals("In Progress"))
                .findFirst()
                .orElseThrow();
        assertThat(inProgressMapping.getWorkflowStatusId()).isEqualTo(inProgressStatus.getId());

        final var doneMapping = columnMappings.stream()
                .filter(m -> m.getBoardColumn().getName().equals("Done"))
                .findFirst()
                .orElseThrow();
        assertThat(doneMapping.getWorkflowStatusId()).isEqualTo(doneStatus.getId());
    }

    private void registerUser() {
        given().contentType(ContentType.JSON)
                .body(new RegisterRequest().email(EMAIL).password(PASSWORD).firstName("Owner").lastName("Example"))
                .when()
                .post("/auth/register")
                .then()
                .statusCode(201);
    }

    private void createProject(final String token) {
        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new CreateProjectRequest().key("XIRA").name("Xira Project"))
                .when()
                .post("/projects")
                .then()
                .statusCode(201);
    }

    private String login(final String email) {
        return given().contentType(ContentType.JSON)
                .body(new LoginRequest().email(email).password(PASSWORD))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("access_token");
    }
}
