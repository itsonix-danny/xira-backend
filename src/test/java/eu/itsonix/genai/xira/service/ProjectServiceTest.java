package eu.itsonix.genai.xira.service;

import jakarta.persistence.EntityNotFoundException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.itsonix.genai.xira.jpa.entity.Project;
import eu.itsonix.genai.xira.jpa.entity.ProjectMember;
import eu.itsonix.genai.xira.jpa.entity.ProjectRole;
import eu.itsonix.genai.xira.jpa.entity.XiraUser;
import eu.itsonix.genai.xira.jpa.repository.ProjectMemberRepository;
import eu.itsonix.genai.xira.jpa.repository.ProjectRepository;
import eu.itsonix.genai.xira.jpa.repository.XiraUserRepository;
import eu.itsonix.genai.xira.web.model.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private WorkflowService workflowService;

    @Mock
    private AuthService authService;

    @Mock
    private XiraUserRepository xiraUserRepository;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void givenValidRequest_thenCreatesProject() {
        final CreateProjectRequest request = new CreateProjectRequest().key("XIRA")
                .name("Xira Project")
                .description("Test description");

        when(projectRepository.existsByKeyIgnoreCase("XIRA")).thenReturn(false);

        final XiraUser owner = XiraUser.builder()
                .id("user-id")
                .email("owner@example.com")
                .firstName("Owner")
                .lastName("Example")
                .passwordHash("hash")
                .build();
        when(authService.getAuthenticatedUser()).thenReturn(owner);

        final Project savedProject = Project.builder()
                .id("project-id")
                .key("XIRA")
                .name("Xira Project")
                .description("Test description")
                .owner(owner)
                .build();
        when(projectRepository.save(any(Project.class))).thenReturn(savedProject);

        projectService.createProject(request);

        verify(projectRepository).save(any(Project.class));
        verify(workflowService).createDefaultWorkflow(any(Project.class));
        verify(projectMemberRepository).save(any(ProjectMember.class));
    }

    @Test
    void givenExistingProjectKey_thenThrowsIllegalState() {
        final CreateProjectRequest request = new CreateProjectRequest().key("XIRA").name("Xira Project");

        when(projectRepository.existsByKeyIgnoreCase("XIRA")).thenReturn(true);

        assertThatThrownBy(() -> projectService.createProject(request)).isInstanceOf(IllegalStateException.class)
                .hasMessage("Project key already exists");
    }

    @Test
    void givenValidUpdateRequest_thenUpdatesProject() {
        final UpdateProjectRequest request = new UpdateProjectRequest().name("Updated Name")
                .description("Updated Description");

        final Project existingProject = Project.builder()
                .id("project-id")
                .key("XIRA")
                .name("Old Name")
                .description("Old Description")
                .build();

        when(projectRepository.findByKeyIgnoreCase("XIRA")).thenReturn(Optional.of(existingProject));

        projectService.updateProject("XIRA", request);

        verify(projectRepository).save(any(Project.class));
    }

    @Test
    void givenNonExistentProject_thenThrowsEntityNotFound() {
        final UpdateProjectRequest request = new UpdateProjectRequest().name("Updated Name");

        when(projectRepository.findByKeyIgnoreCase("NONEXISTENT")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.updateProject("NONEXISTENT", request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Project not found");
    }

    @Test
    void givenValidRequest_whenAddProjectMember_thenSavesMember() {
        final AddProjectMemberRequest request = new AddProjectMemberRequest()
                .userId(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
                .role(ProjectMemberRole.DEVELOPER);

        final Project project = Project.builder().id("project-id").key("XIRA").build();

        when(projectRepository.findByKeyIgnoreCase("XIRA")).thenReturn(Optional.of(project));

        final XiraUser user = XiraUser.builder()
                .id("550e8400-e29b-41d4-a716-446655440000")
                .email("member@example.com")
                .build();
        when(xiraUserRepository.findById("550e8400-e29b-41d4-a716-446655440000")).thenReturn(Optional.of(user));
        when(projectMemberRepository.existsByProjectIdAndUserId("project-id", "550e8400-e29b-41d4-a716-446655440000"))
                .thenReturn(false);

        projectService.addProjectMember("XIRA", request);

        verify(projectMemberRepository).save(any(ProjectMember.class));
    }

    @Test
    void givenExistingMember_whenAddProjectMember_thenThrowsIllegalState() {
        final AddProjectMemberRequest request = new AddProjectMemberRequest()
                .userId(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
                .role(ProjectMemberRole.ADMIN);

        final Project project = Project.builder().id("project-id").key("XIRA").build();

        when(projectRepository.findByKeyIgnoreCase("XIRA")).thenReturn(Optional.of(project));
        when(xiraUserRepository.findById("550e8400-e29b-41d4-a716-446655440000")).thenReturn(Optional
                .of(XiraUser.builder().id("550e8400-e29b-41d4-a716-446655440000").email("member@example.com").build()));
        when(projectMemberRepository.existsByProjectIdAndUserId("project-id", "550e8400-e29b-41d4-a716-446655440000"))
                .thenReturn(true);

        assertThatThrownBy(() -> projectService.addProjectMember("XIRA", request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("User is already a project member");
    }

    @Test
    void givenNonExistingProject_whenAddProjectMember_thenThrowsEntityNotFound() {
        final AddProjectMemberRequest request = new AddProjectMemberRequest()
                .userId(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
                .role(ProjectMemberRole.DEVELOPER);

        when(projectRepository.findByKeyIgnoreCase("XIRA")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.addProjectMember("XIRA", request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Project not found");
    }

    @Test
    void givenNonExistingUser_whenAddProjectMember_thenThrowsEntityNotFound() {
        final AddProjectMemberRequest request = new AddProjectMemberRequest()
                .userId(UUID.fromString("550e8400-e29b-41d4-a716-446655440001"))
                .role(ProjectMemberRole.DEVELOPER);

        final Project project = Project.builder().id("project-id").key("XIRA").build();

        when(projectRepository.findByKeyIgnoreCase("XIRA")).thenReturn(Optional.of(project));
        when(xiraUserRepository.findById("550e8400-e29b-41d4-a716-446655440001")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.addProjectMember("XIRA", request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    void givenValidRequest_whenRemoveProjectMember_thenDeletesMember() {
        final Project project = Project.builder().id("project-id").key("XIRA").build();

        when(projectRepository.findByKeyIgnoreCase("XIRA")).thenReturn(Optional.of(project));

        final ProjectMember member = ProjectMember.builder()
                .projectId("project-id")
                .userId("user-id")
                .role(ProjectRole.DEVELOPER)
                .build();

        when(projectMemberRepository.findByProjectIdAndUserId("project-id", "user-id")).thenReturn(Optional.of(member));

        projectService.removeProjectMember("XIRA", "user-id");

        verify(projectMemberRepository).delete(member);
    }

    @Test
    void givenNonExistingProject_whenRemoveProjectMember_thenThrowsEntityNotFound() {
        when(projectRepository.findByKeyIgnoreCase("XIRA")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.removeProjectMember("XIRA", "user-id"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Project not found");
    }

    @Test
    void givenNonExistingMember_whenRemoveProjectMember_thenThrowsEntityNotFound() {
        final Project project = Project.builder().id("project-id").key("XIRA").build();

        when(projectRepository.findByKeyIgnoreCase("XIRA")).thenReturn(Optional.of(project));
        when(projectMemberRepository.findByProjectIdAndUserId("project-id", "user-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.removeProjectMember("XIRA", "user-id"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Project member not found");
    }

    @Test
    void givenValidRequest_whenUpdateProjectMemberRole_thenUpdatesRole() {
        final Project project = Project.builder().id("project-id").key("XIRA").ownerId("owner-id").build();
        final ProjectMember member = ProjectMember.builder()
                .projectId("project-id")
                .userId("user-id")
                .role(ProjectRole.DEVELOPER)
                .build();

        when(projectRepository.findByKeyIgnoreCase("XIRA")).thenReturn(Optional.of(project));
        when(projectMemberRepository.findByProjectIdAndUserId("project-id", "user-id")).thenReturn(Optional.of(member));

        final UpdateProjectMemberRoleRequest request = new UpdateProjectMemberRoleRequest()
                .role(ProjectMemberRole.ADMIN);

        projectService.updateProjectMemberRole("XIRA", "user-id", request);

        verify(projectMemberRepository).save(member);
    }

    @Test
    void givenOwner_whenUpdateProjectMemberRoleToDeveloper_thenThrowsIllegalState() {
        final XiraUser owner = XiraUser.builder().id("owner-id").build();
        final Project project = Project.builder()
                .id("project-id")
                .key("XIRA")
                .owner(owner)
                .ownerId(owner.getId())
                .build();
        final ProjectMember member = ProjectMember.builder()
                .projectId("project-id")
                .userId("owner-id")
                .role(ProjectRole.ADMIN)
                .build();

        when(projectRepository.findByKeyIgnoreCase("XIRA")).thenReturn(Optional.of(project));
        when(projectMemberRepository.findByProjectIdAndUserId("project-id", "owner-id"))
                .thenReturn(Optional.of(member));

        final UpdateProjectMemberRoleRequest request = new UpdateProjectMemberRoleRequest()
                .role(ProjectMemberRole.DEVELOPER);

        assertThatThrownBy(() -> projectService.updateProjectMemberRole("XIRA", "owner-id", request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Project owner must remain an admin");
    }

    @Test
    void givenNonExistingProject_whenUpdateProjectMemberRole_thenThrowsEntityNotFound() {
        final UpdateProjectMemberRoleRequest request = new UpdateProjectMemberRoleRequest()
                .role(ProjectMemberRole.DEVELOPER);

        when(projectRepository.findByKeyIgnoreCase("XIRA")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.updateProjectMemberRole("XIRA", "user-id", request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Project not found");
    }

    @Test
    void givenNonExistingMember_whenUpdateProjectMemberRole_thenThrowsEntityNotFound() {
        final Project project = Project.builder().id("project-id").key("XIRA").build();
        final UpdateProjectMemberRoleRequest request = new UpdateProjectMemberRoleRequest()
                .role(ProjectMemberRole.ADMIN);

        when(projectRepository.findByKeyIgnoreCase("XIRA")).thenReturn(Optional.of(project));
        when(projectMemberRepository.findByProjectIdAndUserId("project-id", "user-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.updateProjectMemberRole("XIRA", "user-id", request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Project member not found");
    }

    @Test
    void givenAuthenticatedUser_whenGetProjectsForAuthenticatedUser_thenReturnsProjects() {
        final XiraUser user = XiraUser.builder().id("user-id").email("user@example.com").build();
        when(authService.getAuthenticatedUser()).thenReturn(user);

        final Project project1 = Project.builder()
                .id("project-1")
                .key("PROJ1")
                .name("Project 1")
                .ownerId("owner-id")
                .build();

        final Project project2 = Project.builder()
                .id("project-2")
                .key("PROJ2")
                .name("Project 2")
                .ownerId("user-id")
                .build();

        final ProjectMember member1 = ProjectMember.builder()
                .projectId("project-1")
                .userId("user-id")
                .project(project1)
                .role(ProjectRole.DEVELOPER)
                .build();

        final ProjectMember member2 = ProjectMember.builder()
                .projectId("project-2")
                .userId("user-id")
                .project(project2)
                .role(ProjectRole.ADMIN)
                .build();

        when(projectMemberRepository.findAllByUserId("user-id")).thenReturn(List.of(member1, member2));

        final var projects = projectService.getProjectsForAuthenticatedUser();

        assertThat(projects).hasSize(2);
    }

    @Test
    void givenAuthenticatedUserAndValidProject_whenGetProjectDetails_thenReturnsProjectDetails() {
        final XiraUser user = XiraUser.builder().id("user-id").email("user@example.com").build();
        when(authService.getAuthenticatedUser()).thenReturn(user);

        final Project project = Project.builder()
                .id("project-id")
                .key("XIRA")
                .name("Xira Project")
                .ownerId("owner-id")
                .boards(List.of())
                .build();

        final ProjectMember projectMember = ProjectMember.builder()
                .projectId("project-id")
                .userId("user-id")
                .project(project)
                .role(ProjectRole.ADMIN)
                .build();

        when(projectMemberRepository.findByProject_KeyIgnoreCaseAndUserId("XIRA", "user-id"))
                .thenReturn(Optional.of(projectMember));

        final var projectDetails = projectService.getProjectDetails("XIRA");

        assertThat(projectDetails).isNotNull();
        assertThat(projectDetails.getKey()).isEqualTo("XIRA");
        assertThat(projectDetails.getName()).isEqualTo("Xira Project");
    }

    @Test
    void givenNonMember_whenGetProjectDetails_thenThrowsEntityNotFound() {
        final XiraUser user = XiraUser.builder().id("user-id").email("user@example.com").build();
        when(authService.getAuthenticatedUser()).thenReturn(user);

        when(projectMemberRepository.findByProject_KeyIgnoreCaseAndUserId("XIRA", "user-id"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getProjectDetails("XIRA"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Project not found");
    }
}
