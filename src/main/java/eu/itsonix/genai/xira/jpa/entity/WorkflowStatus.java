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
public class WorkflowStatus {
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

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private WorkflowStatusCategory category;

    @Column(name = "\"order\"", nullable = false)
    private Integer order;
}
