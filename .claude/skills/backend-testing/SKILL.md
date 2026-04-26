---
name: backend-testing
description: Authoritative guide for writing and running tests in the Xira Spring Boot backend. Use whenever Claude is creating, modifying, debugging, or reviewing unit tests, integration tests, or test infrastructure in xira-backend.
---

# Backend testing — the Xira way

## When to use this skill

Whenever you create, modify, debug, or review tests under `xira-backend/src/test/`, including service-level unit tests,
controller-level integration tests, and the shared test infrastructure.

## How to run tests

```bash
mvn test                       # Unit + integration tests (Surefire runs everything in this project)
mvn verify                     # Same — kept for parity; no separate Failsafe phase
mvn test -Dtest=ClassName      # Single class
mvn test -Dtest=ClassName#methodName  # Single method
mvn clean install -DskipTests  # Build without tests
```

A task is **NOT COMPLETE** until the relevant tests have been written, run, and observed green. Run new tests
immediately — do not batch.

## Test layout & naming

```
src/test/java/eu/itsonix/genai/xira/
├── service/   — unit tests (Mockito-only, no Spring context)
│   └── *Test.java                        e.g. IssueServiceTest
└── web/       — integration tests (full Spring context + Postgres)
    ├── BaseIntegrationTest.java          shared base — extend it
    └── *IntegrationTest.java             e.g. IssueControllerIntegrationTest
src/test/resources/
└── application-test.yaml                 ddl-auto=create-drop, show-sql
```

Conventions:

- **One test class per service / controller**, mirroring the production package.
- **Suffix `*Test.java` for unit, `*IntegrationTest.java` for integration**. This project does **not** use the Failsafe
  `*IT.java` convention — Surefire runs everything via `mvn test`.
- **Method names**: `given[Condition]_when[Action]_then[Outcome]` (action segment optional when there's only one action
  under test). Examples: `givenValidRequest_whenCreateIssue_thenCreatesIssueWithDefaultStatus`,
  `givenInvalidProjectKey_whenCreateIssue_thenThrowsEntityNotFoundException`, `givenInvalidCredentials_thenReturns401`.

## Core principles (apply to both unit and integration tests)

- **Run tests immediately after writing them**; never finish a task on un-run tests.
- **Cover edge cases**, not just happy paths — invalid inputs, missing auth, wrong roles, conflicting state.
- **Test observable behavior**, not delegation/internal calls. Don't write tests that just assert "the service called
  the repository."
- **Avoid redundancy**: if two tests cover the same behavior, keep the more comprehensive one.
- **Black-box bias for integration tests**: prefer verifying outcomes through the API (e.g., register then login) over
  reading the database directly.
- **Mirror existing tests** when in doubt — consistency is a hard rule in this project.
- **Style rules from CLAUDE.md still apply**: `final` on locals, builder patterns, no comments, functional style. The
  one carve-out: `var` is permitted in tests.

## Stack & dependencies

| Concern                  | Library                                          | Version (`pom.xml`)            |
|--------------------------|--------------------------------------------------|--------------------------------|
| Test runner / assertions | JUnit Jupiter 5, AssertJ, Hamcrest, Mockito      | via `spring-boot-starter-test` |
| HTTP DSL                 | REST Assured                                     | 5.5.2                          |
| Containers               | Testcontainers core + junit-jupiter + postgresql | 1.19.7                         |
| Database (test)          | `postgres:16-alpine` (TestContainers image)      | —                              |

No JaCoCo, no separate Failsafe profile, no `@ActiveProfiles("test")` — the test datasource is wired entirely via
`@DynamicPropertySource` on `BaseIntegrationTest`, and `application-test.yaml` is loaded by Spring's standard test
resource lookup.

## Unit tests (`service/*Test.java`)

Each service test class mocks every collaborator with `@Mock` and injects them with `@InjectMocks`. No Spring context.

```java

@ExtendWith(MockitoExtension.class)
class IssueServiceTest {

    @Mock
    private IssueRepository issueRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private WorkflowStatusRepository workflowStatusRepository;
    @Mock
    private AuthService authService;
    // ...other repositories...

    @InjectMocks
    private IssueService issueService;
}
```

### Canonical happy-path template

From `IssueServiceTest#givenValidRequest_whenCreateIssue_thenCreatesIssueWithDefaultStatus` (lines 59–89). It is the
cleanest example of the "build a graph of related entities, stub the repositories, exercise the service, assert
outcome + verify save" pattern.

