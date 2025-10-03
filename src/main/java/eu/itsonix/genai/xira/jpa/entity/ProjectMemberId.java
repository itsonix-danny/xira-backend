package eu.itsonix.genai.xira.jpa.entity;

import java.io.Serializable;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class ProjectMemberId implements Serializable {
    private String projectId;
    private String userId;
}
