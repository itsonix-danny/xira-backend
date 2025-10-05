package eu.itsonix.genai.xira.service;

import jakarta.persistence.EntityNotFoundException;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import eu.itsonix.genai.xira.jpa.entity.Project;
import eu.itsonix.genai.xira.jpa.entity.Sprint;
import eu.itsonix.genai.xira.jpa.entity.SprintIssue;
import eu.itsonix.genai.xira.jpa.entity.SprintState;
import eu.itsonix.genai.xira.jpa.entity.WorkflowStatusCategory;
import eu.itsonix.genai.xira.jpa.repository.ProjectRepository;
import eu.itsonix.genai.xira.jpa.repository.SprintIssueRepository;
import eu.itsonix.genai.xira.jpa.repository.SprintRepository;
import eu.itsonix.genai.xira.mapper.SprintMapper;
import eu.itsonix.genai.xira.web.model.AddSprintRequest;
import eu.itsonix.genai.xira.web.model.SprintResponse;
import eu.itsonix.genai.xira.web.model.UpdateSprintRequest;

@Service
@RequiredArgsConstructor
public class SprintService {

    private final ProjectRepository projectRepository;
    private final SprintRepository sprintRepository;
    private final SprintIssueRepository sprintIssueRepository;

    @Transactional
    public SprintResponse createSprint(final String projectKey, final AddSprintRequest addSprintRequest) {
        final Project project = projectRepository.findByKeyIgnoreCase(projectKey)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));

        final Sprint sprint = Sprint.builder()
                .project(project)
                .name(addSprintRequest.getName())
                .goal(addSprintRequest.getGoal())
                .state(SprintState.PLANNED)
                .build();

        final Sprint savedSprint = sprintRepository.save(sprint);
        return SprintMapper.toSprintResponse(savedSprint);
    }

    @Transactional
    public SprintResponse updateSprint(final String projectKey, final String sprintId,
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

        final Sprint savedSprint = sprintRepository.save(sprint);
        return SprintMapper.toSprintResponse(savedSprint);
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
    public SprintResponse startSprint(final String projectKey, final String sprintId) {
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

        final Sprint savedSprint = sprintRepository.save(sprint);
        return SprintMapper.toSprintResponse(savedSprint);
    }

    @Transactional
    public SprintResponse finishSprint(final String projectKey, final String sprintId) {
        final Sprint sprint = getSprintForProject(projectKey, sprintId);

        if (sprint.getState() != SprintState.ACTIVE) {
            throw new IllegalStateException("Only active sprints can be finished");
        }

        final List<SprintIssue> sprintIssues = sprintIssueRepository.findAllBySprintId(sprint.getId());
        final List<SprintIssue> unfinishedIssues = sprintIssues.stream()
                .filter(sprintIssue -> sprintIssue.getIssue().getStatus().getCategory() != WorkflowStatusCategory.DONE)
                .toList();

        if (!unfinishedIssues.isEmpty()) {
            sprintIssueRepository.deleteAll(unfinishedIssues);
        }

        sprint.setState(SprintState.CLOSED);
        sprint.setFinishedAt(Instant.now());

        final Sprint savedSprint = sprintRepository.save(sprint);
        return SprintMapper.toSprintResponse(savedSprint);
    }

    private Sprint getSprintForProject(final String projectKey, final String sprintId) {
        return sprintRepository.findByProject_KeyIgnoreCaseAndId(projectKey, sprintId)
                .orElseThrow(() -> new EntityNotFoundException("Sprint not found"));
    }
}
