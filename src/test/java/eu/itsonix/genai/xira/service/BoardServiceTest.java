package eu.itsonix.genai.xira.service;

import jakarta.persistence.EntityNotFoundException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.itsonix.genai.xira.jpa.entity.*;
import eu.itsonix.genai.xira.jpa.repository.*;
import eu.itsonix.genai.xira.web.model.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private WorkflowService workflowService;

    @Mock
    private BoardColumnRepository boardColumnRepository;

    @Mock
    private BoardColumnWorkflowStatusRepository boardColumnWorkflowStatusRepository;

    @Mock
    private SprintRepository sprintRepository;

    @Mock
    private IssueRepository issueRepository;

    @InjectMocks
    private BoardService boardService;

    @Test
    void givenValidRequest_whenAddScrumBoard_thenCreatesBoard() {
        final Project project = Project.builder().id("project-id").key("XIRA").name("Xira").build();

        when(projectRepository.findByKeyIgnoreCase("XIRA")).thenReturn(Optional.of(project));
        when(boardRepository.existsByProjectIdAndType("project-id", BoardType.SCRUM)).thenReturn(false);
        when(boardRepository.countByProjectId("project-id")).thenReturn(0);

        final Board savedBoard = Board.builder()
                .id("board-id")
                .name("Scrum Board")
                .boardNumber(1)
                .type(BoardType.SCRUM)
                .project(project)
                .build();
        when(boardRepository.save(any(Board.class))).thenReturn(savedBoard);

        mockWorkflowStatusesAndColumns();

        final String boardNumber = boardService.addBoard("XIRA",
                new AddBoardRequest().name("Scrum Board").type(AddBoardRequest.TypeEnum.SCRUM));

        assertThat(boardNumber).isEqualTo("1");
        verify(boardRepository).save(any(Board.class));
        verify(boardColumnRepository, times(3)).save(any(BoardColumn.class));
        verify(boardColumnWorkflowStatusRepository, times(3)).save(any(BoardColumnWorkflowStatus.class));
    }

    @Test
    void givenValidRequest_whenAddKanbanBoard_thenCreatesBoard() {
        final Project project = Project.builder().id("project-id").key("XIRA").name("Xira").build();

        when(projectRepository.findByKeyIgnoreCase("XIRA")).thenReturn(Optional.of(project));
        when(boardRepository.countByProjectId("project-id")).thenReturn(0);

        final Board savedBoard = Board.builder()
                .id("board-id")
                .name("Kanban Board")
                .boardNumber(1)
                .type(BoardType.KANBAN)
                .project(project)
                .build();
        when(boardRepository.save(any(Board.class))).thenReturn(savedBoard);

        mockWorkflowStatusesAndColumns();

        final String boardNumber = boardService.addBoard("XIRA",
                new AddBoardRequest().name("Kanban Board").type(AddBoardRequest.TypeEnum.KANBAN));

        assertThat(boardNumber).isEqualTo("1");
        verify(boardRepository).save(any(Board.class));
        verify(boardColumnRepository, times(3)).save(any(BoardColumn.class));
        verify(boardColumnWorkflowStatusRepository, times(3)).save(any(BoardColumnWorkflowStatus.class));
    }

    @Test
    void givenExistingBoard_whenAddBoard_thenAssignsNextBoardNumber() {
        final Project project = Project.builder().id("project-id").key("XIRA").name("Xira").build();

        when(projectRepository.findByKeyIgnoreCase("XIRA")).thenReturn(Optional.of(project));
        when(boardRepository.countByProjectId("project-id")).thenReturn(2);

        final Board savedBoard = Board.builder()
                .id("board-id")
                .name("Third Board")
                .boardNumber(3)
                .type(BoardType.KANBAN)
                .project(project)
                .build();
        when(boardRepository.save(any(Board.class))).thenReturn(savedBoard);

        mockWorkflowStatusesAndColumns();

        final String boardNumber = boardService.addBoard("XIRA",
                new AddBoardRequest().name("Third Board").type(AddBoardRequest.TypeEnum.KANBAN));

        assertThat(boardNumber).isEqualTo("3");
        verify(boardRepository).save(any(Board.class));
        verify(boardColumnRepository, times(3)).save(any(BoardColumn.class));
        verify(boardColumnWorkflowStatusRepository, times(3)).save(any(BoardColumnWorkflowStatus.class));
    }

    @Test
    void givenUnknownProject_whenAddBoard_thenThrowsEntityNotFound() {
        when(projectRepository.findByKeyIgnoreCase("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> boardService.addBoard("UNKNOWN",
                new AddBoardRequest().name("Kanban Board").type(AddBoardRequest.TypeEnum.KANBAN)))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Project not found");

        verify(boardRepository, never()).save(any(Board.class));
    }

    @Test
    void givenExistingScrumBoard_whenAddScrumBoard_thenThrowsIllegalState() {
        final Project project = Project.builder().id("project-id").key("XIRA").name("Xira").build();

        when(projectRepository.findByKeyIgnoreCase("XIRA")).thenReturn(Optional.of(project));
        when(boardRepository.existsByProjectIdAndType("project-id", BoardType.SCRUM)).thenReturn(true);

        assertThatThrownBy(() -> boardService.addBoard("XIRA",
                new AddBoardRequest().name("Scrum Board").type(AddBoardRequest.TypeEnum.SCRUM)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Project already has a Scrum board");

        verify(boardRepository, never()).save(any(Board.class));
    }

    @Test
    void givenValidRequest_whenUpdateBoard_thenUpdatesBoard() {
        final Project project = Project.builder().id("project-id").key("XIRA").build();

        final Board existingBoard = Board.builder()
                .id("board-id")
                .name("Old Name")
                .boardNumber(1)
                .type(BoardType.KANBAN)
                .project(project)
                .build();

        when(boardRepository.findByProjectKeyIgnoreCaseAndBoardNumber("XIRA", 1))
                .thenReturn(Optional.of(existingBoard));

        boardService.updateBoard("XIRA", 1, new UpdateBoardRequest().name("Updated Name"));

        verify(boardRepository).save(any(Board.class));
    }

    @Test
    void givenNonExistentBoard_whenUpdateBoard_thenThrowsEntityNotFound() {
        when(boardRepository.findByProjectKeyIgnoreCaseAndBoardNumber("XIRA", 99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> boardService.updateBoard("XIRA", 99, new UpdateBoardRequest().name("New Name")))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Board not found");
    }

    private void mockWorkflowStatusesAndColumns() {
        final WorkflowStatus todoStatus = WorkflowStatus.builder()
                .id("status-1")
                .name("To Do")
                .category(WorkflowStatusCategory.TODO)
                .statusOrder(1)
                .build();
        final WorkflowStatus inProgressStatus = WorkflowStatus.builder()
                .id("status-2")
                .name("In Progress")
                .category(WorkflowStatusCategory.IN_PROGRESS)
                .statusOrder(2)
                .build();
        final WorkflowStatus doneStatus = WorkflowStatus.builder()
                .id("status-3")
                .name("Done")
                .category(WorkflowStatusCategory.DONE)
                .statusOrder(3)
                .build();

        when(workflowService.getDefaultStatusesByCategory(any()))
                .thenReturn(Map.of(WorkflowStatusCategory.TODO, todoStatus, WorkflowStatusCategory.IN_PROGRESS,
                        inProgressStatus, WorkflowStatusCategory.DONE, doneStatus));

        when(boardColumnRepository.save(any(BoardColumn.class))).thenAnswer(invocation -> {
            final BoardColumn column = invocation.getArgument(0);
            return BoardColumn.builder()
                    .id("column-" + column.getColumnOrder())
                    .board(column.getBoard())
                    .name(column.getName())
                    .columnOrder(column.getColumnOrder())
                    .build();
        });
    }

    @Test
    void givenScrumBoardWithActiveSprint_whenGetBoardDetails_thenReturnsActiveSprint() {
        final Board board = createScrumBoard();
        final String sprintId = "550e8400-e29b-41d4-a716-446655440000";
        final Sprint activeSprint = createSprintWithIssues(sprintId,
                eu.itsonix.genai.xira.jpa.entity.SprintState.ACTIVE, Set.of());

        when(boardRepository.findByProjectKeyIgnoreCaseAndBoardNumber("XIRA", 1)).thenReturn(Optional.of(board));
        when(sprintRepository.findByProjectIdAndState("project-id",
                eu.itsonix.genai.xira.jpa.entity.SprintState.ACTIVE)).thenReturn(Optional.of(activeSprint));
        when(sprintRepository.findByProjectIdAndStateOrderByCreatedAt("project-id",
                eu.itsonix.genai.xira.jpa.entity.SprintState.PLANNED)).thenReturn(List.of());
        when(issueRepository.findByProjectIdAndStatusCategoryNotAndSprintIssuesEmptyOrderBySeqNoAsc("project-id",
                WorkflowStatusCategory.DONE)).thenReturn(List.of());

        final GetBoardDetails200Response response = boardService.getBoardDetails("XIRA", 1);

        assertThat(response).isInstanceOf(ScrumBoardDetailsResponse.class);
        final ScrumBoardDetailsResponse scrumResponse = (ScrumBoardDetailsResponse) response;
        assertThat(scrumResponse.getActiveSprint()).isNotNull();
    }

    @Test
    void givenScrumBoardWithoutActiveSprint_whenGetBoardDetails_thenReturnsNullActiveSprint() {
        final Board board = createScrumBoard();

        when(boardRepository.findByProjectKeyIgnoreCaseAndBoardNumber("XIRA", 1)).thenReturn(Optional.of(board));
        when(sprintRepository.findByProjectIdAndState("project-id",
                eu.itsonix.genai.xira.jpa.entity.SprintState.ACTIVE)).thenReturn(Optional.empty());
        when(sprintRepository.findByProjectIdAndStateOrderByCreatedAt("project-id",
                eu.itsonix.genai.xira.jpa.entity.SprintState.PLANNED)).thenReturn(List.of());
        when(issueRepository.findByProjectIdAndStatusCategoryNotAndSprintIssuesEmptyOrderBySeqNoAsc("project-id",
                WorkflowStatusCategory.DONE)).thenReturn(List.of());

        final GetBoardDetails200Response response = boardService.getBoardDetails("XIRA", 1);

        assertThat(response).isInstanceOf(ScrumBoardDetailsResponse.class);
        final ScrumBoardDetailsResponse scrumResponse = (ScrumBoardDetailsResponse) response;
        assertThat(scrumResponse.getActiveSprint()).isNull();
    }

    @Test
    void givenScrumBoardWithPlannedSprints_whenGetBoardDetails_thenReturnsOrderedByCreatedAt() {
        final Board board = createScrumBoard();
        final String sprintId1 = "550e8400-e29b-41d4-a716-446655440001";
        final String sprintId2 = "550e8400-e29b-41d4-a716-446655440002";
        final Sprint sprint1 = createSprintWithIssues(sprintId1, eu.itsonix.genai.xira.jpa.entity.SprintState.PLANNED,
                Set.of());
        final Sprint sprint2 = createSprintWithIssues(sprintId2, eu.itsonix.genai.xira.jpa.entity.SprintState.PLANNED,
                Set.of());

        when(boardRepository.findByProjectKeyIgnoreCaseAndBoardNumber("XIRA", 1)).thenReturn(Optional.of(board));
        when(sprintRepository.findByProjectIdAndState("project-id",
                eu.itsonix.genai.xira.jpa.entity.SprintState.ACTIVE)).thenReturn(Optional.empty());
        when(sprintRepository.findByProjectIdAndStateOrderByCreatedAt("project-id",
                eu.itsonix.genai.xira.jpa.entity.SprintState.PLANNED)).thenReturn(List.of(sprint1, sprint2));
        when(issueRepository.findByProjectIdAndStatusCategoryNotAndSprintIssuesEmptyOrderBySeqNoAsc("project-id",
                WorkflowStatusCategory.DONE)).thenReturn(List.of());

        final GetBoardDetails200Response response = boardService.getBoardDetails("XIRA", 1);

        assertThat(response).isInstanceOf(ScrumBoardDetailsResponse.class);
        final ScrumBoardDetailsResponse scrumResponse = (ScrumBoardDetailsResponse) response;
        assertThat(scrumResponse.getPlannedSprints()).hasSize(2);
    }

    @Test
    void givenScrumBoardWithBacklog_whenGetBoardDetails_thenExcludesDoneStatus() {
        final Board board = createScrumBoard();

        when(boardRepository.findByProjectKeyIgnoreCaseAndBoardNumber("XIRA", 1)).thenReturn(Optional.of(board));
        when(sprintRepository.findByProjectIdAndState("project-id",
                eu.itsonix.genai.xira.jpa.entity.SprintState.ACTIVE)).thenReturn(Optional.empty());
        when(sprintRepository.findByProjectIdAndStateOrderByCreatedAt("project-id",
                eu.itsonix.genai.xira.jpa.entity.SprintState.PLANNED)).thenReturn(List.of());
        when(issueRepository.findByProjectIdAndStatusCategoryNotAndSprintIssuesEmptyOrderBySeqNoAsc("project-id",
                WorkflowStatusCategory.DONE)).thenReturn(List.of());

        boardService.getBoardDetails("XIRA", 1);

        verify(issueRepository).findByProjectIdAndStatusCategoryNotAndSprintIssuesEmptyOrderBySeqNoAsc("project-id",
                WorkflowStatusCategory.DONE);
    }

    @Test
    void givenKanbanBoard_whenGetBoardDetails_thenReturnsColumnsOrderedByColumnOrder() {
        final Board board = createKanbanBoard();
        final BoardColumn column1 = createBoardColumn("col-1", "To Do", 1);
        final BoardColumn column2 = createBoardColumn("col-2", "Done", 2);

        when(boardRepository.findByProjectKeyIgnoreCaseAndBoardNumber("XIRA", 1)).thenReturn(Optional.of(board));
        when(boardColumnRepository.findByBoardIdOrderByColumnOrder("board-id")).thenReturn(List.of(column1, column2));
        when(issueRepository.findByStatusIdInOrderByCreatedAt(List.of())).thenReturn(List.of());

        final GetBoardDetails200Response response = boardService.getBoardDetails("XIRA", 1);

        assertThat(response).isInstanceOf(KanbanBoardDetailsResponse.class);
        final KanbanBoardDetailsResponse kanbanResponse = (KanbanBoardDetailsResponse) response;
        assertThat(kanbanResponse.getColumns()).hasSize(2);
        assertThat(kanbanResponse.getColumns().getFirst().getName()).isEqualTo("To Do");
        assertThat(kanbanResponse.getColumns().get(1).getName()).isEqualTo("Done");
    }

    @Test
    void givenScrumBoardWithActiveSprint_whenGetActiveSprint_thenReturnsSprintWithColumns() {
        final String activeSprintId = "550e8400-e29b-41d4-a716-446655440003";
        final Board board = createScrumBoard();
        final Sprint activeSprint = createSprintWithIssues(activeSprintId,
                eu.itsonix.genai.xira.jpa.entity.SprintState.ACTIVE, Set.of());
        final BoardColumn column = createBoardColumn("col-1", "To Do", 1);

        when(boardRepository.findByProjectKeyIgnoreCaseAndBoardNumberAndType("XIRA", 1, BoardType.SCRUM))
                .thenReturn(Optional.of(board));
        when(sprintRepository.findByProjectIdAndState("project-id",
                eu.itsonix.genai.xira.jpa.entity.SprintState.ACTIVE)).thenReturn(Optional.of(activeSprint));
        when(boardColumnRepository.findByBoardIdOrderByColumnOrder("board-id")).thenReturn(List.of(column));

        final ActiveSprintResponse response = boardService.getActiveSprint("XIRA", 1);

        assertThat(response).isNotNull();
        assertThat(response.getColumns()).hasSize(1);
    }

    @Test
    void givenScrumBoardWithoutActiveSprint_whenGetActiveSprint_thenReturnsNull() {
        final Board board = createScrumBoard();

        when(boardRepository.findByProjectKeyIgnoreCaseAndBoardNumberAndType("XIRA", 1, BoardType.SCRUM))
                .thenReturn(Optional.of(board));
        when(sprintRepository.findByProjectIdAndState("project-id",
                eu.itsonix.genai.xira.jpa.entity.SprintState.ACTIVE)).thenReturn(Optional.empty());

        final ActiveSprintResponse response = boardService.getActiveSprint("XIRA", 1);

        assertThat(response).isNull();
    }

    @Test
    void givenKanbanBoard_whenGetActiveSprint_thenThrowsEntityNotFound() {
        when(boardRepository.findByProjectKeyIgnoreCaseAndBoardNumberAndType("XIRA", 1, BoardType.SCRUM))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> boardService.getActiveSprint("XIRA", 1)).isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Board not found or not a SCRUM board");
    }

    @Test
    void givenNonExistentBoard_whenGetBoardDetails_thenThrowsEntityNotFound() {
        when(boardRepository.findByProjectKeyIgnoreCaseAndBoardNumber("XIRA", 99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> boardService.getBoardDetails("XIRA", 99)).isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Board not found");
    }

    @Test
    void givenNonExistentBoard_whenGetActiveSprint_thenThrowsEntityNotFound() {
        when(boardRepository.findByProjectKeyIgnoreCaseAndBoardNumberAndType("XIRA", 99, BoardType.SCRUM))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> boardService.getActiveSprint("XIRA", 99)).isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Board not found or not a SCRUM board");
    }

    private Board createScrumBoard() {
        return Board.builder()
                .id("board-id")
                .name("Scrum Board")
                .boardNumber(1)
                .type(BoardType.SCRUM)
                .projectId("project-id")
                .build();
    }

    private Board createKanbanBoard() {
        return Board.builder()
                .id("board-id")
                .name("Kanban Board")
                .boardNumber(1)
                .type(BoardType.KANBAN)
                .projectId("project-id")
                .build();
    }

    private Sprint createSprintWithIssues(final String id, final eu.itsonix.genai.xira.jpa.entity.SprintState state,
            final Set<SprintIssue> sprintIssues) {
        return Sprint.builder()
                .id(id)
                .name("Sprint 1")
                .goal("Sprint goal")
                .state(state)
                .sprintIssues(sprintIssues)
                .createdAt(Instant.now())
                .build();
    }

    private BoardColumn createBoardColumn(final String id, final String name, final Integer order) {
        return BoardColumn.builder().id(id).name(name).columnOrder(order).build();
    }
}
