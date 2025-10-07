package eu.itsonix.genai.xira.service;

import jakarta.persistence.EntityNotFoundException;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import eu.itsonix.genai.xira.jpa.entity.*;
import eu.itsonix.genai.xira.jpa.repository.*;
import eu.itsonix.genai.xira.jpa.specification.IssueSpecification;
import eu.itsonix.genai.xira.mapper.IssueMapper;
import eu.itsonix.genai.xira.web.model.*;

@Service
@RequiredArgsConstructor
public class IssueService {

    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;
    private final WorkflowStatusRepository workflowStatusRepository;
    private final IssueAssigneeRepository issueAssigneeRepository;
    private final IssueCommentRepository issueCommentRepository;
    private final XiraUserRepository xiraUserRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final AuthService authService;

    @Transactional
    public String createIssue(final String projectKey, final CreateIssueRequest createIssueRequest) {
        final Project project = projectRepository.findByKeyIgnoreCase(projectKey)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));

        final Integer nextSeqNo = issueRepository.findMaxSeqNoByProjectId(project.getId()) + 1;
        final String issueKey = String.format("%s-%d", project.getKey(), nextSeqNo);

        final WorkflowStatus defaultStatus = workflowStatusRepository
                .findFirstByWorkflowProjectIdOrderByStatusOrderAsc(project.getId())
                .orElseThrow(() -> new IllegalStateException("No workflow status found"));

        final Issue issue = Issue.builder()
                .project(project)
                .seqNo(nextSeqNo)
                .key(issueKey)
                .issueType(IssueType.valueOf(createIssueRequest.getIssueType().name()))
                .status(defaultStatus)
                .priority(IssuePriority.valueOf(createIssueRequest.getPriority().name()))
                .reporter(authService.getAuthenticatedUser())
                .title(createIssueRequest.getTitle())
                .description(createIssueRequest.getDescription())
                .build();

        issueRepository.save(issue);

        return issueKey;
    }

    @Transactional
    public void updateIssue(final String projectKey, final String issueKey,
            final UpdateIssueRequest updateIssueRequest) {
        final Issue issue = issueRepository.findByKeyAndProjectKeyIgnoreCase(issueKey, projectKey)
                .orElseThrow(() -> new EntityNotFoundException("Issue not found"));

        if (updateIssueRequest.getTitle() != null) {
            issue.setTitle(updateIssueRequest.getTitle());
        }

        if (updateIssueRequest.getDescription() != null) {
            issue.setDescription(updateIssueRequest.getDescription());
        }

        if (updateIssueRequest.getIssueType() != null) {
            issue.setIssueType(IssueType.valueOf(updateIssueRequest.getIssueType().name()));
        }

        if (updateIssueRequest.getPriority() != null) {
            issue.setPriority(IssuePriority.valueOf(updateIssueRequest.getPriority().name()));
        }

        issueRepository.save(issue);
    }

    @Transactional
    public void addAssignee(final String projectKey, final String issueKey,
            final AddAssigneeRequest addAssigneeRequest) {
        final Issue issue = issueRepository.findByKeyAndProjectKeyIgnoreCase(issueKey, projectKey)
                .orElseThrow(() -> new EntityNotFoundException("Issue not found"));

        final String userId = addAssigneeRequest.getUserId().toString();
        final XiraUser user = xiraUserRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (!projectMemberRepository.existsByProject_KeyIgnoreCaseAndUserId(projectKey, userId)) {
            throw new IllegalStateException("User is not a member of this project");
        }

        if (issueAssigneeRepository.existsByIssueIdAndUserId(issue.getId(), userId)) {
            throw new IllegalStateException("User is already assigned to this issue");
        }

        issueAssigneeRepository.save(
                IssueAssignee.builder().issueId(issue.getId()).userId(userId).issue(issue).xiraUser(user).build());
    }

    @Transactional
    public void removeAssignee(final String projectKey, final String issueKey, final String userId) {
        final Issue issue = issueRepository.findByKeyAndProjectKeyIgnoreCase(issueKey, projectKey)
                .orElseThrow(() -> new EntityNotFoundException("Issue not found"));

        if (!issueAssigneeRepository.existsByIssueIdAndUserId(issue.getId(), userId)) {
            throw new EntityNotFoundException("Assignee not found");
        }

        issueAssigneeRepository.deleteByIssueIdAndUserId(issue.getId(), userId);
    }

    @Transactional
    public void setIssueStatus(final String projectKey, final String issueKey,
            final SetIssueStatusRequest setIssueStatusRequest) {
        final Issue issue = issueRepository.findByKeyAndProjectKeyIgnoreCase(issueKey, projectKey)
                .orElseThrow(() -> new EntityNotFoundException("Issue not found"));

        final String statusId = setIssueStatusRequest.getStatusId().toString();
        final WorkflowStatus status = workflowStatusRepository
                .findByIdAndWorkflowProjectKeyIgnoreCase(statusId, projectKey)
                .orElseThrow(() -> new EntityNotFoundException("Workflow status not found"));

        issue.setStatus(status);
        issue.setClosedAt(status.getCategory() == WorkflowStatusCategory.DONE ? Instant.now() : null);

        issueRepository.save(issue);
    }

    @Transactional
    public String addComment(final String projectKey, final String issueKey,
            final AddCommentRequest addCommentRequest) {
        final Issue issue = issueRepository.findByKeyAndProjectKeyIgnoreCase(issueKey, projectKey)
                .orElseThrow(() -> new EntityNotFoundException("Issue not found"));

        final XiraUser author = authService.getAuthenticatedUser();

        final IssueComment comment = issueCommentRepository.save(
                IssueComment.builder().issue(issue).author(author).content(addCommentRequest.getContent()).build());

        return comment.getId();
    }

    @Transactional
    public void updateComment(final String projectKey, final String issueKey, final String commentId,
            final UpdateCommentRequest updateCommentRequest) {
        final IssueComment comment = issueCommentRepository
                .findByIdAndIssueKeyAndIssueProjectKeyIgnoreCase(commentId, issueKey, projectKey)
                .orElseThrow(() -> new EntityNotFoundException("Comment not found"));

        comment.setContent(updateCommentRequest.getContent());
        issueCommentRepository.save(comment);
    }

    @Transactional
    public void deleteComment(final String projectKey, final String issueKey, final String commentId) {
        final IssueComment comment = issueCommentRepository
                .findByIdAndIssueKeyAndIssueProjectKeyIgnoreCase(commentId, issueKey, projectKey)
                .orElseThrow(() -> new EntityNotFoundException("Comment not found"));

        issueCommentRepository.delete(comment);
    }

    @Transactional(readOnly = true)
    public List<IssueSummaryResponse> getIssues(final String projectKey, final String assigneeId,
            final Boolean includeFinished, final String search) {
        final String userId = authService.getAuthenticatedUser().getId();
        final Specification<Issue> spec = IssueSpecification.getIssuesForUser(userId, projectKey, assigneeId,
                includeFinished, search);

        return issueRepository.findAll(spec)
                .stream()
                .sorted(Comparator.comparing(Issue::getSeqNo))
                .map(IssueMapper::toIssueSummaryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public IssueDetailsResponse getIssueDetails(final String projectKey, final String issueKey) {
        final Issue issue = issueRepository.findWithDetailsByKeyAndProjectKeyIgnoreCase(issueKey, projectKey)
                .orElseThrow(() -> new EntityNotFoundException("Issue not found"));

        return IssueMapper.toIssueDetailsResponse(issue);
    }
}
