package eu.itsonix.genai.xira.service;

import jakarta.persistence.EntityNotFoundException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.itsonix.genai.xira.jpa.entity.*;
import eu.itsonix.genai.xira.jpa.repository.IssueRepository;
import eu.itsonix.genai.xira.jpa.repository.ProjectRepository;
import eu.itsonix.genai.xira.jpa.repository.SprintIssueRepository;
import eu.itsonix.genai.xira.jpa.repository.SprintRepository;
import eu.itsonix.genai.xira.web.model.AddSprintRequest;
import eu.itsonix.genai.xira.web.model.UpdateSprintRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SprintServiceTest {

    private static final String PROJECT_KEY = "XIRA";
    private static final String PROJECT_ID = "project-id";

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private SprintRepository sprintRepository;

    @Mock
    private SprintIssueRepository sprintIssueRepository;

    @Mock
    private IssueRepository issueRepository;

    @InjectMocks
    private SprintService sprintService;

    @Test
    void givenExistingProject_whenCreateSprint_thenReturnsSprintId() {
        final Project project = project();
        final String sprintId = UUID.randomUUID().toString();

        when(projectRepository.findByKeyIgnoreCase(PROJECT_KEY)).thenReturn(Optional.of(project));
        when(sprintRepository.save(any(Sprint.class))).thenAnswer(invocation -> {
            final Sprint sprint = invocation.getArgument(0);
            sprint.setId(sprintId);
            return sprint;
        });

        final String returnedId = sprintService.createSprint(PROJECT_KEY,
                new AddSprintRequest().name("Sprint 1").goal("Deliver feature"));

        assertThat(returnedId).isEqualTo(sprintId);

        final ArgumentCaptor<Sprint> sprintCaptor = ArgumentCaptor.forClass(Sprint.class);
        verify(sprintRepository).save(sprintCaptor.capture());
        assertThat(sprintCaptor.getValue().getName()).isEqualTo("Sprint 1");
        assertThat(sprintCaptor.getValue().getGoal()).isEqualTo("Deliver feature");
        assertThat(sprintCaptor.getValue().getState()).isEqualTo(SprintState.PLANNED);
        assertThat(sprintCaptor.getValue().getProject()).isEqualTo(project);
    }

    @Test
    void givenUnknownProject_whenCreateSprint_thenThrowsEntityNotFound() {
        when(projectRepository.findByKeyIgnoreCase(PROJECT_KEY)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sprintService.createSprint(PROJECT_KEY,
                new AddSprintRequest().name("Sprint 1").goal("Deliver feature")))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Project not found");

        verify(sprintRepository, never()).save(any(Sprint.class));
    }

    @Test
    void givenPlannedSprint_whenUpdateSprint_thenReturnsUpdatedResponse() {
        final Sprint sprint = plannedSprint();
        when(sprintRepository.findByProject_KeyIgnoreCaseAndId(PROJECT_KEY, sprint.getId()))
                .thenReturn(Optional.of(sprint));

        sprintService.updateSprint(PROJECT_KEY, sprint.getId(),
                new UpdateSprintRequest().name("Updated Sprint").goal("Updated goal"));

        assertThat(sprint.getName()).isEqualTo("Updated Sprint");
        assertThat(sprint.getGoal()).isEqualTo("Updated goal");
        verify(sprintRepository).save(sprint);
    }

    @Test
    void givenClosedSprint_whenUpdateSprint_thenThrowsIllegalState() {
        final Sprint sprint = plannedSprint();
        sprint.setState(SprintState.CLOSED);
        when(sprintRepository.findByProject_KeyIgnoreCaseAndId(PROJECT_KEY, sprint.getId()))
                .thenReturn(Optional.of(sprint));

        assertThatThrownBy(() -> sprintService.updateSprint(PROJECT_KEY, sprint.getId(),
                new UpdateSprintRequest().name("Updated Sprint"))).isInstanceOf(IllegalStateException.class)
                .hasMessage("Finished sprints cannot be updated");

        verify(sprintRepository, never()).save(any(Sprint.class));
    }

    @Test
    void givenUnknownSprint_whenUpdateSprint_thenThrowsEntityNotFound() {
        when(sprintRepository.findByProject_KeyIgnoreCaseAndId(PROJECT_KEY, "unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sprintService.updateSprint(PROJECT_KEY, "unknown",
                new UpdateSprintRequest().name("Updated Sprint"))).isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Sprint not found");
    }

    @Test
    void givenPlannedSprint_whenDeleteSprint_thenDeletesSprintAndIssues() {
        final Sprint sprint = plannedSprint();
        when(sprintRepository.findByProject_KeyIgnoreCaseAndId(PROJECT_KEY, sprint.getId()))
                .thenReturn(Optional.of(sprint));

        sprintService.deleteSprint(PROJECT_KEY, sprint.getId());

        verify(sprintIssueRepository).deleteAllBySprintId(sprint.getId());
        verify(sprintRepository).delete(sprint);
    }

    @Test
    void givenActiveSprint_whenDeleteSprint_thenThrowsIllegalState() {
        final Sprint sprint = plannedSprint();
        sprint.setState(SprintState.ACTIVE);
        when(sprintRepository.findByProject_KeyIgnoreCaseAndId(PROJECT_KEY, sprint.getId()))
                .thenReturn(Optional.of(sprint));

        assertThatThrownBy(() -> sprintService.deleteSprint(PROJECT_KEY, sprint.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only planned sprints can be deleted");

        verify(sprintIssueRepository, never()).deleteAllBySprintId(sprint.getId());
        verify(sprintRepository, never()).delete(any(Sprint.class));
    }

    @Test
    void givenPlannedSprint_whenStartSprint_thenSetsActiveState() {
        final Sprint sprint = plannedSprint();
        sprint.setFinishedAt(Instant.now());
        when(sprintRepository.findByProject_KeyIgnoreCaseAndId(PROJECT_KEY, sprint.getId()))
                .thenReturn(Optional.of(sprint));
        when(sprintRepository.existsByProjectIdAndStateAndIdNot(PROJECT_ID, SprintState.ACTIVE, sprint.getId()))
                .thenReturn(false);

        sprintService.startSprint(PROJECT_KEY, sprint.getId());

        assertThat(sprint.getState()).isEqualTo(SprintState.ACTIVE);
        assertThat(sprint.getStartedAt()).isNotNull();
        assertThat(sprint.getFinishedAt()).isNull();
        verify(sprintRepository).save(sprint);
    }

    @Test
    void givenClosedSprint_whenStartSprint_thenThrowsIllegalState() {
        final Sprint sprint = plannedSprint();
        sprint.setState(SprintState.CLOSED);
        when(sprintRepository.findByProject_KeyIgnoreCaseAndId(PROJECT_KEY, sprint.getId()))
                .thenReturn(Optional.of(sprint));

        assertThatThrownBy(() -> sprintService.startSprint(PROJECT_KEY, sprint.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Finished sprints cannot be started");
    }

    @Test
    void givenActiveSprint_whenStartSprint_thenThrowsIllegalState() {
        final Sprint sprint = plannedSprint();
        sprint.setState(SprintState.ACTIVE);
        when(sprintRepository.findByProject_KeyIgnoreCaseAndId(PROJECT_KEY, sprint.getId()))
                .thenReturn(Optional.of(sprint));

        assertThatThrownBy(() -> sprintService.startSprint(PROJECT_KEY, sprint.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Sprint is already active");
    }

    @Test
    void givenAnotherActiveSprint_whenStartSprint_thenThrowsIllegalState() {
        final Sprint sprint = plannedSprint();
        when(sprintRepository.findByProject_KeyIgnoreCaseAndId(PROJECT_KEY, sprint.getId()))
                .thenReturn(Optional.of(sprint));
        when(sprintRepository.existsByProjectIdAndStateAndIdNot(PROJECT_ID, SprintState.ACTIVE, sprint.getId()))
                .thenReturn(true);

        assertThatThrownBy(() -> sprintService.startSprint(PROJECT_KEY, sprint.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Another sprint is already active for this project");
    }

    @Test
    void givenActiveSprintWithUnfinishedIssues_whenFinishSprint_thenRemovesUnfinishedIssues() {
        final Sprint sprint = plannedSprint();
        sprint.setState(SprintState.ACTIVE);
        sprint.setStartedAt(Instant.now().minusSeconds(3600));

        when(sprintRepository.findByProject_KeyIgnoreCaseAndId(PROJECT_KEY, sprint.getId()))
                .thenReturn(Optional.of(sprint));

        sprintService.finishSprint(PROJECT_KEY, sprint.getId());

        assertThat(sprint.getState()).isEqualTo(SprintState.CLOSED);
        assertThat(sprint.getFinishedAt()).isNotNull();
        verify(sprintIssueRepository, times(1)).deleteAllBySprintIdAndIssue_Status_CategoryNot(sprint.getId(),
                WorkflowStatusCategory.DONE);
    }

    @Test
    void givenActiveSprintWithoutUnfinishedIssues_whenFinishSprint_thenDeletesUnfinishedIssues() {
        final Sprint sprint = plannedSprint();
        sprint.setState(SprintState.ACTIVE);

        when(sprintRepository.findByProject_KeyIgnoreCaseAndId(PROJECT_KEY, sprint.getId()))
                .thenReturn(Optional.of(sprint));

        sprintService.finishSprint(PROJECT_KEY, sprint.getId());

        verify(sprintIssueRepository, times(1)).deleteAllBySprintIdAndIssue_Status_CategoryNot(sprint.getId(),
                WorkflowStatusCategory.DONE);
        assertThat(sprint.getState()).isEqualTo(SprintState.CLOSED);
    }

    @Test
    void givenNonActiveSprint_whenFinishSprint_thenThrowsIllegalState() {
        final Sprint sprint = plannedSprint();
        when(sprintRepository.findByProject_KeyIgnoreCaseAndId(PROJECT_KEY, sprint.getId()))
                .thenReturn(Optional.of(sprint));

        assertThatThrownBy(() -> sprintService.finishSprint(PROJECT_KEY, sprint.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only active sprints can be finished");
    }

    private Sprint plannedSprint() {
        return Sprint.builder()
                .id(UUID.randomUUID().toString())
                .project(project())
                .projectId(PROJECT_ID)
                .name("Sprint 1")
                .goal("Deliver feature")
                .state(SprintState.PLANNED)
                .build();
    }

    @Test
    void givenPlannedSprintAndIssue_whenAddIssueToSprint_thenAddsIssue() {
        final Sprint sprint = plannedSprint();
        final Issue issue = createIssue();

        when(sprintRepository.findByProject_KeyIgnoreCaseAndId(PROJECT_KEY, sprint.getId()))
                .thenReturn(Optional.of(sprint));
        when(issueRepository.findByKeyAndProjectKeyIgnoreCase("XIRA-1", PROJECT_KEY)).thenReturn(Optional.of(issue));
        when(sprintIssueRepository.existsBySprintIdAndIssueId(sprint.getId(), issue.getId())).thenReturn(false);

        sprintService.addIssueToSprint(PROJECT_KEY, sprint.getId(), "XIRA-1");

        verify(sprintIssueRepository).deleteAllByIssueId(issue.getId());
        final ArgumentCaptor<SprintIssue> captor = ArgumentCaptor.forClass(SprintIssue.class);
        verify(sprintIssueRepository).save(captor.capture());
        assertThat(captor.getValue().getSprintId()).isEqualTo(sprint.getId());
        assertThat(captor.getValue().getIssueId()).isEqualTo(issue.getId());
    }

    @Test
    void givenClosedSprint_whenAddIssueToSprint_thenThrowsIllegalState() {
        final Sprint sprint = plannedSprint();
        sprint.setState(SprintState.CLOSED);

        when(sprintRepository.findByProject_KeyIgnoreCaseAndId(PROJECT_KEY, sprint.getId()))
                .thenReturn(Optional.of(sprint));

        assertThatThrownBy(() -> sprintService.addIssueToSprint(PROJECT_KEY, sprint.getId(), "XIRA-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot add issues to a finished sprint");

        verify(sprintIssueRepository, never()).save(any(SprintIssue.class));
    }

    @Test
    void givenNonExistentIssue_whenAddIssueToSprint_thenThrowsEntityNotFound() {
        final Sprint sprint = plannedSprint();

        when(sprintRepository.findByProject_KeyIgnoreCaseAndId(PROJECT_KEY, sprint.getId()))
                .thenReturn(Optional.of(sprint));
        when(issueRepository.findByKeyAndProjectKeyIgnoreCase("XIRA-99", PROJECT_KEY)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sprintService.addIssueToSprint(PROJECT_KEY, sprint.getId(), "XIRA-99"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Issue not found");

        verify(sprintIssueRepository, never()).save(any(SprintIssue.class));
    }

    @Test
    void givenIssueAlreadyInSprint_whenAddIssueToSprint_thenThrowsIllegalState() {
        final Sprint sprint = plannedSprint();
        final Issue issue = createIssue();

        when(sprintRepository.findByProject_KeyIgnoreCaseAndId(PROJECT_KEY, sprint.getId()))
                .thenReturn(Optional.of(sprint));
        when(issueRepository.findByKeyAndProjectKeyIgnoreCase("XIRA-1", PROJECT_KEY)).thenReturn(Optional.of(issue));
        when(sprintIssueRepository.existsBySprintIdAndIssueId(sprint.getId(), issue.getId())).thenReturn(true);

        assertThatThrownBy(() -> sprintService.addIssueToSprint(PROJECT_KEY, sprint.getId(), "XIRA-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Issue is already in this sprint");

        verify(sprintIssueRepository, never()).save(any(SprintIssue.class));
    }

    @Test
    void givenPlannedSprintAndIssueInSprint_whenRemoveIssueFromSprint_thenRemovesIssue() {
        final Sprint sprint = plannedSprint();
        final Issue issue = createIssue();

        when(sprintRepository.findByProject_KeyIgnoreCaseAndId(PROJECT_KEY, sprint.getId()))
                .thenReturn(Optional.of(sprint));
        when(issueRepository.findByKeyAndProjectKeyIgnoreCase("XIRA-1", PROJECT_KEY)).thenReturn(Optional.of(issue));
        when(sprintIssueRepository.existsBySprintIdAndIssueId(sprint.getId(), issue.getId())).thenReturn(true);

        sprintService.removeIssueFromSprint(PROJECT_KEY, sprint.getId(), "XIRA-1");

        verify(sprintIssueRepository).deleteBySprintIdAndIssueId(sprint.getId(), issue.getId());
    }

    @Test
    void givenClosedSprint_whenRemoveIssueFromSprint_thenThrowsIllegalState() {
        final Sprint sprint = plannedSprint();
        sprint.setState(SprintState.CLOSED);

        when(sprintRepository.findByProject_KeyIgnoreCaseAndId(PROJECT_KEY, sprint.getId()))
                .thenReturn(Optional.of(sprint));

        assertThatThrownBy(() -> sprintService.removeIssueFromSprint(PROJECT_KEY, sprint.getId(), "XIRA-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot remove issues from a finished sprint");

        verify(sprintIssueRepository, never()).deleteBySprintIdAndIssueId(any(), any());
    }

    @Test
    void givenIssueNotInSprint_whenRemoveIssueFromSprint_thenThrowsEntityNotFound() {
        final Sprint sprint = plannedSprint();
        final Issue issue = createIssue();

        when(sprintRepository.findByProject_KeyIgnoreCaseAndId(PROJECT_KEY, sprint.getId()))
                .thenReturn(Optional.of(sprint));
        when(issueRepository.findByKeyAndProjectKeyIgnoreCase("XIRA-1", PROJECT_KEY)).thenReturn(Optional.of(issue));
        when(sprintIssueRepository.existsBySprintIdAndIssueId(sprint.getId(), issue.getId())).thenReturn(false);

        assertThatThrownBy(() -> sprintService.removeIssueFromSprint(PROJECT_KEY, sprint.getId(), "XIRA-1"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Issue is not in this sprint");

        verify(sprintIssueRepository, never()).deleteBySprintIdAndIssueId(any(), any());
    }

    private Project project() {
        final XiraUser owner = XiraUser.builder()
                .id("owner-id")
                .email("owner@example.com")
                .firstName("Owner")
                .lastName("User")
                .passwordHash("hash")
                .build();
        return Project.builder().id(PROJECT_ID).key(PROJECT_KEY).name("Xira").owner(owner).build();
    }

    private Issue createIssue() {
        return Issue.builder()
                .id(UUID.randomUUID().toString())
                .key("XIRA-1")
                .title("Test Issue")
                .seqNo(1)
                .issueType(IssueType.TASK)
                .priority(IssuePriority.MEDIUM)
                .projectId(PROJECT_ID)
                .build();
    }
}
