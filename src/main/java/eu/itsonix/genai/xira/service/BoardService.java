package eu.itsonix.genai.xira.service;

import jakarta.persistence.EntityNotFoundException;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import eu.itsonix.genai.xira.jpa.entity.*;
import eu.itsonix.genai.xira.jpa.repository.BoardColumnRepository;
import eu.itsonix.genai.xira.jpa.repository.BoardColumnWorkflowStatusRepository;
import eu.itsonix.genai.xira.jpa.repository.BoardRepository;
import eu.itsonix.genai.xira.jpa.repository.ProjectRepository;
import eu.itsonix.genai.xira.web.model.AddBoardRequest;
import eu.itsonix.genai.xira.web.model.UpdateBoardRequest;

@Service
@RequiredArgsConstructor
public class BoardService {

    private static final String COLUMN_TODO = "To Do";
    private static final String COLUMN_IN_PROGRESS = "In Progress";
    private static final String COLUMN_DONE = "Done";

    private final BoardRepository boardRepository;
    private final ProjectRepository projectRepository;
    private final WorkflowService workflowService;
    private final BoardColumnRepository boardColumnRepository;
    private final BoardColumnWorkflowStatusRepository boardColumnWorkflowStatusRepository;

    @Transactional
    public String addBoard(final String projectKey, final AddBoardRequest addBoardRequest) {
        final Project project = projectRepository.findByKeyIgnoreCase(projectKey)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));

        if (addBoardRequest.getType() == AddBoardRequest.TypeEnum.SCRUM
                && boardRepository.existsByProjectIdAndType(project.getId(), BoardType.SCRUM)) {
            throw new IllegalStateException("Project already has a Scrum board");
        }

        final Board board = boardRepository.save(Board.builder()
                .name(addBoardRequest.getName())
                .boardNumber(boardRepository.countByProjectId(project.getId()) + 1)
                .type(BoardType.valueOf(addBoardRequest.getType().getValue()))
                .project(project)
                .build());

        final BoardColumn todoColumn = boardColumnRepository
                .save(BoardColumn.builder().board(board).name(COLUMN_TODO).columnOrder(1).build());
        final BoardColumn inProgressColumn = boardColumnRepository
                .save(BoardColumn.builder().board(board).name(COLUMN_IN_PROGRESS).columnOrder(2).build());
        final BoardColumn doneColumn = boardColumnRepository
                .save(BoardColumn.builder().board(board).name(COLUMN_DONE).columnOrder(3).build());

        createDefaultBoardColumnWorkflowStatus(project, todoColumn, inProgressColumn, doneColumn);

        return board.getBoardNumber().toString();
    }

    private void createDefaultBoardColumnWorkflowStatus(final Project project, final BoardColumn todoColumn,
            final BoardColumn inProgressColumn, final BoardColumn doneColumn) {
        final Map<WorkflowStatusCategory, WorkflowStatus> statusesByCategory = workflowService
                .getDefaultStatusesByCategory(project.getId());
        final WorkflowStatus todoStatus = statusesByCategory.get(WorkflowStatusCategory.TODO);
        final WorkflowStatus inProgressStatus = statusesByCategory.get(WorkflowStatusCategory.IN_PROGRESS);
        final WorkflowStatus doneStatus = statusesByCategory.get(WorkflowStatusCategory.DONE);

        boardColumnWorkflowStatusRepository.save(BoardColumnWorkflowStatus.builder()
                .boardColumnId(todoColumn.getId())
                .workflowStatusId(todoStatus.getId())
                .boardColumn(todoColumn)
                .workflowStatus(todoStatus)
                .isDefault(true)
                .build());

        boardColumnWorkflowStatusRepository.save(BoardColumnWorkflowStatus.builder()
                .boardColumnId(inProgressColumn.getId())
                .workflowStatusId(inProgressStatus.getId())
                .boardColumn(inProgressColumn)
                .workflowStatus(inProgressStatus)
                .isDefault(true)
                .build());

        boardColumnWorkflowStatusRepository.save(BoardColumnWorkflowStatus.builder()
                .boardColumnId(doneColumn.getId())
                .workflowStatusId(doneStatus.getId())
                .boardColumn(doneColumn)
                .workflowStatus(doneStatus)
                .isDefault(true)
                .build());
    }

    @Transactional
    public void updateBoard(final String projectKey, final Integer boardNumber,
            final UpdateBoardRequest updateBoardRequest) {
        final Board board = boardRepository.findByProjectKeyIgnoreCaseAndBoardNumber(projectKey, boardNumber)
                .orElseThrow(() -> new EntityNotFoundException("Board not found"));

        if (updateBoardRequest.getName() != null) {
            board.setName(updateBoardRequest.getName());
        }

        boardRepository.save(board);
    }
}
