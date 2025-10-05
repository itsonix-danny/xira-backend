package eu.itsonix.genai.xira.mapper;

import eu.itsonix.genai.xira.jpa.entity.Project;
import eu.itsonix.genai.xira.jpa.entity.ProjectMember;
import eu.itsonix.genai.xira.jpa.entity.ProjectRole;
import eu.itsonix.genai.xira.web.model.ProjectDetailsResponse;
import eu.itsonix.genai.xira.web.model.ProjectMemberRole;
import eu.itsonix.genai.xira.web.model.ProjectSummaryResponse;

public final class ProjectMapper {

    private ProjectMapper() {
    }

    public static ProjectSummaryResponse toProjectSummaryResponse(final ProjectMember projectMember) {
        final Project project = projectMember.getProject();
        return new ProjectSummaryResponse().key(project.getKey())
                .name(project.getName())
                .description(project.getDescription())
                .isOwner(isOwner(projectMember))
                .isAdmin(isAdmin(projectMember));
    }

    public static ProjectDetailsResponse toProjectDetailsResponse(final ProjectMember projectMember) {
        final Project project = projectMember.getProject();
        return new ProjectDetailsResponse().key(project.getKey())
                .name(project.getName())
                .description(project.getDescription())
                .isOwner(isOwner(projectMember))
                .isAdmin(isAdmin(projectMember))
                .boards(project.getBoards().stream().map(BoardMapper::toProjectBoardResponse).toList());
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
}
