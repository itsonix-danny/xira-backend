package eu.itsonix.genai.xira.mapper;

import java.util.List;
import java.util.Map;

import eu.itsonix.genai.xira.jpa.entity.Board;
import eu.itsonix.genai.xira.jpa.entity.BoardColumn;
import eu.itsonix.genai.xira.jpa.entity.Issue;
import eu.itsonix.genai.xira.web.model.BoardColumnResponse;
import eu.itsonix.genai.xira.web.model.KanbanBoardDetailsResponse;
import eu.itsonix.genai.xira.web.model.ProjectBoardResponse;

public final class BoardMapper {

    private BoardMapper() {
    }

    public static ProjectBoardResponse toProjectBoardResponse(final Board board) {
        return new ProjectBoardResponse().name(board.getName())
                .boardNumber(board.getBoardNumber())
                .type(ProjectBoardResponse.TypeEnum.valueOf(board.getType().name()));
    }

    public static KanbanBoardDetailsResponse toKanbanBoardDetailsResponse(final Board board,
            final Map<BoardColumn, List<Issue>> issuesByColumn) {
        return new KanbanBoardDetailsResponse().name(board.getName())
                .type(KanbanBoardDetailsResponse.TypeEnum.KANBAN)
                .columns(issuesByColumn.entrySet().stream()
                        .map(entry -> toBoardColumnResponse(entry.getKey(), entry.getValue()))
                        .toList());
    }

    private static BoardColumnResponse toBoardColumnResponse(final BoardColumn column, final List<Issue> issues) {
        return new BoardColumnResponse().name(column.getName())
                .issues(issues.stream().map(IssueMapper::toIssueSummaryResponse).toList());
    }
}
