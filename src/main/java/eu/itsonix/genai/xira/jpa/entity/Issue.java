package eu.itsonix.genai.xira.jpa.entity;

import jakarta.persistence.*;

import java.time.Instant;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.UuidGenerator;
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
@EntityListeners(AuditingEntityListener.class)
@Table(indexes = { @Index(columnList = "project_id, seq_no", unique = true) })
public class Issue {
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.AUTO)
    @Column(nullable = false)
    private String id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "project_id", insertable = false, updatable = false)
    private String projectId;

    @Column(nullable = false)
    private Integer seqNo;

    @Column(nullable = false, unique = true)
    private String key;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private IssueType issueType;

    @ManyToOne(optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private WorkflowStatus status;

    @Column(name = "status_id", insertable = false, updatable = false)
    private String statusId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private IssuePriority priority;

    @ManyToOne(optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private XiraUser reporter;

    @Column(name = "reporter_id", insertable = false, updatable = false)
    private String reporterId;

    @ManyToOne
    @JoinColumn(name = "assignee_id")
    private XiraUser assignee;

    @Column(name = "assignee_id", insertable = false, updatable = false)
    private String assigneeId;

    @Column(nullable = false)
    private String title;

    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    @ColumnDefault("''")
    private String description;

    @CreatedDate
    @Column(nullable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    @Column
    private Instant closedAt;
}
