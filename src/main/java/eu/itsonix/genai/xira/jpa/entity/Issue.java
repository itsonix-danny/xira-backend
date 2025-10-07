package eu.itsonix.genai.xira.jpa.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString(exclude = { "issueAssignees", "sprintIssues", "comments" })
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

    @Column(nullable = false)
    private String title;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "issue", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<IssueAssignee> issueAssignees = new HashSet<>();

    @OneToMany(mappedBy = "issue", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<SprintIssue> sprintIssues = new HashSet<>();

    @OneToMany(mappedBy = "issue", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<IssueComment> comments = new HashSet<>();

    @CreatedDate
    @Column(nullable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    @Column
    private Instant closedAt;
}
