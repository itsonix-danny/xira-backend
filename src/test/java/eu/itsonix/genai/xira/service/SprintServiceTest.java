package eu.itsonix.genai.xira.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityNotFoundException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.itsonix.genai.xira.jpa.entity.Issue;
import eu.itsonix.genai.xira.jpa.entity.IssuePriority;
import eu.itsonix.genai.xira.jpa.entity.IssueType;
import eu.itsonix.genai.xira.jpa.entity.Project;
import eu.itsonix.genai.xira.jpa.entity.Sprint;
import eu.itsonix.genai.xira.jpa.entity.SprintIssue;
import eu.itsonix.genai.xira.jpa.entity.SprintState;
import eu.itsonix.genai.xira.jpa.entity.Workflow;
import eu.itsonix.genai.xira.jpa.entity.WorkflowStatus;
import eu.itsonix.genai.xira.jpa.entity.WorkflowStatusCategory;
import eu.itsonix.genai.xira.jpa.entity.XiraUser;
import eu.itsonix.genai.xira.jpa.repository.ProjectRepository;
import eu.itsonix.genai.xira.jpa.repository.SprintIssueRepository;
import eu.itsonix.genai.xira.jpa.repository.SprintRepository;
import eu.itsonix.genai.xira.web.model.AddSprintRequest;
import eu.itsonix.genai.xira.web.model.SprintResponse;
import eu.itsonix.genai.xira.web.model.UpdateSprintRequest;

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

    @InjectMocks
    private SprintService sprintService;

    @Test
    void givenExistingProject_whenCreateSprint_thenReturnsSprintResponse() {
        final Project project = project();
        final String sprintId = UUID.randomUUID().toString();

        when(projectRepository.findByKeyIgnoreCase(PROJECT_KEY)).thenReturn(Optional.of(project));
        when(sprintRepository.save(any(Sprint.class))).thenAnswer(invocation -> {
            final Sprint sprint = invocation.getArgument(0);
            sprint.setId(sprintId);
            return sprint;
        });

        final SprintResponse response = sprintService.createSprint(PROJECT_KEY,
                new AddSprintRequest().name("Sprint 1").goal("Deliver feature"));

        assertThat(response.getId()).isEqualTo(UUID.fromString(sprintId));
        assertThat(response.getName()).isEqualTo("Sprint 1");
        assertThat(response.getGoal()).isEqualTo("Deliver feature");
        assertThat(response.getState()).isEqualTo(eu.itsonix.genai.xira.web.model.SprintState.PLANNED);

        final ArgumentCaptor<Sprint> sprintCaptor = ArgumentCaptor.forClass(Sprint.class);
        verify(sprintRepository).save(sprintCaptor.capture());
        assertThat(sprintCaptor.getValue().getState()).isEqualTo(SprintState.PLANNED);
        assertThat(sprintCaptor.getValue().getProject()).isEqualTo(project);
        assertThat(sprintCaptor.getValue().getGoal()).isEqualTo("Deliver feature");
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
        when(sprintRepository.save(sprint)).thenReturn(sprint);

        final SprintResponse response = sprintService.updateSprint(PROJECT_KEY, sprint.getId(),
                new UpdateSprintRequest().name("Updated Sprint").goal("Updated goal"));

        assertThat(response.getName()).isEqualTo("Updated Sprint");
        assertThat(response.getGoal()).isEqualTo("Updated goal");
        verify(sprintRepository).save(sprint);
    }

    @Test
    void givenClosedSprint_whenUpdateSprint_thenThrowsIllegalState() {
        final Sprint sprint = plannedSprint();
        sprint.setState(SprintState.CLOSED);
        when(sprintRepository.findByProject_KeyIgnoreCaseAndId(PROJECT_KEY, sprint.getId()))
                .thenReturn(Optional.of(sprint));

        assertThatThrownBy(() -> sprintService.updateSprint(PROJECT_KEY, sprint.getId(),
                new UpdateSprintRequest().name("Updated Sprint")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Finished sprints cannot be updated");

        verify(sprintRepository, never()).save(any(Sprint.class));
    }

    @Test
    void givenUnknownSprint_whenUpdateSprint_thenThrowsEntityNotFound() {
        when(sprintRepository.findByProject_KeyIgnoreCaseAndId(PROJECT_KEY, "unknown"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> sprintService.updateSprint(PROJECT_KEY, "unknown",
                new UpdateSprintRequest().name("Updated Sprint")))
                .isInstanceOf(EntityNotFoundException.class)
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
        when(sprintRepository.save(sprint)).thenReturn(sprint);

        final SprintResponse response = sprintService.startSprint(PROJECT_KEY, sprint.getId());

        assertThat(response.getState()).isEqualTo(eu.itsonix.genai.xira.web.model.SprintState.ACTIVE);
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
        final SprintIssue doneIssue = sprintIssue(sprint.getId(), doneWorkflowStatus());
        final SprintIssue todoIssue = sprintIssue(sprint.getId(), todoWorkflowStatus());

        when(sprintRepository.findByProject_KeyIgnoreCaseAndId(PROJECT_KEY, sprint.getId()))
                .thenReturn(Optional.of(sprint));
        when(sprintIssueRepository.findAllBySprintId(sprint.getId()))
                .thenReturn(List.of(doneIssue, todoIssue));
        when(sprintRepository.save(sprint)).thenReturn(sprint);

        final SprintResponse response = sprintService.finishSprint(PROJECT_KEY, sprint.getId());

        assertThat(response.getState()).isEqualTo(eu.itsonix.genai.xira.web.model.SprintState.CLOSED);
        assertThat(sprint.getState()).isEqualTo(SprintState.CLOSED);
        assertThat(sprint.getFinishedAt()).isNotNull();
        verify(sprintIssueRepository, times(1)).deleteAll(List.of(todoIssue));
    }

    @Test
    void givenActiveSprintWithoutUnfinishedIssues_whenFinishSprint_thenKeepsAssignments() {
        final Sprint sprint = plannedSprint();
        sprint.setState(SprintState.ACTIVE);
        final SprintIssue doneIssue = sprintIssue(sprint.getId(), doneWorkflowStatus());

        when(sprintRepository.findByProject_KeyIgnoreCaseAndId(PROJECT_KEY, sprint.getId()))
                .thenReturn(Optional.of(sprint));
        when(sprintIssueRepository.findAllBySprintId(sprint.getId()))
                .thenReturn(List.of(doneIssue));
        when(sprintRepository.save(sprint)).thenReturn(sprint);

        sprintService.finishSprint(PROJECT_KEY, sprint.getId());

        verify(sprintIssueRepository, never()).deleteAll(any());
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

        verify(sprintIssueRepository, never()).findAllBySprintId(sprint.getId());
    }

    private Sprint plannedSprint() {
        final Sprint sprint = Sprint.builder()
                .id(UUID.randomUUID().toString())
                .project(project())
                .projectId(PROJECT_ID)
                .name("Sprint 1")
                .goal("Deliver feature")
                .state(SprintState.PLANNED)
                .build();
        return sprint;
    }

    private Project project() {
        final XiraUser owner = XiraUser.builder()
                .id("owner-id")
                .email("owner@example.com")
                .firstName("Owner")
                .lastName("User")
                .passwordHash("hash")
                .build();
        return Project.builder()
                .id(PROJECT_ID)
                .key(PROJECT_KEY)
                .name("Xira")
                .owner(owner)
                .build();
    }

    private SprintIssue sprintIssue(final String sprintId, final WorkflowStatus workflowStatus) {
        final Issue issue = Issue.builder()
                .id(UUID.randomUUID().toString())
                .project(project())
                .projectId(PROJECT_ID)
                .seqNo(1)
                .key("XIRA-1")
                .issueType(IssueType.STORY)
                .status(workflowStatus)
                .statusId(workflowStatus.getId())
                .priority(IssuePriority.MEDIUM)
                .reporter(project().getOwner())
                .reporterId("owner-id")
                .title("Issue")
                .description("Description")
                .build();
        return SprintIssue.builder()
                .sprintId(sprintId)
                .issueId(issue.getId())
                .issue(issue)
                .build();
    }

    private WorkflowStatus doneWorkflowStatus() {
        return workflowStatus("done-status", WorkflowStatusCategory.DONE);
    }

    private WorkflowStatus todoWorkflowStatus() {
        return workflowStatus("todo-status", WorkflowStatusCategory.TODO);
    }

    private WorkflowStatus workflowStatus(final String id, final WorkflowStatusCategory category) {
        return WorkflowStatus.builder()
                .id(id)
                .workflow(Workflow.builder().id("workflow-id").project(project()).build())
                .workflowId("workflow-id")
                .name("Status")
                .category(category)
                .statusOrder(1)
                .build();
    }
}
