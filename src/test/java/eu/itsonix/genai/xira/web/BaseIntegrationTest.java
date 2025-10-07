package eu.itsonix.genai.xira.web;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import eu.itsonix.genai.xira.jpa.repository.*;
import eu.itsonix.genai.xira.web.model.*;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class BaseIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void registerDatasourceProperties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private BoardColumnWorkflowStatusRepository boardColumnWorkflowStatusRepository;

    @Autowired
    private BoardColumnRepository boardColumnRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private SprintIssueRepository sprintIssueRepository;

    @Autowired
    private SprintRepository sprintRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private XiraUserRepository xiraUserRepository;

    @Autowired
    private IssueAssigneeRepository issueAssigneeRepository;

    @Autowired
    private IssueCommentRepository issueCommentRepository;

    @Autowired
    private IssueRepository issueRepository;

    @BeforeEach
    void configureRestAssured() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        issueCommentRepository.deleteAll();
        issueAssigneeRepository.deleteAll();
        issueRepository.deleteAll();
        sprintIssueRepository.deleteAll();
        sprintRepository.deleteAll();
        boardColumnWorkflowStatusRepository.deleteAll();
        boardColumnRepository.deleteAll();
        boardRepository.deleteAll();
        projectMemberRepository.deleteAll();
        projectRepository.deleteAll();
        xiraUserRepository.deleteAll();
    }

    protected void registerUser(final String email, final String password, final String firstName,
            final String lastName) {
        given().contentType(ContentType.JSON)
                .body(new RegisterRequest().email(email).password(password).firstName(firstName).lastName(lastName))
                .when()
                .post("/auth/register")
                .then()
                .statusCode(201);
    }

    protected String login(final String email, final String password) {
        return given().contentType(ContentType.JSON)
                .body(new LoginRequest().email(email).password(password))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("access_token");
    }

    protected void createProject(final String token) {
        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new CreateProjectRequest().key("XIRA").name("Xira Project"))
                .when()
                .post("/projects")
                .then()
                .statusCode(201);
    }

    protected String getUserIdByEmail(final String token, final String email) {
        return given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .queryParam("email", email)
                .when()
                .get("/users")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("[0].id");
    }

    protected void addProjectMember(final String token, final String userId, final ProjectMemberRole role) {
        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new AddProjectMemberRequest().userId(UUID.fromString(userId)).role(role))
                .when()
                .post("/projects/{key}/members", "XIRA")
                .then()
                .statusCode(201);
    }

    protected void createBoard(final String token) {
        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .body(new AddBoardRequest().name("Development Board").type(AddBoardRequest.TypeEnum.KANBAN))
                .when()
                .post("/projects/{key}/boards", "XIRA")
                .then()
                .statusCode(201);
    }
}
