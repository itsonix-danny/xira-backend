package eu.itsonix.genai.xira.service;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import eu.itsonix.genai.xira.jpa.entity.Project;
import eu.itsonix.genai.xira.jpa.entity.ProjectMember;
import eu.itsonix.genai.xira.jpa.entity.ProjectRole;
import eu.itsonix.genai.xira.jpa.entity.XiraUser;
import eu.itsonix.genai.xira.jpa.repository.ProjectMemberRepository;
import eu.itsonix.genai.xira.jpa.repository.ProjectRepository;
import eu.itsonix.genai.xira.jpa.repository.XiraUserRepository;
import eu.itsonix.genai.xira.web.model.AddProjectMemberRequest;
import eu.itsonix.genai.xira.web.model.CreateProjectRequest;
import eu.itsonix.genai.xira.web.model.ProjectMemberRole;
import eu.itsonix.genai.xira.web.model.UpdateProjectRequest;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final WorkflowService workflowService;
    private final AuthService authService;
    private final XiraUserRepository xiraUserRepository;

    @Transactional
    public void createProject(final CreateProjectRequest createProjectRequest) {
        if (projectRepository.existsByKeyIgnoreCase(createProjectRequest.getKey())) {
            throw new IllegalStateException("Project key already exists");
        }

        final XiraUser owner = authService.getAuthenticatedUser();

        final Project project = Project.builder()
                .key(createProjectRequest.getKey())
                .name(createProjectRequest.getName())
                .description(createProjectRequest.getDescription())
                .owner(owner)
                .build();

        project.setWorkflow(workflowService.createDefaultWorkflow(project));

        projectRepository.save(project);

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

        if (updateProjectRequest.getName() != null) {
            project.setName(updateProjectRequest.getName());
        }

        if (updateProjectRequest.getDescription() != null) {
            project.setDescription(updateProjectRequest.getDescription());
        }

        projectRepository.save(project);
    }

    @Transactional
    public void addProjectMember(final String projectKey, final AddProjectMemberRequest addProjectMemberRequest) {
        final Project project = projectRepository.findByKeyIgnoreCase(projectKey)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));

        final String userId = addProjectMemberRequest.getUserId().toString();
        final XiraUser user = xiraUserRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (projectMemberRepository.existsByProjectIdAndUserId(project.getId(), userId)) {
            throw new IllegalStateException("User is already a project member");
        }

        final ProjectRole projectRole = mapRole(addProjectMemberRequest.getRole());

        projectMemberRepository.save(ProjectMember.builder()
                .projectId(project.getId())
                .userId(userId)
                .project(project)
                .xiraUser(user)
                .role(projectRole)
                .build());
    }

    @Transactional
    public void removeProjectMember(final String projectKey, final String userId) {
        final Project project = projectRepository.findByKeyIgnoreCase(projectKey)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));

        final ProjectMember projectMember = projectMemberRepository
                .findByProjectIdAndUserId(project.getId(), userId)
                .orElseThrow(() -> new EntityNotFoundException("Project member not found"));

        projectMemberRepository.delete(projectMember);
    }

    private ProjectRole mapRole(final ProjectMemberRole role) {
        if (role == null) {
            throw new IllegalArgumentException("Role must not be null");
        }
        return ProjectRole.valueOf(role.getValue());
    }
}
