package eu.itsonix.genai.xira.web;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import eu.itsonix.genai.xira.service.BoardService;
import eu.itsonix.genai.xira.web.api.BoardsApi;
import eu.itsonix.genai.xira.web.model.AddBoardRequest;

@RestController
@RequiredArgsConstructor
public class BoardController implements BoardsApi {

    private final BoardService boardService;

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> createBoard(final String key, final AddBoardRequest addBoardRequest) {
        final String boardId = boardService.addBoard(key, addBoardRequest);
        return ResponseEntity.created(URI.create(String.format("/projects/%s/boards/%s", key, boardId))).build();
    }
}
