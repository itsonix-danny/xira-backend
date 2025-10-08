package eu.itsonix.genai.xira.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import eu.itsonix.genai.xira.jpa.entity.SprintIssue;
import eu.itsonix.genai.xira.jpa.entity.SprintIssueId;
import eu.itsonix.genai.xira.jpa.entity.WorkflowStatusCategory;

@Repository
public interface SprintIssueRepository extends JpaRepository<SprintIssue, SprintIssueId> {

    void deleteAllBySprintId(final String sprintId);

    void deleteAllBySprintIdAndIssue_Status_CategoryNot(final String sprintId, final WorkflowStatusCategory category);

    boolean existsBySprintIdAndIssueId(final String sprintId, final String issueId);

    void deleteBySprintIdAndIssueId(final String sprintId, final String issueId);

    void deleteAllByIssueId(final String issueId);
}
