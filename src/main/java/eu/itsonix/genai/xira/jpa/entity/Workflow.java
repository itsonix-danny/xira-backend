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
public class Workflow {
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
    private String name;
}
