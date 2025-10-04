package eu.itsonix.genai.xira.service;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import eu.itsonix.genai.xira.jpa.entity.Project;
import eu.itsonix.genai.xira.jpa.entity.ProjectMember;
import eu.itsonix.genai.xira.jpa.entity.ProjectRole;
import eu.itsonix.genai.xira.jpa.entity.XiraUser;
import eu.itsonix.genai.xira.jpa.repository.ProjectMemberRepository;
import eu.itsonix.genai.xira.jpa.repository.ProjectRepository;
import eu.itsonix.genai.xira.web.model.CreateProjectRequest;
import eu.itsonix.genai.xira.web.model.UpdateProjectRequest;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final AuthService authService;

    @Transactional
    public void createProject(final CreateProjectRequest createProjectRequest) {
        if (projectRepository.existsByKeyIgnoreCase(createProjectRequest.getKey())) {
            throw new IllegalStateException("Project key already exists");
        }

        final XiraUser owner = authService.getAuthenticatedUser();

        final Project project = projectRepository.save(Project.builder()
                .key(createProjectRequest.getKey())
                .name(createProjectRequest.getName())
                .description(createProjectRequest.getDescription())
                .owner(owner)
                .build());

        projectMemberRepository.save(ProjectMember.builder()
                .projectId(project.getId())
                .userId(owner.getId())
                .project(project)
                .xiraUser(owner)
                .role(ProjectRole.ADMIN)
                .build());
    }

    @Transactional
    public void updateProject(final String projectKey, final UpdateProjectRequest updateProjectRequest) {
        final Project project = projectRepository.findByKeyIgnoreCase(projectKey)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));

        if (!authService.isProjectAdmin(projectKey)) {
            throw new AccessDeniedException("User is not a project admin");
        }

        if (updateProjectRequest.getName() != null) {
            project.setName(updateProjectRequest.getName());
        }

        if (updateProjectRequest.getDescription() != null) {
            project.setDescription(updateProjectRequest.getDescription());
        }

        projectRepository.save(project);
    }

}
