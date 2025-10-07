package eu.itsonix.genai.xira.mapper;

import java.util.Comparator;

import eu.itsonix.genai.xira.jpa.entity.Issue;
import eu.itsonix.genai.xira.jpa.entity.IssueAssignee;
import eu.itsonix.genai.xira.jpa.entity.XiraUser;
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
}
