package eu.itsonix.genai.xira.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

class UserControllerIntegrationTest extends BaseIntegrationTest {

    private static final String EMAIL = "user@example.com";
    private static final String PASSWORD = "password";

    @Test
    void givenValidEmail_whenGetUsers_thenReturnsUserInArray() {
        registerUser(EMAIL, PASSWORD, "John", "Doe");
        final String token = login(EMAIL, PASSWORD);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .queryParam("email", EMAIL)
                .when()
                .get("/users")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].email", equalTo(EMAIL))
                .body("[0].firstName", equalTo("John"))
                .body("[0].lastName", equalTo("Doe"));
    }

    @Test
    void givenNonExistentEmail_whenGetUsers_thenReturnsEmptyArray() {
        registerUser(EMAIL, PASSWORD, "John", "Doe");
        final String token = login(EMAIL, PASSWORD);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .queryParam("email", "missing@example.com")
                .when()
                .get("/users")
                .then()
                .statusCode(200)
                .body("$", hasSize(0));
    }

    @Test
    void givenUnauthenticated_whenGetUsers_thenReturnsUnauthorized() {
        given().contentType(ContentType.JSON).when().get("/users").then().statusCode(401);
    }

    @Test
    void givenEmailWithDifferentCase_whenGetUsers_thenReturnsUserInArray() {
        registerUser(EMAIL, PASSWORD, "John", "Doe");
        final String token = login(EMAIL, PASSWORD);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .queryParam("email", EMAIL.toUpperCase())
                .when()
                .get("/users")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].email", equalTo(EMAIL))
                .body("[0].firstName", equalTo("John"))
                .body("[0].lastName", equalTo("Doe"));
    }

    @Test
    void givenNoParams_whenGetUsers_thenReturnsAllUsers() {
        registerUser("user1@example.com", PASSWORD, "John", "Doe");
        registerUser("user2@example.com", PASSWORD, "Jane", "Smith");
        final String token = login("user1@example.com", PASSWORD);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .when()
                .get("/users")
                .then()
                .statusCode(200)
                .body("$", hasSize(2));
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

    @Test
    void givenUserNotInProject_whenGetUsersWithProjectKey_thenReturnsUserInArray() {
        final String user1Email = "user1@example.com";
        final String user2Email = "user2@example.com";
        registerUser(user1Email, PASSWORD, "John", "Doe");
        registerUser(user2Email, PASSWORD, "Jane", "Smith");
        final String token = login(user1Email, PASSWORD);

        createProject(token);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .queryParam("email", user2Email)
                .queryParam("projectKey", "XIRA")
                .when()
                .get("/users")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].email", equalTo(user2Email))
                .body("[0].firstName", equalTo("Jane"))
                .body("[0].lastName", equalTo("Smith"));
    }

    @Test
    void givenUserAlreadyInProject_whenGetUsersWithProjectKey_thenReturnsEmptyArray() {
        final String user1Email = "user1@example.com";
        registerUser(user1Email, PASSWORD, "John", "Doe");
        final String token = login(user1Email, PASSWORD);

        createProject(token);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .queryParam("email", user1Email)
                .queryParam("projectKey", "XIRA")
                .when()
                .get("/users")
                .then()
                .statusCode(200)
                .body("$", hasSize(0));
    }

    @Test
    void givenProjectKeyOnly_whenGetUsers_thenReturnsUsersNotInProject() {
        final String user1Email = "user1@example.com";
        final String user2Email = "user2@example.com";
        registerUser(user1Email, PASSWORD, "John", "Doe");
        registerUser(user2Email, PASSWORD, "Jane", "Smith");
        final String token = login(user1Email, PASSWORD);

        createProject(token);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .queryParam("projectKey", "XIRA")
                .when()
                .get("/users")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].email", equalTo(user2Email));
    }

    @Test
    void givenNonExistentEmailAndValidProjectKey_whenGetUsers_thenReturnsEmptyArray() {
        final String user1Email = "user1@example.com";
        registerUser(user1Email, PASSWORD, "John", "Doe");
        final String token = login(user1Email, PASSWORD);

        createProject(token);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .queryParam("email", "nonexistent@example.com")
                .queryParam("projectKey", "XIRA")
                .when()
                .get("/users")
                .then()
                .statusCode(200)
                .body("$", hasSize(0));
    }

    @Test
    void givenValidEmailAndValidProjectKey_whenGetUsers_thenReturnsMatchingUserNotInProject() {
        final String user1Email = "user1@example.com";
        final String user2Email = "user2@example.com";
        registerUser(user1Email, PASSWORD, "John", "Doe");
        registerUser(user2Email, PASSWORD, "Jane", "Smith");
        final String token = login(user1Email, PASSWORD);

        createProject(token);

        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .queryParam("email", user2Email)
                .queryParam("projectKey", "XIRA")
                .when()
                .get("/users")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].email", equalTo(user2Email));
    }

}
