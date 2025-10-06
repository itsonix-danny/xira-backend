package eu.itsonix.genai.xira.jpa.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import eu.itsonix.genai.xira.jpa.entity.Issue;

@Repository
public interface IssueRepository extends JpaRepository<Issue, String> {

    Optional<Issue> findByKeyAndProjectKeyIgnoreCase(final String key, final String projectKey);

    @Query("SELECT COALESCE(MAX(i.seqNo), 0) FROM Issue i WHERE i.projectId = :projectId")
    Integer findMaxSeqNoByProjectId(final String projectId);
}
