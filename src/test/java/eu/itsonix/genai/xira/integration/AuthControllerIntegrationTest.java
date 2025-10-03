package eu.itsonix.genai.xira.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import eu.itsonix.genai.xira.jpa.entity.XiraUser;
import eu.itsonix.genai.xira.jpa.repository.XiraUserRepository;
import eu.itsonix.genai.xira.web.model.LoginRequest;
import eu.itsonix.genai.xira.web.model.RegisterRequest;
import eu.itsonix.genai.xira.web.model.TokenResponse;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AuthControllerIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:15");

    @Autowired
    private XiraUserRepository xiraUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @DynamicPropertySource
    static void postgresProperties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
    }

    @LocalServerPort
    void serverPort(final int port) {
        RestAssured.port = port;
    }

    @BeforeEach
    void setup() {
        xiraUserRepository.deleteAll();
    }

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
}
