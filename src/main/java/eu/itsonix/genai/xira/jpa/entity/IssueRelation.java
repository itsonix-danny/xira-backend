package eu.itsonix.genai.xira.jpa.entity;

import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Builder
@Entity
@IdClass(IssueRelationId.class)
public class IssueRelation {
    @Id
    @Column(name = "issue_id", nullable = false)
    private String issueId;

    @Id
    @Column(name = "related_issue_id", nullable = false)
    private String relatedIssueId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "issue_id", nullable = false, insertable = false, updatable = false)
    private Issue issue;

    @ManyToOne(optional = false)
    @JoinColumn(name = "related_issue_id", nullable = false, insertable = false, updatable = false)
    private Issue relatedIssue;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private IssueRelationType relationType;
}

