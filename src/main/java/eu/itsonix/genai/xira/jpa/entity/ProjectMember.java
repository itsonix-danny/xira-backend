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
@IdClass(ProjectMemberId.class)
public class ProjectMember {
    @Id
    @Column(name = "project_id", nullable = false)
    private String projectId;

    @Id
    @Column(name = "user_id", nullable = false)
    private String userId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "project_id", insertable = false, updatable = false)
    private Project project;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private XiraUser xiraUser;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ProjectRole role;
}
