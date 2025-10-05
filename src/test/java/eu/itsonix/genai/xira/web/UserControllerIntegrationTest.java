package eu.itsonix.genai.xira.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import eu.itsonix.genai.xira.web.model.LoginRequest;
import eu.itsonix.genai.xira.web.model.RegisterRequest;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

class UserControllerIntegrationTest extends BaseIntegrationTest {

    private static final String EMAIL = "user@example.com";
    private static final String PASSWORD = "password";

    @Test
    void givenValidEmail_whenGetUsers_thenReturnsUserDetails() {
        registerUser();
        final String token = login();

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .queryParam("email", EMAIL)
                .when()
                .get("/users")
                .then()
                .statusCode(200)
                .body("email", equalTo(EMAIL))
                .body("firstName", equalTo("John"))
                .body("lastName", equalTo("Doe"));
    }

    @Test
    void givenNonExistentEmail_whenGetUsers_thenReturnsNotFound() {
        registerUser();
        final String token = login();

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .queryParam("email", "missing@example.com")
                .when()
                .get("/users")
                .then()
                .statusCode(404);
    }

    @Test
    void givenUnauthenticated_whenGetUsers_thenReturnsUnauthorized() {
        given().contentType(ContentType.JSON)
                .queryParam("email", EMAIL)
                .when()
                .get("/users")
                .then()
                .statusCode(401);
    }

    @Test
    void givenEmailWithDifferentCase_whenGetUsers_thenReturnsUserDetails() {
        registerUser();
        final String token = login();

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .queryParam("email", EMAIL.toUpperCase())
                .when()
                .get("/users")
                .then()
                .statusCode(200)
                .body("email", equalTo(EMAIL))
                .body("firstName", equalTo("John"))
                .body("lastName", equalTo("Doe"));
    }

    private void registerUser() {
        given().contentType(ContentType.JSON)
                .body(new RegisterRequest().email(EMAIL)
                        .password(PASSWORD)
                        .firstName("John")
                        .lastName("Doe"))
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
