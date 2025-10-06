package eu.itsonix.genai.xira.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import eu.itsonix.genai.xira.jpa.entity.IssueAssignee;
import eu.itsonix.genai.xira.jpa.entity.IssueAssigneeId;

@Repository
public interface IssueAssigneeRepository extends JpaRepository<IssueAssignee, IssueAssigneeId> {

    boolean existsByIssueIdAndUserId(final String issueId, final String userId);

    @Modifying
    void deleteByIssueIdAndUserId(final String issueId, final String userId);
}
