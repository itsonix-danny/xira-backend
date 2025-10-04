package eu.itsonix.genai.xira.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.itsonix.genai.xira.jpa.entity.Board;
import eu.itsonix.genai.xira.jpa.entity.BoardColumn;
import eu.itsonix.genai.xira.jpa.entity.BoardColumnWorkflowStatus;
import eu.itsonix.genai.xira.jpa.entity.BoardType;
import eu.itsonix.genai.xira.jpa.entity.Project;
import eu.itsonix.genai.xira.jpa.entity.WorkflowStatus;
import eu.itsonix.genai.xira.jpa.entity.WorkflowStatusCategory;
import eu.itsonix.genai.xira.jpa.repository.BoardColumnRepository;
import eu.itsonix.genai.xira.jpa.repository.BoardColumnWorkflowStatusRepository;
import eu.itsonix.genai.xira.jpa.repository.BoardRepository;
import eu.itsonix.genai.xira.jpa.repository.ProjectRepository;
import eu.itsonix.genai.xira.web.model.AddBoardRequest;
import eu.itsonix.genai.xira.web.model.UpdateBoardRequest;

import java.util.Map;
import java.util.Optional;

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

    @InjectMocks
    private BoardService boardService;

    @Test
    void givenValidRequest_whenAddScrumBoard_thenCreatesBoard() {
        final Project project = Project.builder()
                .id("project-id")
                .key("XIRA")
                .name("Xira")
                .build();

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

        final String boardNumber = boardService.addBoard("XIRA", new AddBoardRequest()
                .name("Scrum Board")
                .type(AddBoardRequest.TypeEnum.SCRUM));

        assertThat(boardNumber).isEqualTo("1");
        verify(boardRepository).save(any(Board.class));
        verify(boardColumnRepository, times(3)).save(any(BoardColumn.class));
        verify(boardColumnWorkflowStatusRepository, times(3)).save(any(BoardColumnWorkflowStatus.class));
    }

    @Test
    void givenValidRequest_whenAddKanbanBoard_thenCreatesBoard() {
        final Project project = Project.builder()
                .id("project-id")
                .key("XIRA")
                .name("Xira")
                .build();

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

        final String boardNumber = boardService.addBoard("XIRA", new AddBoardRequest()
                .name("Kanban Board")
                .type(AddBoardRequest.TypeEnum.KANBAN));

        assertThat(boardNumber).isEqualTo("1");
        verify(boardRepository).save(any(Board.class));
        verify(boardColumnRepository, times(3)).save(any(BoardColumn.class));
        verify(boardColumnWorkflowStatusRepository, times(3)).save(any(BoardColumnWorkflowStatus.class));
    }

    @Test
    void givenExistingBoard_whenAddBoard_thenAssignsNextBoardNumber() {
        final Project project = Project.builder()
                .id("project-id")
                .key("XIRA")
                .name("Xira")
                .build();

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

        final String boardNumber = boardService.addBoard("XIRA", new AddBoardRequest()
                .name("Third Board")
                .type(AddBoardRequest.TypeEnum.KANBAN));

        assertThat(boardNumber).isEqualTo("3");
        verify(boardRepository).save(any(Board.class));
        verify(boardColumnRepository, times(3)).save(any(BoardColumn.class));
        verify(boardColumnWorkflowStatusRepository, times(3)).save(any(BoardColumnWorkflowStatus.class));
    }

    @Test
    void givenUnknownProject_whenAddBoard_thenThrowsEntityNotFound() {
        when(projectRepository.findByKeyIgnoreCase("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> boardService.addBoard("UNKNOWN", new AddBoardRequest()
                .name("Kanban Board")
                .type(AddBoardRequest.TypeEnum.KANBAN)))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Project not found");

        verify(boardRepository, never()).save(any(Board.class));
    }

    @Test
    void givenExistingScrumBoard_whenAddScrumBoard_thenThrowsIllegalState() {
        final Project project = Project.builder()
                .id("project-id")
                .key("XIRA")
                .name("Xira")
                .build();

        when(projectRepository.findByKeyIgnoreCase("XIRA")).thenReturn(Optional.of(project));
        when(boardRepository.existsByProjectIdAndType("project-id", BoardType.SCRUM)).thenReturn(true);

        assertThatThrownBy(() -> boardService.addBoard("XIRA", new AddBoardRequest()
                .name("Scrum Board")
                .type(AddBoardRequest.TypeEnum.SCRUM)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Project already has a Scrum board");

        verify(boardRepository, never()).save(any(Board.class));
    }

    @Test
    void givenValidRequest_whenUpdateBoard_thenUpdatesBoard() {
        final Project project = Project.builder()
                .id("project-id")
                .key("XIRA")
                .build();

        final Board existingBoard = Board.builder()
                .id("board-id")
                .name("Old Name")
                .boardNumber(1)
                .type(BoardType.KANBAN)
                .project(project)
                .build();

        when(boardRepository.findByProjectKeyIgnoreCaseAndBoardNumber("XIRA", 1)).thenReturn(Optional.of(existingBoard));

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

        when(workflowService.getDefaultStatusesByCategory(any())).thenReturn(Map.of(
                WorkflowStatusCategory.TODO, todoStatus,
                WorkflowStatusCategory.IN_PROGRESS, inProgressStatus,
                WorkflowStatusCategory.DONE, doneStatus
        ));

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
}
