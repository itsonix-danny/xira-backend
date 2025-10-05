package eu.itsonix.genai.xira.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

class UserControllerIntegrationTest extends BaseIntegrationTest {

    private static final String EMAIL = "user@example.com";
    private static final String PASSWORD = "password";

    @Test
    void givenValidEmail_whenGetUsers_thenReturnsUserDetails() {
        registerUser(EMAIL, PASSWORD, "John", "Doe");
        final String token = login(EMAIL, PASSWORD);

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
        registerUser(EMAIL, PASSWORD, "John", "Doe");
        final String token = login(EMAIL, PASSWORD);

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
        given().contentType(ContentType.JSON).queryParam("email", EMAIL).when().get("/users").then().statusCode(401);
    }

    @Test
    void givenEmailWithDifferentCase_whenGetUsers_thenReturnsUserDetails() {
        registerUser(EMAIL, PASSWORD, "John", "Doe");
        final String token = login(EMAIL, PASSWORD);

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

    @Test
    void givenMissingEmailParam_whenGetUsers_thenReturnsBadRequest() {
        registerUser(EMAIL, PASSWORD, "John", "Doe");
        final String token = login(EMAIL, PASSWORD);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .when()
                .get("/users")
                .then()
                .statusCode(400);
    }

    @Test
    void givenInvalidEmailFormat_whenGetUsers_thenReturnsBadRequest() {
        registerUser(EMAIL, PASSWORD, "John", "Doe");
        final String token = login(EMAIL, PASSWORD);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .queryParam("email", "invalid")
                .when()
                .get("/users")
                .then()
                .statusCode(400);
    }

}
