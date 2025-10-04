package eu.itsonix.genai.xira.web;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import eu.itsonix.genai.xira.service.ProjectService;
import eu.itsonix.genai.xira.web.api.ProjectsApi;
import eu.itsonix.genai.xira.web.model.CreateProjectRequest;

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
}
