package eu.itsonix.genai.xira.mapper;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import eu.itsonix.genai.xira.jpa.entity.BoardColumn;
import eu.itsonix.genai.xira.jpa.entity.Issue;
import eu.itsonix.genai.xira.jpa.entity.Sprint;
import eu.itsonix.genai.xira.jpa.entity.SprintIssue;
import eu.itsonix.genai.xira.web.model.ActiveSprintResponse;
import eu.itsonix.genai.xira.web.model.BoardColumnResponse;
import eu.itsonix.genai.xira.web.model.SprintState;
import eu.itsonix.genai.xira.web.model.SprintWithIssuesResponse;

public final class SprintMapper {

    private SprintMapper() {
    }

    public static SprintWithIssuesResponse toSprintWithIssuesResponse(final Sprint sprint) {
        return new SprintWithIssuesResponse().id(UUID.fromString(sprint.getId()))
                .name(sprint.getName())
                .goal(sprint.getGoal())
                .state(eu.itsonix.genai.xira.web.model.SprintState.fromValue(sprint.getState().name()))
                .createdAt(toOffsetDateTime(sprint.getCreatedAt()))
                .startedAt(toOffsetDateTime(sprint.getStartedAt()))
                .finishedAt(toOffsetDateTime(sprint.getFinishedAt()))
                .issues(sprint.getSprintIssues()
                        .stream()
                        .map(SprintIssue::getIssue)
                        .sorted(Comparator.comparing(Issue::getSeqNo))
                        .map(IssueMapper::toIssueSummaryResponse)
                        .toList());
    }

    public static ActiveSprintResponse toActiveSprintResponse(final Sprint sprint,
            final Map<BoardColumn, List<Issue>> issuesByColumn) {
        return new ActiveSprintResponse().id(UUID.fromString(sprint.getId()))
                .name(sprint.getName())
                .goal(sprint.getGoal())
                .state(SprintState.fromValue(sprint.getState().name()))
                .createdAt(toOffsetDateTime(sprint.getCreatedAt()))
                .startedAt(toOffsetDateTime(sprint.getStartedAt()))
                .finishedAt(toOffsetDateTime(sprint.getFinishedAt()))
                .columns(issuesByColumn.entrySet()
                        .stream()
                        .map(entry -> toBoardColumnResponseForSprint(entry.getKey(), entry.getValue()))
                        .toList());
    }

    private static BoardColumnResponse toBoardColumnResponseForSprint(final BoardColumn column,
            final List<Issue> issues) {
        return new BoardColumnResponse().name(column.getName())
                .statuses(column.getBoardColumnWorkflowStatuses()
                        .stream()
                        .sorted(Comparator
                                .comparing(boardColumnWorkflowStatus -> boardColumnWorkflowStatus.getWorkflowStatus()
                                        .getStatusOrder()))
                        .map(boardColumnWorkflowStatus -> WorkflowMapper
                                .toWorkflowStatusResponse(boardColumnWorkflowStatus.getWorkflowStatus()))
                        .toList())
                .issues(issues.stream()
                        .sorted(Comparator.comparing(Issue::getSeqNo))
                        .map(IssueMapper::toIssueSummaryResponse)
                        .toList());
    }

    private static OffsetDateTime toOffsetDateTime(final Instant instant) {
        if (instant == null) {
            return null;
        }
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
