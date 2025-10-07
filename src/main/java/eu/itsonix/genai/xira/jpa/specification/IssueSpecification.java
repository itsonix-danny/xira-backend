package eu.itsonix.genai.xira.jpa.specification;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.data.jpa.domain.Specification;

import eu.itsonix.genai.xira.jpa.entity.Issue;
import eu.itsonix.genai.xira.jpa.entity.IssueAssignee;
import eu.itsonix.genai.xira.jpa.entity.ProjectMember;

public final class IssueSpecification {

    private IssueSpecification() {
    }

    public static Specification<Issue> getIssuesForUser(final String userId, final String projectKey,
            final String assigneeId, final Boolean includeFinished, final String search) {
        return (root, query, cb) -> {
            final List<Predicate> predicates = new ArrayList<>();

            final Subquery<String> projectSubquery = Objects.requireNonNull(query).subquery(String.class);
            final Root<ProjectMember> pmRoot = projectSubquery.from(ProjectMember.class);
            projectSubquery.select(pmRoot.get("project").get("id")).where(cb.equal(pmRoot.get("userId"), userId));

            predicates.add(root.get("projectId").in(projectSubquery));

            if (projectKey != null) {
                predicates.add(cb.equal(cb.lower(root.get("project").get("key")), projectKey.toLowerCase()));
            }

            if (assigneeId != null) {
                final Subquery<String> assigneeSubquery = query.subquery(String.class);
                final Root<IssueAssignee> iaRoot = assigneeSubquery.from(IssueAssignee.class);
                assigneeSubquery.select(iaRoot.get("issueId")).where(cb.equal(iaRoot.get("userId"), assigneeId));

                predicates.add(root.get("id").in(assigneeSubquery));
            }

            if (includeFinished == null || !includeFinished) {
                predicates.add(cb.isNull(root.get("closedAt")));
            }

            if (search != null && !search.isBlank()) {
                final String searchPattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(cb.like(cb.lower(root.get("key")), searchPattern),
                        cb.like(cb.lower(root.get("title")), searchPattern)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
