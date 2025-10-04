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
public class BoardColumn {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.AUTO)
    @Column(nullable = false)
    private String id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @Column(name = "board_id", insertable = false, updatable = false)
    private String boardId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer columnOrder;

    @CreatedDate
    @Column(nullable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;
}