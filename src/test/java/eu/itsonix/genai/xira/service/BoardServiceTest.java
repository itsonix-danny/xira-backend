package eu.itsonix.genai.xira.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import eu.itsonix.genai.xira.jpa.entity.Board;
import eu.itsonix.genai.xira.jpa.entity.BoardType;
import eu.itsonix.genai.xira.jpa.entity.Project;
import eu.itsonix.genai.xira.jpa.repository.BoardRepository;
import eu.itsonix.genai.xira.jpa.repository.ProjectRepository;
import eu.itsonix.genai.xira.web.model.AddBoardRequest;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private AuthService authService;

    @InjectMocks
    private BoardService boardService;

    @Test
    void givenAdmin_whenAddScrumBoardWithoutExisting_thenCreatesBoard() {
        final Project project = Project.builder()
                .id("project-id")
                .key("XIRA")
                .name("Xira")
                .build();

        when(projectRepository.findByKeyIgnoreCase("XIRA")).thenReturn(Optional.of(project));
        when(authService.isProjectAdmin("XIRA")).thenReturn(true);
        when(boardRepository.existsByProjectIdAndType("project-id", BoardType.SCRUM)).thenReturn(false);

        final Board savedBoard = Board.builder()
                .id("board-id")
                .name("Scrum Board")
                .type(BoardType.SCRUM)
                .project(project)
                .build();
        when(boardRepository.save(any(Board.class))).thenReturn(savedBoard);

        final String boardId = boardService.addBoard("XIRA", new AddBoardRequest()
                .name("Scrum Board")
                .type(AddBoardRequest.TypeEnum.SCRUM));

        assertThat(boardId).isEqualTo("board-id");
        verify(boardRepository).save(any(Board.class));
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
    void givenNonAdmin_whenAddBoard_thenThrowsAccessDenied() {
        final Project project = Project.builder()
                .id("project-id")
                .key("XIRA")
                .name("Xira")
                .build();

        when(projectRepository.findByKeyIgnoreCase("XIRA")).thenReturn(Optional.of(project));
        when(authService.isProjectAdmin("XIRA")).thenReturn(false);

        assertThatThrownBy(() -> boardService.addBoard("XIRA", new AddBoardRequest()
                .name("Kanban Board")
                .type(AddBoardRequest.TypeEnum.KANBAN)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("User is not a project admin");

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
        when(authService.isProjectAdmin("XIRA")).thenReturn(true);
        when(boardRepository.existsByProjectIdAndType("project-id", BoardType.SCRUM)).thenReturn(true);

        assertThatThrownBy(() -> boardService.addBoard("XIRA", new AddBoardRequest()
                .name("Scrum Board")
                .type(AddBoardRequest.TypeEnum.SCRUM)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Project already has a Scrum board");

        verify(boardRepository, never()).save(any(Board.class));
    }
}
