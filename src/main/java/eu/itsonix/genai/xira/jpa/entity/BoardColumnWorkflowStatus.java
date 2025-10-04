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
@IdClass(BoardColumnWorkflowStatusId.class)
@EntityListeners(AuditingEntityListener.class)
public class BoardColumnWorkflowStatus {
    @Id
    @Column(name = "board_column_id", nullable = false)
    private String boardColumnId;

    @Id
    @Column(name = "workflow_status_id", nullable = false)
    private String workflowStatusId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "board_column_id", insertable = false, updatable = false)
    private BoardColumn boardColumn;

    @ManyToOne(optional = false)
    @JoinColumn(name = "workflow_status_id", insertable = false, updatable = false)
    private WorkflowStatus workflowStatus;

    @Column(nullable = false)
    private Boolean isDefault;

    @CreatedDate
    @Column(nullable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;
}