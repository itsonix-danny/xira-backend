package eu.itsonix.genai.xira.jpa.entity;

import java.io.Serializable;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class IssueAssigneeId implements Serializable {
    private String issueId;
    private String userId;
}
