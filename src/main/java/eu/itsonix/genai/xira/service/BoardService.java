package eu.itsonix.genai.xira.service;

import jakarta.persistence.EntityNotFoundException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import eu.itsonix.genai.xira.jpa.entity.*;
import eu.itsonix.genai.xira.jpa.entity.SprintState;
import eu.itsonix.genai.xira.jpa.repository.*;
import eu.itsonix.genai.xira.mapper.BoardMapper;
import eu.itsonix.genai.xira.mapper.IssueMapper;
import eu.itsonix.genai.xira.mapper.SprintMapper;
import eu.itsonix.genai.xira.web.model.*;

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
    private final SprintRepository sprintRepository;
    private final IssueRepository issueRepository;

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

    @Transactional(readOnly = true)
    public GetBoardDetails200Response getBoardDetails(final String projectKey, final Integer boardNumber) {
        final Board board = boardRepository.findByProjectKeyIgnoreCaseAndBoardNumber(projectKey, boardNumber)
                .orElseThrow(() -> new EntityNotFoundException("Board not found"));

        return board.getType() == BoardType.SCRUM ? getScrumBoardDetails(board) : getKanbanBoardDetails(board);
    }

    private ScrumBoardDetailsResponse getScrumBoardDetails(final Board board) {
        final SprintWithIssuesResponse activeSprintResponse = sprintRepository
                .findByProjectIdAndState(board.getProjectId(), SprintState.ACTIVE)
                .map(SprintMapper::toSprintWithIssuesResponse)
                .orElse(null);

        final List<SprintWithIssuesResponse> plannedSprintsResponse = sprintRepository
                .findByProjectIdAndStateOrderByCreatedAt(board.getProjectId(), SprintState.PLANNED)
                .stream()
                .map(SprintMapper::toSprintWithIssuesResponse)
                .toList();

        final List<IssueSummaryResponse> backlogIssues = issueRepository
                .findByProjectIdAndStatusCategoryNotAndSprintIssuesEmptyOrderBySeqNoAsc(board.getProjectId(),
                        WorkflowStatusCategory.DONE)
                .stream()
                .map(IssueMapper::toIssueSummaryResponse)
                .toList();

        return new ScrumBoardDetailsResponse().name(board.getName())
                .type(ScrumBoardDetailsResponse.TypeEnum.SCRUM)
                .activeSprint(activeSprintResponse)
                .plannedSprints(plannedSprintsResponse)
                .backlog(backlogIssues);
    }

    private KanbanBoardDetailsResponse getKanbanBoardDetails(final Board board) {
        final List<BoardColumn> columns = boardColumnRepository.findByBoardIdOrderByColumnOrder(board.getId());

        final List<String> boardWorkflowStatusIds = columns.stream()
                .flatMap(column -> column.getBoardColumnWorkflowStatuses().stream())
                .map(BoardColumnWorkflowStatus::getWorkflowStatusId)
                .distinct()
                .toList();

        final Map<String, List<Issue>> issuesByStatusId = issueRepository
                .findByStatusIdInOrderByCreatedAt(boardWorkflowStatusIds)
                .stream()
                .collect(Collectors.groupingBy(Issue::getStatusId));

        final Map<BoardColumn, List<Issue>> issuesByColumn = columns.stream()
                .collect(Collectors.toMap(Function.identity(),
                        column -> column.getBoardColumnWorkflowStatuses()
                                .stream()
                                .flatMap(boardColumnWorkflowStatus -> issuesByStatusId
                                        .getOrDefault(boardColumnWorkflowStatus.getWorkflowStatusId(), List.of())
                                        .stream())
                                .toList(),
                        (u, _) -> u, LinkedHashMap::new));

        return BoardMapper.toKanbanBoardDetailsResponse(board, issuesByColumn);
    }

    @Transactional(readOnly = true)
    public ActiveSprintResponse getActiveSprint(final String projectKey, final Integer boardNumber) {
        final Board board = boardRepository
                .findByProjectKeyIgnoreCaseAndBoardNumberAndType(projectKey, boardNumber, BoardType.SCRUM)
                .orElseThrow(() -> new EntityNotFoundException("Board not found or not a SCRUM board"));

        final Sprint activeSprint = sprintRepository.findByProjectIdAndState(board.getProjectId(), SprintState.ACTIVE)
                .orElse(null);

        if (activeSprint == null) {
            return null;
        }

        final Map<String, List<Issue>> issuesByStatusId = activeSprint.getSprintIssues()
                .stream()
                .map(SprintIssue::getIssue)
                .collect(Collectors.groupingBy(Issue::getStatusId));

        final Map<BoardColumn, List<Issue>> issuesByColumn = boardColumnRepository
                .findByBoardIdOrderByColumnOrder(board.getId())
                .stream()
                .collect(Collectors.toMap(Function.identity(),
                        column -> column.getBoardColumnWorkflowStatuses()
                                .stream()
                                .flatMap(boardColumnWorkflowStatus -> issuesByStatusId
                                        .getOrDefault(boardColumnWorkflowStatus.getWorkflowStatusId(), List.of())
                                        .stream())
                                .toList(),
                        (u, _) -> u, LinkedHashMap::new));

        return SprintMapper.toActiveSprintResponse(activeSprint, issuesByColumn);
    }
}
