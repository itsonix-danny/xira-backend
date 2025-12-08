package eu.itsonix.genai.xira.jpa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import eu.itsonix.genai.xira.jpa.entity.IssueRelation;
import eu.itsonix.genai.xira.jpa.entity.IssueRelationId;

public interface IssueRelationRepository extends JpaRepository<IssueRelation, IssueRelationId> {
    List<IssueRelation> findByIssueId(String issueId);

    @Query("SELECT ir FROM IssueRelation ir WHERE ir.issueId = :issueId AND ir.relatedIssueId = :relatedIssueId")
    Optional<IssueRelation> findByIssueIdAndRelatedIssueId(@Param("issueId") String issueId,
            @Param("relatedIssueId") String relatedIssueId);

    void deleteByIssueIdAndRelatedIssueId(String issueId, String relatedIssueId);

    boolean existsByIssueIdAndRelatedIssueId(String issueId, String relatedIssueId);
}

