package eu.itsonix.genai.xira.web;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import eu.itsonix.genai.xira.service.ProjectService;
import eu.itsonix.genai.xira.web.api.ProjectsApi;
import eu.itsonix.genai.xira.web.model.AddProjectMemberRequest;
import eu.itsonix.genai.xira.web.model.CreateProjectRequest;
import eu.itsonix.genai.xira.web.model.UpdateProjectRequest;

@RestController
@RequiredArgsConstructor
public class ProjectController implements ProjectsApi {

    private final ProjectService projectService;

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
    @PreAuthorize("@authService.isProjectAdmin(#key)")
    public ResponseEntity<Void> addProjectMember(final String key,
            final AddProjectMemberRequest addProjectMemberRequest) {
        projectService.addProjectMember(key, addProjectMemberRequest);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Override
    @PreAuthorize("@authService.isProjectAdmin(#key)")
    public ResponseEntity<Void> removeProjectMember(final String key, final UUID userId) {
        projectService.removeProjectMember(key, userId.toString());
        return ResponseEntity.noContent().build();
    }
}