```java

@Test
void givenValidRequest_whenCreateIssue_thenCreatesIssueWithDefaultStatus() {
    final String projectKey = "XIRA";
    final String projectId = "project-id";
    final String userId = "user-id";

    final Project project = Project.builder().id(projectId).key(projectKey).name("Xira").build();
    final XiraUser reporter = XiraUser.builder().id(userId).email("user@example.com").build();
    final WorkflowStatus defaultStatus = WorkflowStatus.builder()
            .id("550e8400-e29b-41d4-a716-446655440100")
            .name("To Do")
            .category(WorkflowStatusCategory.TODO)
            .statusOrder(1)
            .build();

    final CreateIssueRequest request = new CreateIssueRequest().title("Test Issue")
            .description("Test Description")
            .issueType(CreateIssueRequest.IssueTypeEnum.BUG)
            .priority(CreateIssueRequest.PriorityEnum.HIGH);

    when(projectRepository.findByKeyIgnoreCase(projectKey)).thenReturn(Optional.of(project));
    when(authService.getAuthenticatedUser()).thenReturn(reporter);
    when(issueRepository.findMaxSeqNoByProjectId(projectId)).thenReturn(0);
    when(workflowStatusRepository.findFirstByWorkflowProjectIdOrderByStatusOrderAsc(projectId))
            .thenReturn(Optional.of(defaultStatus));

    final String result = issueService.createIssue(projectKey, request);

    assertThat(result).isEqualTo("XIRA-1");
    verify(issueRepository).save(any(Issue.class));
}
```

What to copy from this template:

- **Lombok `@Builder` on entities**: every entity (`Project`, `XiraUser`, `Issue`, `WorkflowStatus`, `Sprint`, `Board`,
  `BoardColumn`, `IssueComment`, `ProjectMember`, etc.) uses Lombok `@Builder`. Build them inline; there are no separate
  `*TestDataBuilder` classes.
- **OpenAPI DTOs use the fluent setter chain**: `new CreateIssueRequest().title("...").issueType(...).priority(...)` —
  never `new` + setters on separate lines.
- **Stub the standard collaborators**: `projectRepository.findByKeyIgnoreCase(...)`,
  `authService.getAuthenticatedUser()`, derived-name finders (`findMaxSeqNoByProjectId`,
  `findFirstByWorkflowProjectIdOrderByStatusOrderAsc`, etc.). Names match production exactly; do not invent.
- **Assertion mix**: AssertJ `assertThat(...).isEqualTo(...)` for return values; `verify(repo).save(any(Issue.class))`
  for persistence side-effects.

### Negative-path template

From `IssueServiceTest#givenInvalidProjectKey_whenCreateIssue_thenThrowsEntityNotFoundException` (lines 151–163). Use
`assertThatThrownBy` for exceptions — it composes type + message in one fluent chain.

```java

@Test
void givenInvalidProjectKey_whenCreateIssue_thenThrowsEntityNotFoundException() {
    final String projectKey = "INVALID";
    final CreateIssueRequest request = new CreateIssueRequest().title("Test Issue")
            .issueType(CreateIssueRequest.IssueTypeEnum.BUG)
            .priority(CreateIssueRequest.PriorityEnum.HIGH);

    when(projectRepository.findByKeyIgnoreCase(projectKey)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> issueService.createIssue(projectKey, request))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessage("Project not found");
}
```

### State-variation template

From `IssueServiceTest#givenExistingIssues_whenCreateIssue_thenIncrementsSeqNo` (lines 91–119). When a service's
behavior depends on a single derived value (here, the max sequence number), vary that one mock return between tests
rather than rebuilding the whole graph.

```java
when(issueRepository.findMaxSeqNoByProjectId(projectId)).

thenReturn(5);

// ...same call as happy path...
assertThat(result).

isEqualTo("XIRA-6");
```

### `ArgumentCaptor` template (use when field-level assertions matter)

From `AuthServiceTest#givenValidRegisterRequest_thenCreatesNewUser` (lines 47–68). Capture the entity passed to `save()`
and inspect each field.

