package eu.itsonix.genai.xira.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import eu.itsonix.genai.xira.web.model.CreateProjectRequest;
import eu.itsonix.genai.xira.web.model.LoginRequest;
import eu.itsonix.genai.xira.web.model.RegisterRequest;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

class ProjectControllerIntegrationTest extends BaseIntegrationTest {

    private static final String EMAIL = "owner@example.com";
    private static final String PASSWORD = "password";

    @Test
    void givenAuthenticatedUser_whenCreateProject_thenReturnsCreatedProject() {
        registerUser();
        final String token = login();

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new CreateProjectRequest().key("XIRA").name("Xira Project"))
                .when()
                .post("/projects")
                .then()
                .statusCode(201)
                .header(HttpHeaders.LOCATION, equalTo("/projects/XIRA"));
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
}
