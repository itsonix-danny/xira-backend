package eu.itsonix.genai.xira.service;

import jakarta.persistence.EntityNotFoundException;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import eu.itsonix.genai.xira.jpa.entity.Board;
import eu.itsonix.genai.xira.jpa.entity.Project;
import eu.itsonix.genai.xira.jpa.entity.ProjectMember;
import eu.itsonix.genai.xira.jpa.entity.ProjectRole;
import eu.itsonix.genai.xira.jpa.entity.XiraUser;
import eu.itsonix.genai.xira.jpa.repository.BoardRepository;
import eu.itsonix.genai.xira.jpa.repository.ProjectMemberRepository;
import eu.itsonix.genai.xira.jpa.repository.ProjectRepository;
import eu.itsonix.genai.xira.jpa.repository.XiraUserRepository;
import eu.itsonix.genai.xira.mapper.ProjectMapper;
import eu.itsonix.genai.xira.web.model.AddProjectMemberRequest;
import eu.itsonix.genai.xira.web.model.CreateProjectRequest;
import eu.itsonix.genai.xira.web.model.ProjectDetailsResponse;
import eu.itsonix.genai.xira.web.model.ProjectMemberRole;
import eu.itsonix.genai.xira.web.model.ProjectSummaryResponse;
import eu.itsonix.genai.xira.web.model.UpdateProjectMemberRoleRequest;
import eu.itsonix.genai.xira.web.model.UpdateProjectRequest;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final WorkflowService workflowService;
    private final AuthService authService;
    private final XiraUserRepository xiraUserRepository;
    private final BoardRepository boardRepository;

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

        final ProjectRole projectRole = ProjectMapper.toProjectRole(addProjectMemberRequest.getRole());

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

        final ProjectMember projectMember = projectMemberRepository.findByProjectIdAndUserId(project.getId(), userId)
                .orElseThrow(() -> new EntityNotFoundException("Project member not found"));

        projectMemberRepository.delete(projectMember);
    }

    @Transactional
    public void updateProjectMemberRole(final String projectKey, final String userId,
            final UpdateProjectMemberRoleRequest updateProjectMemberRoleRequest) {
        final Project project = projectRepository.findByKeyIgnoreCase(projectKey)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));

        final ProjectMember projectMember = projectMemberRepository.findByProjectIdAndUserId(project.getId(), userId)
                .orElseThrow(() -> new EntityNotFoundException("Project member not found"));

        final ProjectRole newRole = ProjectMapper.toProjectRole(updateProjectMemberRoleRequest.getRole());

        if (project.getOwnerId().equals(userId) && newRole != ProjectRole.ADMIN) {
            throw new IllegalStateException("Project owner must remain an admin");
        }

        projectMember.setRole(newRole);
        projectMemberRepository.save(projectMember);
    }

    @Transactional(readOnly = true)
    public List<ProjectSummaryResponse> getProjectsForAuthenticatedUser() {
        final XiraUser user = authService.getAuthenticatedUser();

        return projectMemberRepository.findAllByUserId(user.getId()).stream()
                .map(projectMember -> ProjectMapper.toProjectSummaryResponse(projectMember.getProject(), projectMember))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectDetailsResponse getProjectDetails(final String projectKey) {
        final XiraUser user = authService.getAuthenticatedUser();

        final Project project = projectRepository.findByKeyIgnoreCase(projectKey)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));

        final ProjectMember projectMember = projectMemberRepository
                .findByProjectIdAndUserId(project.getId(), user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Project member not found"));

        final List<Board> boards = boardRepository.findAllByProjectIdOrderByBoardNumberAsc(project.getId());

        return ProjectMapper.toProjectDetailsResponse(project, projectMember, boards);
    }
}
