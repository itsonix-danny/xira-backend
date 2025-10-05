package eu.itsonix.genai.xira.mapper;

import java.util.List;

import eu.itsonix.genai.xira.jpa.entity.Board;
import eu.itsonix.genai.xira.jpa.entity.Project;
import eu.itsonix.genai.xira.jpa.entity.ProjectMember;
import eu.itsonix.genai.xira.jpa.entity.ProjectRole;
import eu.itsonix.genai.xira.web.model.ProjectBoardResponse;
import eu.itsonix.genai.xira.web.model.ProjectDetailsResponse;
import eu.itsonix.genai.xira.web.model.ProjectMemberRole;
import eu.itsonix.genai.xira.web.model.ProjectSummaryResponse;

public final class ProjectMapper {

    private ProjectMapper() {
    }

    public static ProjectSummaryResponse toProjectSummaryResponse(final Project project,
            final ProjectMember projectMember) {
        final boolean owner = isOwner(project, projectMember);
        final boolean admin = isAdmin(projectMember);

        return new ProjectSummaryResponse()
                .key(project.getKey())
                .name(project.getName())
                .description(project.getDescription())
                .isOwner(owner)
                .isAdmin(admin);
    }

    public static ProjectDetailsResponse toProjectDetailsResponse(final Project project,
            final ProjectMember projectMember, final List<Board> boards) {
        final boolean owner = isOwner(project, projectMember);
        final boolean admin = isAdmin(projectMember);
        final List<ProjectBoardResponse> boardResponses = boards.stream()
                .map(ProjectMapper::toProjectBoardResponse)
                .toList();

        return new ProjectDetailsResponse()
                .key(project.getKey())
                .name(project.getName())
                .description(project.getDescription())
                .isOwner(owner)
                .isAdmin(admin)
                .boards(boardResponses);
    }

    private static ProjectBoardResponse toProjectBoardResponse(final Board board) {
        return new ProjectBoardResponse()
                .name(board.getName())
                .boardNumber(board.getBoardNumber())
                .type(ProjectBoardResponse.TypeEnum.valueOf(board.getType().name()));
    }

    private static boolean isOwner(final Project project, final ProjectMember projectMember) {
        return project.getOwnerId() != null && project.getOwnerId().equals(projectMember.getUserId());
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
