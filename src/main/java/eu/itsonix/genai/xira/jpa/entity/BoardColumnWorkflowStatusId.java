package eu.itsonix.genai.xira.jpa.entity;

import java.io.Serializable;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class BoardColumnWorkflowStatusId implements Serializable {
    private String boardColumnId;
    private String workflowStatusId;
}