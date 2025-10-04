package eu.itsonix.genai.xira.web;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import eu.itsonix.genai.xira.service.BoardService;
import eu.itsonix.genai.xira.web.api.BoardsApi;
import eu.itsonix.genai.xira.web.model.AddBoardRequest;
import eu.itsonix.genai.xira.web.model.UpdateBoardRequest;

@RestController
@RequiredArgsConstructor
public class BoardController implements BoardsApi {

    private final BoardService boardService;

    @Override
    @PreAuthorize("@authService.isProjectAdmin(#key)")
    public ResponseEntity<Void> createBoard(final String key, final AddBoardRequest addBoardRequest) {
        final String boardNumber = boardService.addBoard(key, addBoardRequest);
        return ResponseEntity.created(URI.create(String.format("/projects/%s/boards/%s", key, boardNumber))).build();
    }

    @Override
    @PreAuthorize("@authService.isProjectAdmin(#key)")
    public ResponseEntity<Void> updateBoard(final String key, final Integer boardNumber, final UpdateBoardRequest updateBoardRequest) {
        boardService.updateBoard(key, boardNumber, updateBoardRequest);
        return ResponseEntity.ok().build();
    }
}
