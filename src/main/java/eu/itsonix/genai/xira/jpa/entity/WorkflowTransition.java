package eu.itsonix.genai.xira.jpa.entity;

import jakarta.persistence.*;

import org.hibernate.annotations.UuidGenerator;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Builder
@Entity
public class WorkflowTransition {
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.AUTO)
    @Column(nullable = false)
    private String id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    @Column(name = "workflow_id", insertable = false, updatable = false)
    private String workflowId;

    @ManyToOne
    @JoinColumn(name = "from_status_id")
    private WorkflowStatus fromStatus;

    @Column(name = "from_status_id", insertable = false, updatable = false)
    private String fromStatusId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "to_status_id", nullable = false)
    private WorkflowStatus toStatus;

    @Column(name = "to_status_id", insertable = false, updatable = false)
    private String toStatusId;
}
