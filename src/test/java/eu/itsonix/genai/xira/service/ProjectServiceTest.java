package eu.itsonix.genai.xira.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.itsonix.genai.xira.jpa.entity.Project;
import eu.itsonix.genai.xira.jpa.entity.ProjectMember;
import eu.itsonix.genai.xira.jpa.entity.XiraUser;
import eu.itsonix.genai.xira.jpa.repository.ProjectMemberRepository;
import eu.itsonix.genai.xira.jpa.repository.ProjectRepository;
import eu.itsonix.genai.xira.web.model.CreateProjectRequest;

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
                .name("Xira Project");

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
}