```java
final ArgumentCaptor<XiraUser> userCaptor = ArgumentCaptor.forClass(XiraUser.class);

verify(xiraUserRepository).

save(userCaptor.capture());

final XiraUser savedUser = userCaptor.getValue();

assertEquals("test@example.com",savedUser.getEmail());

assertEquals("encodedPassword",savedUser.getPasswordHash());

verify(xiraUserRepository, never()).

save(any(XiraUser.class));  // for negative-call cases
```

### When to skip a unit test

Unit tests are **only required for non-trivial public service methods**. A method that delegates to a single repository
call (e.g., `WorkflowService#getWorkflowStatuses`) does not need a unit test — the integration test covers it.
`WorkflowServiceTest` is intentionally tiny for this reason.

## Integration tests (`web/*IntegrationTest.java`)

Every integration test class **must** extend `BaseIntegrationTest`. The base class owns:

- the static `PostgreSQLContainer<>("postgres:16-alpine")` (started once, reused across the suite)
- `@DynamicPropertySource` injection of `spring.datasource.url/username/password`
- `@LocalServerPort` capture and REST Assured wiring (`baseURI`, `port`)
- the `@BeforeEach` repository-cascade cleanup (FK-safe order)
- helper methods for the common preconditions (register, login, create project, etc.)

Do **not** re-create any of this in subclasses. Do **not** add `@SpringBootTest`, `@DynamicPropertySource`, or
`RestAssured.baseURI/port` reassignment.

### `BaseIntegrationTest` helper inventory

| Helper                                                      | Purpose                                                                                                                   |
|-------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------|
| `registerUser(email, password, firstName, lastName)`        | POST `/auth/register`, expects 201.                                                                                       |
| `login(email, password) → String`                           | POST `/auth/login`, returns the JWT `access_token`.                                                                       |
| `bearer(token) → String`                                    | Formats `"Bearer <token>"` for the `Authorization` header.                                                                |
| `createProject(token)` / `createProject(token, key, name)`  | Default key/name `"XIRA"`/`"Xira Project"`.                                                                               |
| `addProjectMember(token, userId, role)`                     | POST to add a member with a `ProjectMemberRole`. Project key hardcoded to `"XIRA"`.                                       |
| `createBoard(token)`                                        | Creates a Kanban "Development Board" on project `"XIRA"`.                                                                 |
| `createIssue(token, projectKey, title, issueType) → String` | Creates issue with `HIGH` priority, returns issue id (parsed from `Location`).                                            |
| `getUserIdByEmail(token, email) → String`                   | Looks up a user id via `GET /users?email=...`.                                                                            |
| `getWorkflowStatusId(token, statusName) → String`           | Resolves a workflow status id by display name on project `"XIRA"`.                                                        |
| `extractIdFromLocation(location) → String`                  | Pulls the trailing path segment from a `Location` header.                                                                 |
| `prepareProjectOwner(email, password) → String`             | Composite: register + login + create project. Use this to skip the three-line preamble when the test only needs an owner. |

### Database isolation

`BaseIntegrationTest#configureRestAssured` runs `deleteAll()` on every repository in **FK-safe order** before each test:

```java
issueCommentRepository.deleteAll();
issueAssigneeRepository.

deleteAll();
sprintIssueRepository.

deleteAll();
issueRepository.

deleteAll();
sprintRepository.

deleteAll();
boardColumnWorkflowStatusRepository.

deleteAll();
boardColumnRepository.

deleteAll();
boardRepository.

deleteAll();
projectMemberRepository.

deleteAll();
projectRepository.

deleteAll();
xiraUserRepository.

deleteAll();
```

**Cleanup is explicit, not transactional** — there is no rollback. When you add a new entity with foreign keys, you *
*must** add a `deleteAll()` call here in the correct position (children before parents).

Do not put `@Transactional` on a test class — it will not roll back; it will just hide bugs.

### Canonical happy-path template

From `IssueControllerIntegrationTest#givenValidRequest_whenCreateIssue_thenReturns201WithLocation` (lines 16–35).
Three-line preamble (register → login → create project), one DTO build, one fluent REST Assured chain.

```java

@Test
void givenValidRequest_whenCreateIssue_thenReturns201WithLocation() {
    registerUser("user@example.com", "password", "John", "Doe");
    final String token = login("user@example.com", "password");
    createProject(token);

    final CreateIssueRequest request = new CreateIssueRequest().title("Test Issue")
            .description("Test Description")
            .issueType(CreateIssueRequest.IssueTypeEnum.BUG)
            .priority(CreateIssueRequest.PriorityEnum.HIGH);

    given().contentType(ContentType.JSON)
            .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
            .body(request)
            .when()
            .post("/projects/{key}/issues", "XIRA")
            .then()
            .statusCode(201)
            .header(HttpHeaders.LOCATION, matchesPattern(".*/projects/XIRA/issues/XIRA-1"));
}
```

