package eu.itsonix.genai.xira.mapper;

import eu.itsonix.genai.xira.jpa.entity.ProjectRole;
import eu.itsonix.genai.xira.web.model.ProjectMemberRole;

public final class ProjectMapper {

    private ProjectMapper() {
    }

    public static ProjectRole toProjectRole(final ProjectMemberRole role) {
        if (role == null) {
            return null;
        }
        return ProjectRole.valueOf(role.getValue());
    }
}
