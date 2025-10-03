package eu.itsonix.genai.xira.jpa.entity;

import jakarta.persistence.*;

import java.time.Instant;

import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Builder
@Entity
@IdClass(SprintIssueId.class)
@EntityListeners(AuditingEntityListener.class)
public class SprintIssue {
    @Id
    @Column(name = "sprint_id", nullable = false)
    private String sprintId;

    @Id
    @Column(name = "issue_id", nullable = false)
    private String issueId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sprint_id", insertable = false, updatable = false)
    private Sprint sprint;

    @ManyToOne(optional = false)
    @JoinColumn(name = "issue_id", insertable = false, updatable = false)
    private Issue issue;

    @CreatedDate
    @Column(nullable = false)
    @ColumnDefault("now()")
    private Instant addedAt;
}
