package eu.itsonix.genai.xira.web;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import eu.itsonix.genai.xira.service.IssueService;
import eu.itsonix.genai.xira.web.api.IssuesApi;
import eu.itsonix.genai.xira.web.model.*;

@RestController
@RequiredArgsConstructor
public class IssueController implements IssuesApi {

    private final IssueService issueService;

    @Override
    @PreAuthorize("@authService.isProjectMember(#key)")
    public ResponseEntity<Void> createIssue(final String key, final CreateIssueRequest createIssueRequest) {
        final String issueKey = issueService.createIssue(key, createIssueRequest);
        return ResponseEntity.created(URI.create(String.format("/projects/%s/issues/%s", key, issueKey))).build();
    }

    @Override
    @PreAuthorize("@authService.isProjectMember(#key)")
    public ResponseEntity<Void> updateIssue(final String key, final String issueKey,
            final UpdateIssueRequest updateIssueRequest) {
        issueService.updateIssue(key, issueKey, updateIssueRequest);
        return ResponseEntity.ok().build();
    }

    @Override
    @PreAuthorize("@authService.isProjectMember(#key)")
    public ResponseEntity<Void> addAssignee(final String key, final String issueKey,
            final AddAssigneeRequest addAssigneeRequest) {
        issueService.addAssignee(key, issueKey, addAssigneeRequest);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("@authService.isProjectMember(#key)")
    public ResponseEntity<Void> removeAssignee(final String key, final String issueKey, final UUID userId) {
        issueService.removeAssignee(key, issueKey, userId.toString());
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("@authService.isProjectMember(#key)")
    public ResponseEntity<Void> setIssueStatus(final String key, final String issueKey,
            final SetIssueStatusRequest setIssueStatusRequest) {
        issueService.setIssueStatus(key, issueKey, setIssueStatusRequest);
        return ResponseEntity.ok().build();
    }

    @Override
    @PreAuthorize("@authService.isProjectMember(#key)")
    public ResponseEntity<Void> addComment(final String key, final String issueKey,
            final AddCommentRequest addCommentRequest) {
        final String commentId = issueService.addComment(key, issueKey, addCommentRequest);
        return ResponseEntity
                .created(URI.create(String.format("/projects/%s/issues/%s/comments/%s", key, issueKey, commentId)))
                .build();
    }

    @Override
    @PreAuthorize("@authService.isProjectMember(#key) and @authService.isCommentAuthor(#commentId)")
    public ResponseEntity<Void> updateComment(final String key, final String issueKey, final UUID commentId,
            final UpdateCommentRequest updateCommentRequest) {
        issueService.updateComment(key, issueKey, commentId.toString(), updateCommentRequest);
        return ResponseEntity.ok().build();
    }

    @Override
    @PreAuthorize("@authService.isProjectMember(#key) and (@authService.isCommentAuthor(#commentId) or @authService.isProjectAdmin(#key))")
    public ResponseEntity<Void> deleteComment(final String key, final String issueKey, final UUID commentId) {
        issueService.deleteComment(key, issueKey, commentId.toString());
        return ResponseEntity.noContent().build();
    }
}
