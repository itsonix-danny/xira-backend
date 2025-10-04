package eu.itsonix.genai.xira.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityNotFoundException;
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
import eu.itsonix.genai.xira.web.model.AddProjectMemberRequest;
import eu.itsonix.genai.xira.web.model.ProjectMemberRole;
import eu.itsonix.genai.xira.web.model.CreateProjectRequest;
import eu.itsonix.genai.xira.web.model.UpdateProjectRequest;

import java.util.Optional;

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
        final CreateProjectRequest request = new CreateProjectRequest()
                .key("XIRA")
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
        final CreateProjectRequest request = new CreateProjectRequest()
                .key("XIRA")
                .name("Xira Project");

        when(projectRepository.existsByKeyIgnoreCase("XIRA")).thenReturn(true);

        assertThatThrownBy(() -> projectService.createProject(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Project key already exists");
    }

    @Test
    void givenValidUpdateRequest_thenUpdatesProject() {
        final UpdateProjectRequest request = new UpdateProjectRequest()
                .name("Updated Name")
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
        final UpdateProjectRequest request = new UpdateProjectRequest()
                .name("Updated Name");

        when(projectRepository.findByKeyIgnoreCase("NONEXISTENT")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.updateProject("NONEXISTENT", request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Project not found");
    }

    @Test
    void givenValidRequest_whenAddProjectMember_thenSavesMember() {
        final AddProjectMemberRequest request = new AddProjectMemberRequest()
                .email("member@example.com")
                .role(ProjectMemberRole.DEVELOPER);

        final Project project = Project.builder()
                .id("project-id")
                .key("XIRA")
                .build();

        when(projectRepository.findByKeyIgnoreCase("XIRA")).thenReturn(Optional.of(project));
        when(authService.isProjectAdmin("XIRA")).thenReturn(true);

        final XiraUser user = XiraUser.builder()
                .id("user-id")
                .email("member@example.com")
                .build();
        when(xiraUserRepository.findByEmailIgnoreCase("member@example.com")).thenReturn(Optional.of(user));
        when(projectMemberRepository.existsByProject_KeyIgnoreCaseAndXiraUser_EmailIgnoreCase("XIRA",
                "member@example.com")).thenReturn(false);

        projectService.addProjectMember("XIRA", request);

        verify(projectMemberRepository).save(any(ProjectMember.class));
    }

    @Test
    void givenExistingMember_whenAddProjectMember_thenThrowsIllegalState() {
        final AddProjectMemberRequest request = new AddProjectMemberRequest()
                .email("member@example.com")
                .role(ProjectMemberRole.ADMIN);

        final Project project = Project.builder()
                .id("project-id")
                .key("XIRA")
                .build();

        when(projectRepository.findByKeyIgnoreCase("XIRA")).thenReturn(Optional.of(project));
        when(authService.isProjectAdmin("XIRA")).thenReturn(true);
        when(xiraUserRepository.findByEmailIgnoreCase("member@example.com"))
                .thenReturn(Optional.of(XiraUser.builder().id("user-id").email("member@example.com").build()));
        when(projectMemberRepository.existsByProject_KeyIgnoreCaseAndXiraUser_EmailIgnoreCase("XIRA",
                "member@example.com")).thenReturn(true);

        assertThatThrownBy(() -> projectService.addProjectMember("XIRA", request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("User is already a project member");
    }

    @Test
    void givenNonExistingProject_whenAddProjectMember_thenThrowsEntityNotFound() {
        final AddProjectMemberRequest request = new AddProjectMemberRequest()
                .email("member@example.com")
                .role(ProjectMemberRole.DEVELOPER);

        when(projectRepository.findByKeyIgnoreCase("XIRA")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.addProjectMember("XIRA", request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Project not found");
    }

    @Test
    void givenNonExistingUser_whenAddProjectMember_thenThrowsEntityNotFound() {
        final AddProjectMemberRequest request = new AddProjectMemberRequest()
                .email("missing@example.com")
                .role(ProjectMemberRole.DEVELOPER);

        final Project project = Project.builder()
                .id("project-id")
                .key("XIRA")
                .build();

        when(projectRepository.findByKeyIgnoreCase("XIRA")).thenReturn(Optional.of(project));
        when(authService.isProjectAdmin("XIRA")).thenReturn(true);
        when(xiraUserRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.addProjectMember("XIRA", request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    void givenValidRequest_whenRemoveProjectMember_thenDeletesMember() {
        final Project project = Project.builder()
                .id("project-id")
                .key("XIRA")
                .build();

        when(projectRepository.findByKeyIgnoreCase("XIRA")).thenReturn(Optional.of(project));
        when(authService.isProjectAdmin("XIRA")).thenReturn(true);

        final ProjectMember member = ProjectMember.builder()
                .projectId("project-id")
                .userId("user-id")
                .role(ProjectRole.DEVELOPER)
                .build();

        when(projectMemberRepository.findByProject_KeyIgnoreCaseAndXiraUser_EmailIgnoreCase("XIRA", "member@example.com"))
                .thenReturn(Optional.of(member));

        projectService.removeProjectMember("XIRA", "member@example.com");

        verify(projectMemberRepository).delete(member);
    }

    @Test
    void givenNonExistingProject_whenRemoveProjectMember_thenThrowsEntityNotFound() {
        when(projectRepository.findByKeyIgnoreCase("XIRA")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.removeProjectMember("XIRA", "member@example.com"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Project not found");
    }

    @Test
    void givenNonExistingMember_whenRemoveProjectMember_thenThrowsEntityNotFound() {
        final Project project = Project.builder()
                .id("project-id")
                .key("XIRA")
                .build();

        when(projectRepository.findByKeyIgnoreCase("XIRA")).thenReturn(Optional.of(project));
        when(authService.isProjectAdmin("XIRA")).thenReturn(true);
        when(projectMemberRepository.findByProject_KeyIgnoreCaseAndXiraUser_EmailIgnoreCase("XIRA",
                "member@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.removeProjectMember("XIRA", "member@example.com"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Project member not found");
    }
}
