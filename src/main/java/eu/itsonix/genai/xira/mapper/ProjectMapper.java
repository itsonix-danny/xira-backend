package eu.itsonix.genai.xira.mapper;

import java.util.List;
import java.util.UUID;

import eu.itsonix.genai.xira.jpa.entity.Project;
import eu.itsonix.genai.xira.jpa.entity.ProjectMember;
import eu.itsonix.genai.xira.jpa.entity.ProjectRole;
import eu.itsonix.genai.xira.jpa.entity.XiraUser;
import eu.itsonix.genai.xira.web.model.ProjectBoardResponse;
import eu.itsonix.genai.xira.web.model.ProjectDetailsResponse;
import eu.itsonix.genai.xira.web.model.ProjectMemberResponse;
import eu.itsonix.genai.xira.web.model.ProjectMemberRole;

public final class ProjectMapper {

    private ProjectMapper() {
    }

    public static ProjectDetailsResponse toProjectDetailsResponse(final ProjectMember projectMember) {
        final Project project = projectMember.getProject();
        final List<ProjectBoardResponse> boards = project.getBoards() != null
                ? project.getBoards().stream().map(BoardMapper::toProjectBoardResponse).toList()
                : List.of();
        return new ProjectDetailsResponse().key(project.getKey())
                .name(project.getName())
                .description(project.getDescription())
                .isOwner(isOwner(projectMember))
                .isAdmin(isAdmin(projectMember))
                .boards(boards);
    }

    private static boolean isOwner(final ProjectMember projectMember) {
        return projectMember.getProject().getOwnerId().equals(projectMember.getUserId());
    }

    private static boolean isAdmin(final ProjectMember projectMember) {
        return projectMember.getRole() == ProjectRole.ADMIN;
    }

    public static ProjectRole toProjectRole(final ProjectMemberRole role) {
        if (role == null) {
            return null;
        }
        return ProjectRole.valueOf(role.getValue());
    }

    public static ProjectMemberResponse toProjectMemberResponse(final ProjectMember projectMember) {
        final XiraUser user = projectMember.getXiraUser();
        return new ProjectMemberResponse().id(UUID.fromString(user.getId()))
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(toProjectMemberRole(projectMember.getRole()))
                .isOwner(isOwner(projectMember));
    }

    private static ProjectMemberRole toProjectMemberRole(final ProjectRole role) {
        if (role == null) {
            return null;
        }
        return ProjectMemberRole.fromValue(role.name());
    }
}
