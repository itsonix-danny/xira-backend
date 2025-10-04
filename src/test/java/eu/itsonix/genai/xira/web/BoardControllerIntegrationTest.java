package eu.itsonix.genai.xira.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import eu.itsonix.genai.xira.web.model.AddBoardRequest;
import eu.itsonix.genai.xira.web.model.CreateProjectRequest;
import eu.itsonix.genai.xira.web.model.LoginRequest;
import eu.itsonix.genai.xira.web.model.RegisterRequest;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;

class BoardControllerIntegrationTest extends BaseIntegrationTest {

    private static final String EMAIL = "owner@example.com";
    private static final String PASSWORD = "password";

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
                .body(new RegisterRequest().email("member@example.com").password(PASSWORD).firstName("Member").lastName("User"))
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
