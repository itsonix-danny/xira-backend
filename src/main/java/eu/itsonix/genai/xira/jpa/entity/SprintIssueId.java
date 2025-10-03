package eu.itsonix.genai.xira.jpa.entity;

import java.io.Serializable;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class SprintIssueId implements Serializable {
    private String sprintId;
    private String issueId;
}