What to copy:

- `ContentType.JSON`, **never the literal `"application/json"`**.
- `body(dto)` — pass the DTO object, **never `Map`, never a JSON string** (except the deliberate `body("{}")` for "
  missing required fields" tests, see below).
- Path templating: `post("/projects/{key}/issues", "XIRA")` — REST Assured fills `{...}` placeholders from positional
  args.
- `Location` header verified with Hamcrest `matchesPattern(...)` — keys are deterministic (`XIRA-1`, `XIRA-2`, ...).

### Role-based access template

From `IssueControllerIntegrationTest#givenDeveloper_whenCreateIssue_thenReturns201` (lines 110–132). The "owner adds a
developer, developer takes the action" pattern — uses `getUserIdByEmail` + `addProjectMember`.

```java

@Test
void givenDeveloper_whenCreateIssue_thenReturns201() {
    registerUser("admin@example.com", "password", "Admin", "User");
    final String adminToken = login("admin@example.com", "password");
    createProject(adminToken);

    registerUser("developer@example.com", "password", "Dev", "User");
    final String developerToken = login("developer@example.com", "password");
    final String developerId = getUserIdByEmail(adminToken, "developer@example.com");
    addProjectMember(adminToken, developerId, ProjectMemberRole.DEVELOPER);

    final CreateIssueRequest request = new CreateIssueRequest().title("Test Issue")
            .issueType(CreateIssueRequest.IssueTypeEnum.TASK)
            .priority(CreateIssueRequest.PriorityEnum.MEDIUM);

    given().contentType(ContentType.JSON)
            .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", developerToken))
            .body(request)
            .when()
            .post("/projects/{key}/issues", "XIRA")
            .then()
            .statusCode(201);
}
```

### Sequence / multi-step template

From `IssueControllerIntegrationTest#givenMultipleIssues_whenCreateIssue_thenIncrementsSeqNo` (lines 37–67): two POSTs,
second one's `Location` asserted via `matchesPattern(".*/projects/XIRA/issues/XIRA-2")`. Use this pattern whenever a
test needs to verify server-side sequencing or accumulation.

### REST Assured cheat sheet

```java
// Request
given()
    .

contentType(ContentType.JSON)
    .

header(HttpHeaders.AUTHORIZATION, bearer(token))   // bearer() helper from BaseIntegrationTest
        .

queryParam("email","user@example.com")
    .

body(dto)
.

when()
    .

post("/projects/{key}/issues","XIRA")             // path templating
.

then()
    .

statusCode(201)
    .

header(HttpHeaders.LOCATION, matchesPattern(".*/XIRA-1"))
        .

body("status",equalTo(409))                       // JsonPath + Hamcrest
        .

body("detail",containsString("already exists"))
        .

body("$",hasSize(1));                             // root array size

// Extract
final TokenResponse tokens = given().body(loginRequest)...

post("/auth/login")
    .

then().

statusCode(200)
    .

extract().

as(TokenResponse .class);                 // deserialize body

final String accessToken = ...
        .

extract().

jsonPath().

getString("access_token");    // pull a single field

final String location = ...
        .

extract().

header(HttpHeaders.LOCATION);            // then extractIdFromLocation(location)
```

## Edge cases — what to cover for every endpoint

| Status | Cover when...                                                               | Example                                                                                         |
|--------|-----------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------|
| 400    | Required fields missing, invalid format, validation constraint violation    | `body("{}")` (literal JSON for "missing fields"); lowercase project key; bad email format       |
| 401    | No `Authorization` header; wrong password; expired token                    | `givenInvalidCredentials_thenReturns401`, `givenUnauthenticated_whenCreateIssue_thenReturns401` |
| 403    | Authenticated user is not a project member, or member has insufficient role | `givenNonMember_whenCreateIssue_thenReturns403`                                                 |
| 404    | Unknown project key, unknown issue key, unknown user id                     | (mirror existing 404 tests in the same controller)                                              |
| 409    | Duplicate email, duplicate project key, conflicting state transition        | `givenExistingEmail_thenReturns409`                                                             |

The two canonical permission/auth templates:

```java

@Test
void givenNonMember_whenCreateIssue_thenReturns403() {
    registerUser("owner@example.com", "password", "Owner", "User");
    final String ownerToken = login("owner@example.com", "password");
    createProject(ownerToken);

    registerUser("nonmember@example.com", "password", "Non", "Member");
    final String nonMemberToken = login("nonmember@example.com", "password");

    final CreateIssueRequest request = new CreateIssueRequest().title("Test Issue")
            .issueType(CreateIssueRequest.IssueTypeEnum.BUG)
            .priority(CreateIssueRequest.PriorityEnum.HIGH);

    given().contentType(ContentType.JSON)
            .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", nonMemberToken))
            .body(request)
            .when()
            .post("/projects/{key}/issues", "XIRA")
            .then()
            .statusCode(403);
}

@Test
void givenUnauthenticated_whenCreateIssue_thenReturns401() {
    final CreateIssueRequest request = new CreateIssueRequest().title("Test Issue")
            .issueType(CreateIssueRequest.IssueTypeEnum.BUG)
            .priority(CreateIssueRequest.PriorityEnum.HIGH);

    given().contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/projects/{key}/issues", "XIRA")
            .then()
            .statusCode(401);
}
```

The two canonical input-validation templates (from `AuthControllerIntegrationTest`):

```java

@Test
void givenMissingRequiredFields_thenReturns400() {
    given().contentType(ContentType.JSON).body("{}").when().post("/auth/register").then().statusCode(400);
}

@Test
void givenInvalidCredentials_thenReturns401() {
    final XiraUser user = XiraUser.builder()
            .email("user@example.com")
            .firstName("Test").lastName("User")
            .passwordHash(passwordEncoder.encode("correctPassword"))
            .build();
    xiraUserRepository.save(user);

    final LoginRequest loginRequest = new LoginRequest().email("user@example.com").password("wrongPassword");
    given().contentType(ContentType.JSON).body(loginRequest).when().post("/auth/login").then().statusCode(401);
}
```

`body("{}")` is the **only** time a JSON string literal is acceptable — it deliberately bypasses the DTO to send a
payload that the DTO builder would refuse to construct.

## Common pitfalls

- **Forgetting to extend `BaseIntegrationTest`** → no Postgres, no port wiring, no cleanup. Always extend it.
- **Forgetting to add a new repository to the `@BeforeEach` `deleteAll()` chain** → flaky cross-test contamination,
  often invisible until a test runs second in a suite. New entity types and new join tables both need an entry, in
  FK-safe order (children before parents).
- **Adding a new `@OneToMany` collection without considering FK ordering** in the cleanup chain.
- **Using `@Transactional` on a test class** — this project does **not** roll back; it deletes. `@Transactional` will
  silently mask bugs.
- **Reassigning `RestAssured.baseURI`/`port` in a subclass** — `BaseIntegrationTest` already does this with the random
  port; overriding will cause flaky port conflicts.
- **Hardcoding `"application/json"`** instead of `ContentType.JSON`.
- **Sending `Map` or JSON string bodies** instead of OpenAPI-generated DTOs (except the deliberate `body("{}")` 400
  case).
- **Asserting on the database** when an API call would do — keep integration tests black-box where possible.
- **Forgetting `final` on local variables** — the formatter hook does not auto-fix this.
- **Reaching for `@MockBean` in integration tests** — this project doesn't use it; integration tests use the real Spring
  stack against the real Postgres.
- **Inventing repository derived-method names in unit-test mocks** — names must match production exactly (e.g.,
  `findFirstByWorkflowProjectIdOrderByStatusOrderAsc`, not a paraphrase).

## Game plan when adding a new endpoint

1. Add the unit test(s) in `service/<Service>Test.java`: build the entity graph with Lombok builders, stub each
   collaborator, exercise the service, assert outcome + `verify(repo).save(...)`.
2. Add the integration test(s) in `web/<Controller>IntegrationTest.java` extending `BaseIntegrationTest`: register →
   login → set up project state via the helpers → fire one REST Assured chain → assert status + body/header.
3. Cover the happy path as well as the relevant edge-case rows from the table above.
4. If the endpoint introduces a new entity or join table, append a `deleteAll()` call to
   `BaseIntegrationTest#configureRestAssured` in FK-safe order.
5. `mvn test` — don't finish the task until it's green.
