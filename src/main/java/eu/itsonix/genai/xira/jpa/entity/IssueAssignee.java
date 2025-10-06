package eu.itsonix.genai.xira.jpa.entity;

import jakarta.persistence.*;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Builder
@Entity
@IdClass(IssueAssigneeId.class)
@EntityListeners(AuditingEntityListener.class)
public class IssueAssignee {
    @Id
    @Column(name = "issue_id", nullable = false)
    private String issueId;

    @Id
    @Column(name = "user_id", nullable = false)
    private String userId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "issue_id", insertable = false, updatable = false)
    private Issue issue;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private XiraUser xiraUser;

    @CreatedDate
    @Column(nullable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;
}
