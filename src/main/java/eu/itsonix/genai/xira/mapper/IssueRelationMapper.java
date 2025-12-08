package eu.itsonix.genai.xira.mapper;

import eu.itsonix.genai.xira.jpa.entity.IssueRelation;
import eu.itsonix.genai.xira.web.model.IssueRelationResponse;

public final class IssueRelationMapper {

    private IssueRelationMapper() {
    }

    public static IssueRelationResponse toIssueRelationResponse(final IssueRelation issueRelation) {
        return new IssueRelationResponse()
                .relatedIssueKey(issueRelation.getRelatedIssue().getKey())
                .relationType(IssueRelationResponse.RelationTypeEnum.fromValue(issueRelation.getRelationType().name()));
    }
}

