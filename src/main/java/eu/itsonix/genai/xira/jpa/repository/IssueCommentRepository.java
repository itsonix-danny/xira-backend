package eu.itsonix.genai.xira.jpa.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import eu.itsonix.genai.xira.jpa.entity.IssueComment;

@Repository
public interface IssueCommentRepository extends JpaRepository<IssueComment, String> {

    Optional<IssueComment> findByIdAndIssueKeyAndIssueProjectKeyIgnoreCase(final String id, final String issueKey,
            final String projectKey);
}
