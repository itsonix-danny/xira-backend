package eu.itsonix.genai.xira.jpa.entity;

import jakarta.persistence.*;

import java.time.Instant;

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
public class Project {
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.AUTO)
    @Column(nullable = false)
    private String id;

    @Column(nullable = false, unique = true)
    private String key;

    @Column(nullable = false)
    private String name;

    @Column(length = 200)
    private String description;

    @ManyToOne(optional = false)
    @JoinColumn(name = "lead_user_id", nullable = false)
    private XiraUser owner;

    @Column(name = "lead_user_id", insertable = false, updatable = false)
    private String ownerId;

    @OneToOne(mappedBy = "project", cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    private Workflow workflow;

    @CreatedDate
    @Column(nullable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;
}
