package eu.itsonix.genai.xira.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import eu.itsonix.genai.xira.web.model.AddBoardRequest;
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

}
