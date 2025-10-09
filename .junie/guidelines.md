# Guidelines

You are debugging expert for the local environment.

## Prerequisites

### Environment

The backend is running on http://localhost:8040 with a local database.

### Tools

You can make requests to the app using `curl` and the endpoints described in src/main/resources/openapi.yml
You can execute queries on this database using the query tool (postgres MCP).

### Requests

Before you can make requests, you must log in (endpoint /auth/login) with email and password to get a Bearer token.
Then you send the Bearer token in the Authorization header of further request.

## Endpoints

### Authentication

- POST /auth/register - Register a new user account
- POST /auth/login - Login with email and password

### Users

- GET /users - Search for users

### Projects

- GET /projects - Get projects of the authenticated user
- POST /projects - Create a new project
- GET /projects/{key} - Get project details
- PATCH /projects/{key} - Update a project
- GET /projects/{key}/members - Get all members of a project
- POST /projects/{key}/members - Add a member to a project
- PATCH /projects/{key}/members/{userId} - Update the role of a project member
- DELETE /projects/{key}/members/{userId} - Remove a member from a project
- GET /projects/{key}/workflows/statuses - Get all workflow statuses of a project

### Boards

- POST /projects/{key}/boards - Add a board to a project
- GET /projects/{key}/boards/{boardNumber} - Get board details
- PATCH /projects/{key}/boards/{boardNumber} - Update a board
- DELETE /projects/{key}/boards/{boardNumber} - Delete a board
- GET /projects/{key}/boards/{boardNumber}/active-sprint - Get active sprint view for a SCRUM board

### Sprints

- POST /projects/{key}/sprints - Add a sprint to a project
- PATCH /projects/{key}/sprints/{sprintId} - Update a sprint
- DELETE /projects/{key}/sprints/{sprintId} - Delete a sprint
- POST /projects/{key}/sprints/{sprintId}/start - Start a sprint
- POST /projects/{key}/sprints/{sprintId}/finish - Finish a sprint
- POST /projects/{key}/sprints/{sprintId}/issues/{issueKey} - Add an issue to a sprint
- DELETE /projects/{key}/sprints/{sprintId}/issues/{issueKey} - Remove an issue from a sprint

### Issues

- GET /issues - Get issues with optional filters
- POST /projects/{key}/issues - Create a new issue for a project
- GET /projects/{key}/issues/{issueKey} - Get issue details
- PATCH /projects/{key}/issues/{issueKey} - Update an existing issue
- POST /projects/{key}/issues/{issueKey}/assignees - Add an assignee to an issue
- DELETE /projects/{key}/issues/{issueKey}/assignees/{userId} - Remove an assignee from an issue
- PATCH /projects/{key}/issues/{issueKey}/status - Set workflow status of an issue
- POST /projects/{key}/issues/{issueKey}/comments - Add a comment to an issue
- PATCH /projects/{key}/issues/{issueKey}/comments/{commentId} - Update a comment
- DELETE /projects/{key}/issues/{issueKey}/comments/{commentId} - Delete a comment

## Database tables

### Core Tables

- **xira_user** - User accounts with email, password, and profile info
- **project** - Projects with unique key, name, and owner
- **board** - Project boards (SCRUM or KANBAN type)
- **board_column** - Columns within boards with name and order
- **issue** - Tasks/bugs/stories with type, status, priority, and assignments
- **sprint** - Time-boxed iterations with name, goal, and state
- **issue_comment** - Comments on issues with author and content
- **workflow** - Workflow configuration for a project
- **workflow_status** - Status within a workflow (e.g., To Do, In Progress, Done)

### Join Tables

- **project_member** - Links users to projects with roles (ADMIN, DEVELOPER)
- **sprint_issue** - Links issues to sprints with timestamp
- **issue_assignee** - Links issues to assigned users
- **board_column_workflow_status** - Links board columns to workflow statuses

