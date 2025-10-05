package eu.itsonix.genai.xira.jpa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import eu.itsonix.genai.xira.jpa.entity.SprintIssue;
import eu.itsonix.genai.xira.jpa.entity.SprintIssueId;

@Repository
public interface SprintIssueRepository extends JpaRepository<SprintIssue, SprintIssueId> {

    @EntityGraph(attributePaths = { "issue", "issue.status" })
    List<SprintIssue> findAllBySprintId(String sprintId);

    void deleteAllBySprintId(String sprintId);
}
