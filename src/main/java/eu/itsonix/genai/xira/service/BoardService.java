package eu.itsonix.genai.xira.service;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import eu.itsonix.genai.xira.jpa.entity.Board;
import eu.itsonix.genai.xira.jpa.entity.BoardType;
import eu.itsonix.genai.xira.jpa.entity.Project;
import eu.itsonix.genai.xira.jpa.repository.BoardRepository;
import eu.itsonix.genai.xira.jpa.repository.ProjectRepository;
import eu.itsonix.genai.xira.web.model.AddBoardRequest;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final ProjectRepository projectRepository;
    private final AuthService authService;

    @Transactional
    public String addBoard(final String projectKey, final AddBoardRequest addBoardRequest) {
        final Project project = projectRepository.findByKeyIgnoreCase(projectKey)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));

        if (!authService.isProjectAdmin(projectKey)) {
            throw new AccessDeniedException("User is not a project admin");
        }

        final BoardType boardType = BoardType.valueOf(addBoardRequest.getType().getValue());

        if (boardType == BoardType.SCRUM && boardRepository.existsByProjectIdAndType(project.getId(), BoardType.SCRUM)) {
            throw new IllegalStateException("Project already has a Scrum board");
        }

        final Board board = Board.builder()
                .name(addBoardRequest.getName())
                .type(boardType)
                .project(project)
                .build();

        return boardRepository.save(board).getId();
    }
}
