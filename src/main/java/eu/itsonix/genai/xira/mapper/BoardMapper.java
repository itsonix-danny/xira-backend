package eu.itsonix.genai.xira.mapper;

import eu.itsonix.genai.xira.jpa.entity.Board;
import eu.itsonix.genai.xira.web.model.ProjectBoardResponse;

public final class BoardMapper {

    private BoardMapper() {
    }

    public static ProjectBoardResponse toProjectBoardResponse(final Board board) {
        return new ProjectBoardResponse().name(board.getName())
                .boardNumber(board.getBoardNumber())
                .type(ProjectBoardResponse.TypeEnum.valueOf(board.getType().name()));
    }
}
