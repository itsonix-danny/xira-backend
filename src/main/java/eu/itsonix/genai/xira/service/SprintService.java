package eu.itsonix.genai.xira.service;

import jakarta.persistence.EntityNotFoundException;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import eu.itsonix.genai.xira.jpa.entity.*;
import eu.itsonix.genai.xira.jpa.repository.IssueRepository;
import eu.itsonix.genai.xira.jpa.repository.ProjectRepository;
import eu.itsonix.genai.xira.jpa.repository.SprintIssueRepository;
import eu.itsonix.genai.xira.jpa.repository.SprintRepository;
import eu.itsonix.genai.xira.web.model.AddSprintRequest;
import eu.itsonix.genai.xira.web.model.UpdateSprintRequest;

@Service
@RequiredArgsConstructor
public class SprintService {

    private final ProjectRepository projectRepository;
    private final SprintRepository sprintRepository;
    private final SprintIssueRepository sprintIssueRepository;
    private final IssueRepository issueRepository;

    @Transactional
    public String createSprint(final String projectKey, final AddSprintRequest addSprintRequest) {
        final Project project = projectRepository.findByKeyIgnoreCase(projectKey)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));

        final Sprint sprint = sprintRepository.save(Sprint.builder()
                .project(project)
                .name(addSprintRequest.getName())
                .goal(addSprintRequest.getGoal())
                .state(SprintState.PLANNED)
                .build());

        return sprint.getId();
    }

    @Transactional
    public void updateSprint(final String projectKey, final String sprintId,
            final UpdateSprintRequest updateSprintRequest) {
        final Sprint sprint = getSprintForProject(projectKey, sprintId);

        if (sprint.getState() == SprintState.CLOSED) {
            throw new IllegalStateException("Finished sprints cannot be updated");
        }

        if (updateSprintRequest.getName() != null) {
            sprint.setName(updateSprintRequest.getName());
        }

        if (updateSprintRequest.getGoal() != null) {
            sprint.setGoal(updateSprintRequest.getGoal());
        }

        sprintRepository.save(sprint);
    }

    @Transactional
    public void deleteSprint(final String projectKey, final String sprintId) {
        final Sprint sprint = getSprintForProject(projectKey, sprintId);

        if (sprint.getState() != SprintState.PLANNED) {
            throw new IllegalStateException("Only planned sprints can be deleted");
        }

        sprintIssueRepository.deleteAllBySprintId(sprint.getId());
        sprintRepository.delete(sprint);
    }

    @Transactional
    public void startSprint(final String projectKey, final String sprintId) {
        final Sprint sprint = getSprintForProject(projectKey, sprintId);

        if (sprint.getState() == SprintState.CLOSED) {
            throw new IllegalStateException("Finished sprints cannot be started");
        }

        if (sprint.getState() == SprintState.ACTIVE) {
            throw new IllegalStateException("Sprint is already active");
        }

        if (sprintRepository.existsByProjectIdAndStateAndIdNot(sprint.getProjectId(), SprintState.ACTIVE,
                sprint.getId())) {
            throw new IllegalStateException("Another sprint is already active for this project");
        }

        sprint.setState(SprintState.ACTIVE);
        sprint.setStartedAt(Instant.now());
        sprint.setFinishedAt(null);

        sprintRepository.save(sprint);
    }

    @Transactional
    public void finishSprint(final String projectKey, final String sprintId) {
        final Sprint sprint = getSprintForProject(projectKey, sprintId);

        if (sprint.getState() != SprintState.ACTIVE) {
            throw new IllegalStateException("Only active sprints can be finished");
        }

        sprintIssueRepository.deleteAllBySprintIdAndIssue_Status_CategoryNot(sprint.getId(),
                WorkflowStatusCategory.DONE);

        sprint.setState(SprintState.CLOSED);
        sprint.setFinishedAt(Instant.now());

        sprintRepository.save(sprint);
    }

    @Transactional
    public void addIssueToSprint(final String projectKey, final String sprintId, final String issueKey) {
        final Sprint sprint = getSprintForProject(projectKey, sprintId);

        if (sprint.getState() == SprintState.CLOSED) {
            throw new IllegalStateException("Cannot add issues to a finished sprint");
        }

        final Issue issue = issueRepository.findByKeyAndProjectKeyIgnoreCase(issueKey, projectKey)
                .orElseThrow(() -> new EntityNotFoundException("Issue not found"));

        if (sprintIssueRepository.existsBySprintIdAndIssueId(sprint.getId(), issue.getId())) {
            throw new IllegalStateException("Issue is already in this sprint");
        }

        sprintIssueRepository.deleteAllByIssueId(issue.getId());

        sprintIssueRepository.save(SprintIssue.builder().sprintId(sprint.getId()).issueId(issue.getId()).build());
    }

    @Transactional
    public void removeIssueFromSprint(final String projectKey, final String sprintId, final String issueKey) {
        final Sprint sprint = getSprintForProject(projectKey, sprintId);

        if (sprint.getState() == SprintState.CLOSED) {
            throw new IllegalStateException("Cannot remove issues from a finished sprint");
        }

        final Issue issue = issueRepository.findByKeyAndProjectKeyIgnoreCase(issueKey, projectKey)
                .orElseThrow(() -> new EntityNotFoundException("Issue not found"));

        if (!sprintIssueRepository.existsBySprintIdAndIssueId(sprint.getId(), issue.getId())) {
            throw new EntityNotFoundException("Issue is not in this sprint");
        }

        sprintIssueRepository.deleteBySprintIdAndIssueId(sprint.getId(), issue.getId());
    }

    private Sprint getSprintForProject(final String projectKey, final String sprintId) {
        return sprintRepository.findByProject_KeyIgnoreCaseAndId(projectKey, sprintId)
                .orElseThrow(() -> new EntityNotFoundException("Sprint not found"));
    }
}
