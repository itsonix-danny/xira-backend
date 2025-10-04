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
import org.springframework.security.access.AccessDeniedException;

import eu.itsonix.genai.xira.jpa.entity.Project;
import eu.itsonix.genai.xira.jpa.entity.ProjectMember;
import eu.itsonix.genai.xira.jpa.entity.XiraUser;
import eu.itsonix.genai.xira.jpa.repository.ProjectMemberRepository;
import eu.itsonix.genai.xira.jpa.repository.ProjectRepository;
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
    private AuthService authService;

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
        when(authService.isProjectAdmin("XIRA")).thenReturn(true);

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
    void givenNonAdmin_thenThrowsAccessDenied() {
        final UpdateProjectRequest request = new UpdateProjectRequest()
                .name("Updated Name");

        final Project existingProject = Project.builder()
                .id("project-id")
                .key("XIRA")
                .name("Old Name")
                .build();

        when(projectRepository.findByKeyIgnoreCase("XIRA")).thenReturn(Optional.of(existingProject));
        when(authService.isProjectAdmin("XIRA")).thenReturn(false);

        assertThatThrownBy(() -> projectService.updateProject("XIRA", request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("User is not a project admin");
    }
}
