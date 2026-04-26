package eu.itsonix.genai.xira.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import eu.itsonix.genai.xira.jpa.entity.XiraUser;
import eu.itsonix.genai.xira.jpa.repository.XiraUserRepository;
import eu.itsonix.genai.xira.web.model.LoginRequest;
import eu.itsonix.genai.xira.web.model.RegisterRequest;
import eu.itsonix.genai.xira.web.model.TokenResponse;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

class AuthControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private XiraUserRepository xiraUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void givenValidRegisterRequest_thenReturns201() {
        final RegisterRequest registerRequest = new RegisterRequest().email("newuser@example.com")
                .password("password123")
                .firstName("John")
                .lastName("Doe");

        given().contentType(ContentType.JSON)
                .body(registerRequest)
                .when()
                .post("/auth/register")
                .then()
                .statusCode(201);

        final LoginRequest loginRequest = new LoginRequest().email(registerRequest.getEmail())
                .password(registerRequest.getPassword());

        final TokenResponse tokenResponse = given().contentType(ContentType.JSON)
                .body(loginRequest)
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .as(TokenResponse.class);

        assertThat(tokenResponse.getAccessToken()).isNotNull();
        assertThat(tokenResponse.getAccessToken()).isNotEmpty();
    }

    @Test
    void givenExistingEmail_thenReturns409() {
        final XiraUser xiraUser = XiraUser.builder()
                .email("existing@example.com")
                .firstName("Jane")
                .lastName("Smith")
                .passwordHash(passwordEncoder.encode("password"))
                .build();

        xiraUserRepository.save(xiraUser);

        final RegisterRequest registerRequest = new RegisterRequest().email(xiraUser.getEmail())
                .password("newpassword")
                .firstName("Another")
                .lastName("User");

        given().contentType(ContentType.JSON)
                .body(registerRequest)
                .when()
                .post("/auth/register")
                .then()
                .statusCode(409)
                .body("status", equalTo(409))
                .body("detail", containsString("User with email existing@example.com already exists"));
    }

    @Test
    void givenMissingRequiredFields_thenReturns400() {
        given().contentType(ContentType.JSON).body("{}").when().post("/auth/register").then().statusCode(400);
    }

    @Test
    void givenInvalidCredentials_thenReturns401() {
        final XiraUser user = XiraUser.builder()
                .email("user@example.com")
                .firstName("Test")
                .lastName("User")
                .passwordHash(passwordEncoder.encode("correctPassword"))
                .build();
        xiraUserRepository.save(user);

        final LoginRequest loginRequest = new LoginRequest().email("user@example.com").password("wrongPassword");

        given().contentType(ContentType.JSON).body(loginRequest).when().post("/auth/login").then().statusCode(401);
    }

    @Test
    void givenNonExistentUser_thenReturns401() {
        final LoginRequest loginRequest = new LoginRequest().email("nonexistent@example.com").password("anyPassword");

        given().contentType(ContentType.JSON).body(loginRequest).when().post("/auth/login").then().statusCode(401);
    }

    @Test
    void givenPasswordTooShort_whenRegister_thenReturnsBadRequest() {
        final RegisterRequest registerRequest = new RegisterRequest().email("newuser@example.com")
                .password("short")
                .firstName("John")
                .lastName("Doe");

        given().contentType(ContentType.JSON)
                .body(registerRequest)
                .when()
                .post("/auth/register")
                .then()
                .statusCode(400);
    }

    @Test
    void givenInvalidEmailFormat_whenRegister_thenReturnsBadRequest() {
        final RegisterRequest registerRequest = new RegisterRequest().email("notanemail")
                .password("password123")
                .firstName("John")
                .lastName("Doe");

        given().contentType(ContentType.JSON)
                .body(registerRequest)
                .when()
                .post("/auth/register")
                .then()
                .statusCode(400);
    }

    @Test
    void givenEmptyFirstName_whenRegister_thenReturnsBadRequest() {
        final RegisterRequest registerRequest = new RegisterRequest().email("newuser@example.com")
                .password("password123")
                .firstName("")
                .lastName("Doe");

        given().contentType(ContentType.JSON)
                .body(registerRequest)
                .when()
                .post("/auth/register")
                .then()
                .statusCode(400);
    }

    @Test
    void givenEmptyLastName_whenRegister_thenReturnsBadRequest() {
        final RegisterRequest registerRequest = new RegisterRequest().email("newuser@example.com")
                .password("password123")
                .firstName("John")
                .lastName("");

        given().contentType(ContentType.JSON)
                .body(registerRequest)
                .when()
                .post("/auth/register")
                .then()
                .statusCode(400);
    }

    @Test
    void givenMissingEmail_whenLogin_thenReturnsBadRequest() {
        final LoginRequest loginRequest = new LoginRequest().password("password123");

        given().contentType(ContentType.JSON).body(loginRequest).when().post("/auth/login").then().statusCode(400);
    }

    @Test
    void givenMissingPassword_whenLogin_thenReturnsBadRequest() {
        final LoginRequest loginRequest = new LoginRequest().email("user@example.com");

        given().contentType(ContentType.JSON).body(loginRequest).when().post("/auth/login").then().statusCode(400);
    }
}
