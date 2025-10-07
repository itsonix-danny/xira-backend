package eu.itsonix.genai.xira.jpa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import eu.itsonix.genai.xira.jpa.entity.Issue;
import eu.itsonix.genai.xira.jpa.entity.WorkflowStatusCategory;

@Repository
public interface IssueRepository extends JpaRepository<Issue, String>, JpaSpecificationExecutor<Issue> {

    Optional<Issue> findByKeyAndProjectKeyIgnoreCase(final String key, final String projectKey);

    @EntityGraph(attributePaths = { "status", "reporter", "issueAssignees", "issueAssignees.xiraUser", "sprintIssues",
            "sprintIssues.sprint", "comments", "comments.author" })
    Optional<Issue> findWithDetailsByKeyAndProjectKeyIgnoreCase(final String key, final String projectKey);

    @Query("SELECT COALESCE(MAX(i.seqNo), 0) FROM Issue i WHERE i.projectId = :projectId")
    Integer findMaxSeqNoByProjectId(final String projectId);

    @EntityGraph(attributePaths = { "status", "issueAssignees", "issueAssignees.xiraUser" })
    List<Issue> findByProjectIdAndStatusCategoryNotAndSprintIssuesEmptyOrderBySeqNoAsc(final String projectId,
            final WorkflowStatusCategory doneCategory);

    @EntityGraph(attributePaths = { "status", "issueAssignees", "issueAssignees.xiraUser" })
    List<Issue> findByStatusIdInOrderByCreatedAt(final List<String> statusIds);

    @EntityGraph(attributePaths = { "status", "issueAssignees", "issueAssignees.xiraUser", "project" })
    List<Issue> findAll(final Specification<Issue> spec);
}
