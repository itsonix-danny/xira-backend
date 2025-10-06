package eu.itsonix.genai.xira.web;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import eu.itsonix.genai.xira.service.ProjectService;
import eu.itsonix.genai.xira.service.WorkflowService;
import eu.itsonix.genai.xira.web.api.ProjectsApi;
import eu.itsonix.genai.xira.web.model.AddProjectMemberRequest;
import eu.itsonix.genai.xira.web.model.CreateProjectRequest;
import eu.itsonix.genai.xira.web.model.ProjectDetailsResponse;
import eu.itsonix.genai.xira.web.model.ProjectMemberResponse;
import eu.itsonix.genai.xira.web.model.UpdateProjectMemberRoleRequest;
import eu.itsonix.genai.xira.web.model.UpdateProjectRequest;
import eu.itsonix.genai.xira.web.model.WorkflowStatusResponse;

@RestController
@RequiredArgsConstructor
public class ProjectController implements ProjectsApi {

    private final ProjectService projectService;
    private final WorkflowService workflowService;

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProjectDetailsResponse>> getProjects() {
        final List<ProjectDetailsResponse> projects = projectService.getProjectsForAuthenticatedUser();
        return ResponseEntity.ok(projects);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProjectDetailsResponse> getProjectByKey(final String key) {
        final ProjectDetailsResponse project = projectService.getProjectDetails(key);
        return ResponseEntity.ok(project);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> createProject(final CreateProjectRequest createProjectRequest) {
        projectService.createProject(createProjectRequest);
        return ResponseEntity.created(URI.create(String.format("/projects/%s", createProjectRequest.getKey()))).build();
    }

    @Override
    @PreAuthorize("@authService.isProjectAdmin(#key)")
    public ResponseEntity<Void> updateProject(final String key, final UpdateProjectRequest updateProjectRequest) {
        projectService.updateProject(key, updateProjectRequest);
        return ResponseEntity.ok().build();
    }

    @Override
    @PreAuthorize("@authService.isProjectMember(#key)")
    public ResponseEntity<List<ProjectMemberResponse>> getProjectMembers(final String key) {
        final List<ProjectMemberResponse> members = projectService.getProjectMembers(key);
        return ResponseEntity.ok(members);
    }

    @Override
    @PreAuthorize("@authService.isProjectAdmin(#key)")
    public ResponseEntity<Void> addProjectMember(final String key,
            final AddProjectMemberRequest addProjectMemberRequest) {
        projectService.addProjectMember(key, addProjectMemberRequest);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Override
    @PreAuthorize("@authService.isProjectAdmin(#key)")
    public ResponseEntity<Void> updateProjectMemberRole(final String key, final UUID userId,
            final UpdateProjectMemberRoleRequest updateProjectMemberRoleRequest) {
        projectService.updateProjectMemberRole(key, userId.toString(), updateProjectMemberRoleRequest);
        return ResponseEntity.ok().build();
    }

    @Override
    @PreAuthorize("@authService.isProjectAdmin(#key)")
    public ResponseEntity<Void> removeProjectMember(final String key, final UUID userId) {
        projectService.removeProjectMember(key, userId.toString());
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("@authService.isProjectMember(#key)")
    public ResponseEntity<List<WorkflowStatusResponse>> getWorkflowStatuses(final String key) {
        final List<WorkflowStatusResponse> statuses = workflowService.getWorkflowStatuses(key);
        return ResponseEntity.ok(statuses);
    }
}
