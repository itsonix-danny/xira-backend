# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Xira

This repository contains the backend code for the project Xira, a Jira-like issue tracker and project management system
for software development teams.

## Commands

```bash
mvn clean install              # Build with all tests
mvn clean install -DskipTests  # Build without tests
mvn test                       # Run unit tests only
mvn verify                     # Run all tests including integration
mvn test -Dtest=TestClassName  # Run specific test
```

## IMPORTANT RULES

- Always test and verify your changes before finishing a task
- Use unit tests, integration tests, and manual testing via curl against the locally-running backend
- A task is NOT COMPLETE until it is both successfully tested and verified
- Use the spring-tech-research-analyst subagent for technical research (e.g., dependency updates or framework research)

## Architecture

Spring Boot 3.5.5 (Java 25) with layered architecture:

- **Controllers** (`web`): REST endpoints from OpenAPI spec
- **Services** (`service`): Business logic and orchestration
- **Data Layer** (`jpa`): Entities and Spring Data repositories

### API principles

- **OpenAPI server contract**: The API contract is defined in `src/main/resources/openapi.yaml`
- **Auto-generated APIs and DTOs**: The general APIs and objects defined in the OpenAPI specification are auto-generated
  with the Maven plugin `openapi-generator-maven-plugin` as defined in the `pom.xml`.

### Database Entities

**Core Entities:**

- **XiraUser**: User account with email, name, and password hash
- **Project**: Project with unique key, name, and lead user (owner). Each project has exactly one workflow
  automatically created with three default statuses (To Do, In Progress, Done)
- **Board**: Project board with name, board number (unique per project), and type (SCRUM or KANBAN). Each board
  automatically gets three default columns (To Do, In Progress, Done) matching the project's workflow statuses
- **BoardColumn**: Column in a board with name and order
- **Issue**: Task/bug/story with project reference, sequence number, unique key, type, status, priority, reporter,
  optional assignee, title, and description
- **Sprint**: Time-boxed iteration with project reference, name, optional start/end dates, and state
- **IssueComment**: Comment on an issue with author reference and content
- **Workflow**: Workflow configuration belonging to a project. Created automatically when project is created with
  three statuses: "To Do" (TODO), "In Progress" (IN_PROGRESS), and "Done" (DONE)
- **WorkflowStatus**: Status within a workflow with name, category, and order

**Join Tables:**

- **ProjectMember**: Links users to projects with roles (composite key: projectId + userId)
- **SprintIssue**: Links issues to sprints with timestamp (composite key: sprintId + issueId)
- **BoardColumnWorkflowStatus**: Links board columns to workflow statuses with isDefault flag (composite key:
  boardColumnId + workflowStatusId)

**Enums:**

- **BoardType**: Type of board (SCRUM or KANBAN)
- **IssueType**: Type of issue (e.g., Task, Bug, Story)
- **IssuePriority**: Priority level for issues
- **ProjectRole**: Role of a user in a project
- **SprintState**: State of a sprint (e.g., Planning, Active, Completed)
- **WorkflowStatusCategory**: Category of workflow status (e.g., Todo, InProgress, Done)

**Key Relationships:**

- **Project → XiraUser** (ManyToOne): Project owner/lead
- **Workflow → Project** (ManyToOne): Workflow belongs to project (one workflow per project, created automatically)
- **Board → Project** (ManyToOne): Board belongs to project
- **BoardColumn → Board** (ManyToOne): Column belongs to board
- **BoardColumnWorkflowStatus → BoardColumn** (ManyToOne): Links column to workflow status
- **BoardColumnWorkflowStatus → WorkflowStatus** (ManyToOne): Links column to workflow status
- **Issue → Project** (ManyToOne): Issue belongs to project
- **Issue → WorkflowStatus** (ManyToOne): Current status of issue
- **Issue → XiraUser** (ManyToOne): Reporter (required) and Assignee (optional)
- **Sprint → Project** (ManyToOne): Sprint belongs to project
- **IssueComment → Issue** (ManyToOne): Comment on issue
- **IssueComment → XiraUser** (ManyToOne): Comment author
- **WorkflowStatus → Workflow** (ManyToOne): Status within workflow
- **ProjectMember → Project + XiraUser** (ManyToOne, composite): User membership in project with role
- **SprintIssue → Sprint + Issue** (ManyToOne, composite): Issue assigned to sprint with timestamp

## Coding Patterns

### Core Principles

- **Tests**: Always test new code with unit tests and integration tests (see more details below in 'Testing Approach')
- **Clean Code**: Write clean, readable code with good naming and comments
- **Consistency**: Always follow the same coding style and patterns you can see in similar classes in this project
  (e.g., other services, entities, controllers, mappers, tests, etc.)
- **Lean Code**: Simplicity first, complexity only when necessary
- **Minimal database access**: Avoid multiple repository queries if a single one is enough (e.g., instead of
  `project = projectRepository.findByKey(key)` and `workflowStatusRepository.findByWorkflow(project.getWorkflow())`, use
  `workflowRepository.findByWorkflowProjectKey(key)`)
- **Modularization**: Keep methods and classes small and focused, use helper methods and util classes where necessary
- **No Comments**: Write self-explanatory code with meaningful names instead of adding comments
- **Functional Style**: Always use streams, Optional, and lambdas over loops and null checks
- **Method References**: Prefer `this::methodName` over lambdas
- **Immutability**: Use `final` for all variables where possible
- **Builder Pattern**: Consistently use builder patterns for entities and dtos
- **Avoid var**: Avoid var outside of tests

### Key Development Patterns

- **Query Derivation**: Use Spring Data JPA method naming (e.g., `findByProjectOwnerEmail(final String email)`) and
  avoid `@Query` unless absolutely necessary
- **Avoid (N+1)-Problem**: When related entities are needed, use `@EntityGraph` on repository methods (e.g.,
  `@EntityGraph(attributePaths = { "project", "project.owner" })` for `findAllByUserId(final String userId)`)
- **Entity Relationships & Deep Fetching**: Add `@OneToMany` relationships to enable deep fetching with `@EntityGraph`.
  Use `Set` instead of `List` to avoid `MultipleBagFetchException` when fetching multiple collections simultaneously
- **Clean Mapper Inputs**: Mappers must ONLY accept entities (when they map to DTOs) or DTOs (when they map to
  entities), never mixed entity/DTO parameters. Mappers perform all nested conversions internally
- **Service Layer Pattern**: Services query entities and prepare data structures (grouping, filtering), then pass
  entities to mappers. Pattern: `query → prepare → map → return` (see `BoardService#getScrumBoardDetails` as an example)
- **Transactional Boundaries**: Mark read-only operations with `@Transactional(readOnly = true)`
- **Service Layer**: All business logic in services, controllers only handle HTTP concerns
- **Error Handling**: Log errors with context, throw domain-specific exceptions
- **DTO Mapping**: Consistent static mapping methods in mapping classes under
  `src/main/java/eu/itsonix/genai/xira/mapper` for entity-to-API model conversion
- **Economical use of variables**: Avoid introducing variables that are only used once. Avoid creating multiple
  instances of the same entity when saving (e.g., `final Sprint savedSprint = sprintRepository.save(sprint);`)

## Testing

See the [`backend-testing` skill](.claude/skills/backend-testing/SKILL.md) for the full testing guide — principles,
layout, REST Assured / Mockito / TestContainers patterns, the `BaseIntegrationTest` helper inventory, edge-case
templates, and common pitfalls. Load this skill automatically when working on tests.