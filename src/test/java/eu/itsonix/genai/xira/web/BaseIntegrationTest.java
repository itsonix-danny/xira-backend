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
        sprintIssueRepository.deleteAll();
        issueRepository.deleteAll();
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
        createProject(token, "XIRA", "Xira Project");
    }

    protected void createProject(final String token, final String projectKey, final String projectName) {
        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .body(new CreateProjectRequest().key(projectKey).name(projectName))
                .when()
                .post("/projects")
                .then()
                .statusCode(201);
    }

    protected String bearer(final String token) {
        return String.format("Bearer %s", token);
    }

    protected String extractIdFromLocation(final String location) {
        return location.substring(location.lastIndexOf('/') + 1);
    }

    protected String getUserIdByEmail(final String token, final String email) {
        return given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
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
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .body(new AddProjectMemberRequest().userId(UUID.fromString(userId)).role(role))
                .when()
                .post("/projects/{key}/members", "XIRA")
                .then()
                .statusCode(201);
    }

    protected void createBoard(final String token) {
        given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .body(new AddBoardRequest().name("Development Board").type(AddBoardRequest.TypeEnum.KANBAN))
                .when()
                .post("/projects/{key}/boards", "XIRA")
                .then()
                .statusCode(201);
    }

    protected String getWorkflowStatusId(final String token, final String statusName) {
        final WorkflowStatusResponse[] statuses = given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .when()
                .get("/projects/{key}/workflows/statuses", "XIRA")
                .then()
                .extract()
                .as(WorkflowStatusResponse[].class);

        for (final WorkflowStatusResponse status : statuses) {
            if (status.getName().equals(statusName)) {
                return status.getId().toString();
            }
        }
        throw new IllegalStateException("Status not found: " + statusName);
    }

    protected String createIssue(final String token, final String projectKey, final String title,
            final CreateIssueRequest.IssueTypeEnum issueType) {
        final CreateIssueRequest request = new CreateIssueRequest().title(title)
                .issueType(issueType)
                .priority(CreateIssueRequest.PriorityEnum.HIGH);

        final String location = given().contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .body(request)
                .post("/projects/{key}/issues", projectKey)
                .then()
                .extract()
                .header(HttpHeaders.LOCATION);

        return extractIdFromLocation(location);
    }

    protected String prepareProjectOwner(final String email, final String password) {
        registerUser(email, password, "Owner", "User");
        final String token = login(email, password);
        createProject(token);
        return token;
    }
}
