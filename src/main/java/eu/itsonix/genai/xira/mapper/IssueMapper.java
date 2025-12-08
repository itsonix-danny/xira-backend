package eu.itsonix.genai.xira.mapper;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.Objects;

import eu.itsonix.genai.xira.jpa.entity.*;
import eu.itsonix.genai.xira.web.model.IssueDetailsResponse;
import eu.itsonix.genai.xira.web.model.IssueSummaryResponse;

public final class IssueMapper {

    private IssueMapper() {
    }

    public static IssueSummaryResponse toIssueSummaryResponse(final Issue issue) {
        return new IssueSummaryResponse().key(issue.getKey())
                .title(issue.getTitle())
                .issueType(IssueSummaryResponse.IssueTypeEnum.fromValue(issue.getIssueType().name()))
                .statusCategory(
                        IssueSummaryResponse.StatusCategoryEnum.fromValue(issue.getStatus().getCategory().name()))
                .priority(IssueSummaryResponse.PriorityEnum.fromValue(issue.getPriority().name()))
                .assignees(issue.getIssueAssignees()
                        .stream()
                        .map(IssueAssignee::getXiraUser)
                        .sorted(Comparator.comparing(XiraUser::getFirstName))
                        .map(UserMapper::toUserResponse)
                        .toList());
    }

    public static IssueDetailsResponse toIssueDetailsResponse(final Issue issue) {
        final String sprintName = issue.getSprintIssues()
                .stream()
                .map(SprintIssue::getSprint)
                .filter(Objects::nonNull)
                .findFirst()
                .map(Sprint::getName)
                .orElse(null);

        return new IssueDetailsResponse().key(issue.getKey())
                .title(issue.getTitle())
                .description(issue.getDescription())
                .issueType(IssueDetailsResponse.IssueTypeEnum.fromValue(issue.getIssueType().name()))
                .status(WorkflowMapper.toWorkflowStatusResponse(issue.getStatus()))
                .priority(IssueDetailsResponse.PriorityEnum.fromValue(issue.getPriority().name()))
                .assignees(issue.getIssueAssignees()
                        .stream()
                        .map(IssueAssignee::getXiraUser)
                        .sorted(Comparator.comparing(XiraUser::getFirstName))
                        .map(UserMapper::toUserResponse)
                        .toList())
                .sprintName(sprintName)
                .reporter(UserMapper.toUserResponse(issue.getReporter()))
                .createdAt(OffsetDateTime.ofInstant(issue.getCreatedAt(), ZoneOffset.UTC))
                .closedAt(issue.getClosedAt() != null ? OffsetDateTime.ofInstant(issue.getClosedAt(), ZoneOffset.UTC)
                        : null)
                .relations(issue.getIssueRelations()
                        .stream()
                        .map(IssueRelationMapper::toIssueRelationResponse)
                        .toList())
                .comments(issue.getComments()
                        .stream()
                        .sorted(Comparator.comparing(IssueComment::getCreatedAt))
                        .map(CommentMapper::toCommentResponse)
                        .toList());
    }
}
