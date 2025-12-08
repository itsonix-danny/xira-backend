package eu.itsonix.genai.xira.service;

import jakarta.persistence.EntityNotFoundException;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import eu.itsonix.genai.xira.jpa.entity.*;
import eu.itsonix.genai.xira.jpa.repository.*;
import eu.itsonix.genai.xira.web.model.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssueServiceTest {

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private WorkflowStatusRepository workflowStatusRepository;

    @Mock
    private IssueAssigneeRepository issueAssigneeRepository;

    @Mock
    private IssueCommentRepository issueCommentRepository;

    @Mock
    private IssueRelationRepository issueRelationRepository;

    @Mock
    private XiraUserRepository xiraUserRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private AuthService authService;

    @InjectMocks
    private IssueService issueService;

    @Test
    void givenValidRequest_whenCreateIssue_thenCreatesIssueWithDefaultStatus() {
        final String projectKey = "XIRA";
        final String projectId = "project-id";
        final String userId = "user-id";

        final Project project = Project.builder().id(projectId).key(projectKey).name("Xira").build();
        final XiraUser reporter = XiraUser.builder().id(userId).email("user@example.com").build();
        final WorkflowStatus defaultStatus = WorkflowStatus.builder()
                .id("550e8400-e29b-41d4-a716-446655440100")
                .name("To Do")
                .category(WorkflowStatusCategory.TODO)
                .statusOrder(1)
                .build();

        final CreateIssueRequest request = new CreateIssueRequest().title("Test Issue")
                .description("Test Description")
                .issueType(CreateIssueRequest.IssueTypeEnum.BUG)
                .priority(CreateIssueRequest.PriorityEnum.HIGH);

        when(projectRepository.findByKeyIgnoreCase(projectKey)).thenReturn(Optional.of(project));
        when(authService.getAuthenticatedUser()).thenReturn(reporter);
        when(issueRepository.findMaxSeqNoByProjectId(projectId)).thenReturn(0);
        when(workflowStatusRepository.findFirstByWorkflowProjectIdOrderByStatusOrderAsc(projectId))
                .thenReturn(Optional.of(defaultStatus));

        final String result = issueService.createIssue(projectKey, request);

        assertThat(result).isEqualTo("XIRA-1");
        verify(issueRepository).save(any(Issue.class));
    }

    @Test
    void givenExistingIssues_whenCreateIssue_thenIncrementsSeqNo() {
        final String projectKey = "XIRA";
        final String projectId = "project-id";
        final String userId = "user-id";

        final Project project = Project.builder().id(projectId).key(projectKey).name("Xira").build();
        final XiraUser reporter = XiraUser.builder().id(userId).email("user@example.com").build();
        final WorkflowStatus defaultStatus = WorkflowStatus.builder()
                .id("550e8400-e29b-41d4-a716-446655440100")
                .name("To Do")
                .category(WorkflowStatusCategory.TODO)
                .statusOrder(1)
                .build();

        final CreateIssueRequest request = new CreateIssueRequest().title("Test Issue")
                .issueType(CreateIssueRequest.IssueTypeEnum.TASK)
                .priority(CreateIssueRequest.PriorityEnum.MEDIUM);

        when(projectRepository.findByKeyIgnoreCase(projectKey)).thenReturn(Optional.of(project));
        when(authService.getAuthenticatedUser()).thenReturn(reporter);
        when(issueRepository.findMaxSeqNoByProjectId(projectId)).thenReturn(5);
        when(workflowStatusRepository.findFirstByWorkflowProjectIdOrderByStatusOrderAsc(projectId))
                .thenReturn(Optional.of(defaultStatus));

        final String result = issueService.createIssue(projectKey, request);

        assertThat(result).isEqualTo("XIRA-6");
    }

    @Test
    void givenNoDescription_whenCreateIssue_thenUsesEmptyString() {
        final String projectKey = "XIRA";
        final String projectId = "project-id";
        final String userId = "user-id";

        final Project project = Project.builder().id(projectId).key(projectKey).name("Xira").build();
        final XiraUser reporter = XiraUser.builder().id(userId).email("user@example.com").build();
        final WorkflowStatus defaultStatus = WorkflowStatus.builder()
                .id("550e8400-e29b-41d4-a716-446655440100")
                .name("To Do")
                .category(WorkflowStatusCategory.TODO)
                .statusOrder(1)
                .build();

        final CreateIssueRequest request = new CreateIssueRequest().title("Test Issue")
                .issueType(CreateIssueRequest.IssueTypeEnum.STORY)
                .priority(CreateIssueRequest.PriorityEnum.LOW);

        when(projectRepository.findByKeyIgnoreCase(projectKey)).thenReturn(Optional.of(project));
        when(authService.getAuthenticatedUser()).thenReturn(reporter);
        when(issueRepository.findMaxSeqNoByProjectId(projectId)).thenReturn(0);
        when(workflowStatusRepository.findFirstByWorkflowProjectIdOrderByStatusOrderAsc(projectId))
                .thenReturn(Optional.of(defaultStatus));

        issueService.createIssue(projectKey, request);

        verify(issueRepository).save(any(Issue.class));
    }

    @Test
    void givenInvalidProjectKey_whenCreateIssue_thenThrowsEntityNotFoundException() {
        final String projectKey = "INVALID";
        final CreateIssueRequest request = new CreateIssueRequest().title("Test Issue")
                .issueType(CreateIssueRequest.IssueTypeEnum.BUG)
                .priority(CreateIssueRequest.PriorityEnum.HIGH);

        when(projectRepository.findByKeyIgnoreCase(projectKey)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> issueService.createIssue(projectKey, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Project not found");
    }

    @Test
    void givenNoWorkflowStatus_whenCreateIssue_thenThrowsIllegalStateException() {
        final String projectKey = "XIRA";
        final String projectId = "project-id";

        final Project project = Project.builder().id(projectId).key(projectKey).name("Xira").build();

        final CreateIssueRequest request = new CreateIssueRequest().title("Test Issue")
                .issueType(CreateIssueRequest.IssueTypeEnum.BUG)
                .priority(CreateIssueRequest.PriorityEnum.HIGH);

        when(projectRepository.findByKeyIgnoreCase(projectKey)).thenReturn(Optional.of(project));
        when(issueRepository.findMaxSeqNoByProjectId(projectId)).thenReturn(0);
        when(workflowStatusRepository.findFirstByWorkflowProjectIdOrderByStatusOrderAsc(projectId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> issueService.createIssue(projectKey, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No workflow status found");
    }

    @Test
    void givenValidRequest_whenUpdateIssue_thenUpdatesIssue() {
        final String projectKey = "XIRA";
        final String issueKey = "XIRA-1";
        final Project project = Project.builder().id("project-id").key(projectKey).name("Xira").build();
        final Issue issue = Issue.builder()
                .id("issue-id")
                .key(issueKey)
                .project(project)
                .title("Old Title")
                .description("Old Description")
                .build();

        final UpdateIssueRequest request = new UpdateIssueRequest().title("New Title").description("New Description");

        when(issueRepository.findByKeyAndProjectKeyIgnoreCase(issueKey, projectKey)).thenReturn(Optional.of(issue));

        issueService.updateIssue(projectKey, issueKey, request);

        assertThat(issue.getTitle()).isEqualTo("New Title");
        assertThat(issue.getDescription()).isEqualTo("New Description");
        verify(issueRepository).save(issue);
    }

    @Test
    void givenInvalidIssueKey_whenUpdateIssue_thenThrowsEntityNotFoundException() {
        final String projectKey = "XIRA";
        final String issueKey = "XIRA-999";
        final UpdateIssueRequest request = new UpdateIssueRequest().title("New Title");

        when(issueRepository.findByKeyAndProjectKeyIgnoreCase(issueKey, projectKey)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> issueService.updateIssue(projectKey, issueKey, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Issue not found");
    }

    @Test
    void givenMismatchedProjectKey_whenUpdateIssue_thenThrowsEntityNotFoundException() {
        final String issueKey = "XIRA-1";
        final UpdateIssueRequest request = new UpdateIssueRequest().title("New Title");

        when(issueRepository.findByKeyAndProjectKeyIgnoreCase(issueKey, "WRONG")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> issueService.updateIssue("WRONG", issueKey, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Issue not found");
    }

    @Test
    void givenPriorityUpdate_whenUpdateIssue_thenUpdatesPriority() {
        final String projectKey = "XIRA";
        final String issueKey = "XIRA-1";
        final Project project = Project.builder().id("project-id").key(projectKey).name("Xira").build();
        final Issue issue = Issue.builder()
                .id("issue-id")
                .key(issueKey)
                .project(project)
                .priority(IssuePriority.LOW)
                .build();

        final UpdateIssueRequest request = new UpdateIssueRequest().priority(UpdateIssueRequest.PriorityEnum.HIGH);

        when(issueRepository.findByKeyAndProjectKeyIgnoreCase(issueKey, projectKey)).thenReturn(Optional.of(issue));

        issueService.updateIssue(projectKey, issueKey, request);

        assertThat(issue.getPriority()).isEqualTo(IssuePriority.HIGH);
        verify(issueRepository).save(issue);
    }

    @Test
    void givenIssueTypeUpdate_whenUpdateIssue_thenUpdatesIssueType() {
        final String projectKey = "XIRA";
        final String issueKey = "XIRA-1";
        final Project project = Project.builder().id("project-id").key(projectKey).name("Xira").build();
        final Issue issue = Issue.builder()
                .id("issue-id")
                .key(issueKey)
                .project(project)
                .issueType(IssueType.TASK)
                .build();

        final UpdateIssueRequest request = new UpdateIssueRequest().issueType(UpdateIssueRequest.IssueTypeEnum.BUG);

        when(issueRepository.findByKeyAndProjectKeyIgnoreCase(issueKey, projectKey)).thenReturn(Optional.of(issue));

        issueService.updateIssue(projectKey, issueKey, request);

        assertThat(issue.getIssueType()).isEqualTo(IssueType.BUG);
        verify(issueRepository).save(issue);
    }

    @Test
    void givenAllFieldsUpdate_whenUpdateIssue_thenUpdatesAllFields() {
        final String projectKey = "XIRA";
        final String issueKey = "XIRA-1";
        final Project project = Project.builder().id("project-id").key(projectKey).name("Xira").build();
        final Issue issue = Issue.builder()
                .id("issue-id")
                .key(issueKey)
                .project(project)
                .title("Old Title")
                .description("Old Description")
                .issueType(IssueType.TASK)
                .priority(IssuePriority.LOW)
                .build();

        final UpdateIssueRequest request = new UpdateIssueRequest().title("New Title")
                .description("New Description")
                .issueType(UpdateIssueRequest.IssueTypeEnum.STORY)
                .priority(UpdateIssueRequest.PriorityEnum.CRITICAL);

        when(issueRepository.findByKeyAndProjectKeyIgnoreCase(issueKey, projectKey)).thenReturn(Optional.of(issue));

        issueService.updateIssue(projectKey, issueKey, request);

        assertThat(issue.getTitle()).isEqualTo("New Title");
        assertThat(issue.getDescription()).isEqualTo("New Description");
        assertThat(issue.getIssueType()).isEqualTo(IssueType.STORY);
        assertThat(issue.getPriority()).isEqualTo(IssuePriority.CRITICAL);
        verify(issueRepository).save(issue);
    }

    @Test
    void givenValidRequest_whenAddAssignee_thenAddsAssignee() {
        final String projectKey = "XIRA";
        final String issueKey = "XIRA-1";
        final String userId = "550e8400-e29b-41d4-a716-446655440000";
        final String projectId = "project-id";
        final String issueId = "issue-id";

        final Project project = Project.builder().id(projectId).key(projectKey).build();
        final Issue issue = Issue.builder().id(issueId).key(issueKey).project(project).build();
        final XiraUser user = XiraUser.builder().id(userId).email("user@example.com").build();
        final AddAssigneeRequest request = new AddAssigneeRequest().userId(UUID.fromString(userId));

        when(issueRepository.findByKeyAndProjectKeyIgnoreCase(issueKey, projectKey)).thenReturn(Optional.of(issue));
        when(xiraUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(projectMemberRepository.existsByProject_KeyIgnoreCaseAndUserId(projectKey, userId)).thenReturn(true);
        when(issueAssigneeRepository.existsByIssueIdAndUserId(issueId, userId)).thenReturn(false);

        issueService.addAssignee(projectKey, issueKey, request);

        verify(issueAssigneeRepository).save(any(IssueAssignee.class));
    }

    @Test
    void givenUserNotProjectMember_whenAddAssignee_thenThrowsIllegalStateException() {
        final String projectKey = "XIRA";
        final String issueKey = "XIRA-1";
        final String userId = "550e8400-e29b-41d4-a716-446655440001";
        final String projectId = "project-id";

        final Project project = Project.builder().id(projectId).key(projectKey).build();
        final Issue issue = Issue.builder().id("issue-id").key(issueKey).project(project).build();
        final XiraUser user = XiraUser.builder().id(userId).email("user@example.com").build();
        final AddAssigneeRequest request = new AddAssigneeRequest().userId(UUID.fromString(userId));

        when(issueRepository.findByKeyAndProjectKeyIgnoreCase(issueKey, projectKey)).thenReturn(Optional.of(issue));
        when(xiraUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(projectMemberRepository.existsByProject_KeyIgnoreCaseAndUserId(projectKey, userId)).thenReturn(false);

        assertThatThrownBy(() -> issueService.addAssignee(projectKey, issueKey, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("User is not a member of this project");

        verify(issueAssigneeRepository, never()).save(any());
    }

    @Test
    void givenUserAlreadyAssigned_whenAddAssignee_thenThrowsIllegalStateException() {
        final String projectKey = "XIRA";
        final String issueKey = "XIRA-1";
        final String userId = "550e8400-e29b-41d4-a716-446655440002";
        final String projectId = "project-id";
        final String issueId = "issue-id";

        final Project project = Project.builder().id(projectId).key(projectKey).build();
        final Issue issue = Issue.builder().id(issueId).key(issueKey).project(project).build();
        final XiraUser user = XiraUser.builder().id(userId).email("user@example.com").build();
        final AddAssigneeRequest request = new AddAssigneeRequest().userId(UUID.fromString(userId));

        when(issueRepository.findByKeyAndProjectKeyIgnoreCase(issueKey, projectKey)).thenReturn(Optional.of(issue));
        when(xiraUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(projectMemberRepository.existsByProject_KeyIgnoreCaseAndUserId(projectKey, userId)).thenReturn(true);
        when(issueAssigneeRepository.existsByIssueIdAndUserId(issueId, userId)).thenReturn(true);

        assertThatThrownBy(() -> issueService.addAssignee(projectKey, issueKey, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("User is already assigned to this issue");

        verify(issueAssigneeRepository, never()).save(any());
    }

    @Test
    void givenValidRequest_whenRemoveAssignee_thenRemovesAssignee() {
        final String projectKey = "XIRA";
        final String issueKey = "XIRA-1";
        final String userId = "user-id";
        final String issueId = "issue-id";

        final Project project = Project.builder().id("project-id").key(projectKey).build();
        final Issue issue = Issue.builder().id(issueId).key(issueKey).project(project).build();

        when(issueRepository.findByKeyAndProjectKeyIgnoreCase(issueKey, projectKey)).thenReturn(Optional.of(issue));
        when(issueAssigneeRepository.existsByIssueIdAndUserId(issueId, userId)).thenReturn(true);

        issueService.removeAssignee(projectKey, issueKey, userId);

        verify(issueAssigneeRepository).deleteByIssueIdAndUserId(issueId, userId);
    }

    @Test
    void givenAssigneeNotFound_whenRemoveAssignee_thenThrowsEntityNotFoundException() {
        final String projectKey = "XIRA";
        final String issueKey = "XIRA-1";
        final String userId = "user-id";
        final String issueId = "issue-id";

        final Project project = Project.builder().id("project-id").key(projectKey).build();
        final Issue issue = Issue.builder().id(issueId).key(issueKey).project(project).build();

        when(issueRepository.findByKeyAndProjectKeyIgnoreCase(issueKey, projectKey)).thenReturn(Optional.of(issue));
        when(issueAssigneeRepository.existsByIssueIdAndUserId(issueId, userId)).thenReturn(false);

        assertThatThrownBy(() -> issueService.removeAssignee(projectKey, issueKey, userId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Assignee not found");

        verify(issueAssigneeRepository, never()).deleteByIssueIdAndUserId(anyString(), anyString());
    }

    @Test
    void givenValidStatusWithDoneCategory_whenSetIssueStatus_thenSetsClosedAt() {
        final String projectKey = "XIRA";
        final String issueKey = "XIRA-1";
        final String statusId = "550e8400-e29b-41d4-a716-446655440003";
        final String projectId = "project-id";

        final Project project = Project.builder().id(projectId).key(projectKey).build();
        final Workflow workflow = Workflow.builder().id("workflow-id").project(project).build();
        final WorkflowStatus status = WorkflowStatus.builder()
                .id(statusId)
                .name("Done")
                .category(WorkflowStatusCategory.DONE)
                .workflow(workflow)
                .build();
        final Issue issue = Issue.builder().id("issue-id").key(issueKey).project(project).closedAt(null).build();

        final SetIssueStatusRequest request = new SetIssueStatusRequest().statusId(UUID.fromString(statusId));

        when(issueRepository.findByKeyAndProjectKeyIgnoreCase(issueKey, projectKey)).thenReturn(Optional.of(issue));
        when(workflowStatusRepository.findByIdAndWorkflowProjectKeyIgnoreCase(statusId, projectKey))
                .thenReturn(Optional.of(status));

        issueService.setIssueStatus(projectKey, issueKey, request);

        assertThat(issue.getClosedAt()).isNotNull();
        verify(issueRepository).save(issue);
    }

    @Test
    void givenValidStatusWithNonDoneCategory_whenSetIssueStatus_thenClearsClosedAt() {
        final String projectKey = "XIRA";
        final String issueKey = "XIRA-1";
        final String statusId = "550e8400-e29b-41d4-a716-446655440004";
        final String projectId = "project-id";

        final Project project = Project.builder().id(projectId).key(projectKey).build();
        final Workflow workflow = Workflow.builder().id("workflow-id").project(project).build();
        final WorkflowStatus status = WorkflowStatus.builder()
                .id(statusId)
                .name("In Progress")
                .category(WorkflowStatusCategory.IN_PROGRESS)
                .workflow(workflow)
                .build();
        final Issue issue = Issue.builder()
                .id("issue-id")
                .key(issueKey)
                .project(project)
                .closedAt(Instant.now())
                .build();

        final SetIssueStatusRequest request = new SetIssueStatusRequest().statusId(UUID.fromString(statusId));

        when(issueRepository.findByKeyAndProjectKeyIgnoreCase(issueKey, projectKey)).thenReturn(Optional.of(issue));
        when(workflowStatusRepository.findByIdAndWorkflowProjectKeyIgnoreCase(statusId, projectKey))
                .thenReturn(Optional.of(status));

        issueService.setIssueStatus(projectKey, issueKey, request);

        assertThat(issue.getClosedAt()).isNull();
        verify(issueRepository).save(issue);
    }

    @Test
    void givenStatusFromDifferentProject_whenSetIssueStatus_thenThrowsEntityNotFoundException() {
        final String projectKey = "XIRA";
        final String issueKey = "XIRA-1";
        final String statusId = "550e8400-e29b-41d4-a716-446655440005";

        final Issue issue = Issue.builder().id("issue-id").key(issueKey).build();

        final SetIssueStatusRequest request = new SetIssueStatusRequest().statusId(UUID.fromString(statusId));

        when(issueRepository.findByKeyAndProjectKeyIgnoreCase(issueKey, projectKey)).thenReturn(Optional.of(issue));
        when(workflowStatusRepository.findByIdAndWorkflowProjectKeyIgnoreCase(statusId, projectKey))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> issueService.setIssueStatus(projectKey, issueKey, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Workflow status not found");

        verify(issueRepository, never()).save(any());
    }

    @Test
    void givenValidRequest_whenAddComment_thenAddsComment() {
        final String projectKey = "XIRA";
        final String issueKey = "XIRA-1";

        final Project project = Project.builder().id("project-id").key(projectKey).build();
        final Issue issue = Issue.builder().id("issue-id").key(issueKey).project(project).build();
        final XiraUser author = XiraUser.builder().id("user-id").email("user@example.com").build();
        final IssueComment savedComment = IssueComment.builder().id("comment-id").build();
        final AddCommentRequest request = new AddCommentRequest().content("Test comment");

        when(issueRepository.findByKeyAndProjectKeyIgnoreCase(issueKey, projectKey)).thenReturn(Optional.of(issue));
        when(authService.getAuthenticatedUser()).thenReturn(author);
        when(issueCommentRepository.save(any(IssueComment.class))).thenReturn(savedComment);

        final String result = issueService.addComment(projectKey, issueKey, request);

        assertThat(result).isEqualTo("comment-id");
        verify(issueCommentRepository).save(any(IssueComment.class));
    }

    @Test
    void givenValidRequest_whenUpdateComment_thenUpdatesComment() {
        final String projectKey = "XIRA";
        final String issueKey = "XIRA-1";
        final String commentId = "comment-id";

        final IssueComment comment = IssueComment.builder().id(commentId).content("Old content").build();
        final UpdateCommentRequest request = new UpdateCommentRequest().content("New content");

        when(issueCommentRepository.findByIdAndIssueKeyAndIssueProjectKeyIgnoreCase(commentId, issueKey, projectKey))
                .thenReturn(Optional.of(comment));

        issueService.updateComment(projectKey, issueKey, commentId, request);

        assertThat(comment.getContent()).isEqualTo("New content");
        verify(issueCommentRepository).save(comment);
    }

    @Test
    void givenCommentFromDifferentIssue_whenUpdateComment_thenThrowsEntityNotFoundException() {
        final String projectKey = "XIRA";
        final String issueKey = "XIRA-1";
        final String commentId = "comment-id";
        final UpdateCommentRequest request = new UpdateCommentRequest().content("New content");

        when(issueCommentRepository.findByIdAndIssueKeyAndIssueProjectKeyIgnoreCase(commentId, issueKey, projectKey))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> issueService.updateComment(projectKey, issueKey, commentId, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Comment not found");

        verify(issueCommentRepository, never()).save(any());
    }

    @Test
    void givenValidRequest_whenDeleteComment_thenDeletesComment() {
        final String projectKey = "XIRA";
        final String issueKey = "XIRA-1";
        final String commentId = "comment-id";

        final IssueComment comment = IssueComment.builder().id(commentId).content("Content").build();

        when(issueCommentRepository.findByIdAndIssueKeyAndIssueProjectKeyIgnoreCase(commentId, issueKey, projectKey))
                .thenReturn(Optional.of(comment));

        issueService.deleteComment(projectKey, issueKey, commentId);

        verify(issueCommentRepository).delete(comment);
    }

    @Test
    void givenCommentFromDifferentIssue_whenDeleteComment_thenThrowsEntityNotFoundException() {
        final String projectKey = "XIRA";
        final String issueKey = "XIRA-1";
        final String commentId = "comment-id";

        when(issueCommentRepository.findByIdAndIssueKeyAndIssueProjectKeyIgnoreCase(commentId, issueKey, projectKey))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> issueService.deleteComment(projectKey, issueKey, commentId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Comment not found");

        verify(issueCommentRepository, never()).delete(any());
    }

    @Test
    void givenNoFilters_whenGetIssues_thenReturnsUnfinishedIssuesFromUserProjects() {
        final String userId = "user-id";
        final XiraUser user = XiraUser.builder().id(userId).email("user@example.com").build();
        final Project project = Project.builder().id("project-id").key("XIRA").build();
        final WorkflowStatus status = WorkflowStatus.builder()
                .id("550e8400-e29b-41d4-a716-446655440100")
                .name("To Do")
                .category(WorkflowStatusCategory.TODO)
                .build();

        final Issue issue1 = Issue.builder()
                .id("issue1-id")
                .key("XIRA-1")
                .title("First Issue")
                .seqNo(1)
                .project(project)
                .status(status)
                .issueType(IssueType.BUG)
                .priority(IssuePriority.HIGH)
                .closedAt(null)
                .issueAssignees(Set.of())
                .build();

        final Issue issue2 = Issue.builder()
                .id("issue2-id")
                .key("XIRA-2")
                .title("Second Issue")
                .seqNo(2)
                .project(project)
                .status(status)
                .issueType(IssueType.TASK)
                .priority(IssuePriority.MEDIUM)
                .closedAt(null)
                .issueAssignees(Set.of())
                .build();

        when(authService.getAuthenticatedUser()).thenReturn(user);
        when(issueRepository.findAll(any(Specification.class))).thenReturn(List.of(issue1, issue2));

        final List<IssueSummaryResponse> result = issueService.getIssues(null, null, null, null);

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().getKey()).isEqualTo("XIRA-1");
        assertThat(result.get(1).getKey()).isEqualTo("XIRA-2");
        verify(issueRepository).findAll(any(Specification.class));
    }

    @Test
    void givenProjectKeyFilter_whenGetIssues_thenReturnsIssuesFromSpecificProject() {
        final String userId = "user-id";
        final XiraUser user = XiraUser.builder().id(userId).email("user@example.com").build();
        final Project project = Project.builder().id("project-id").key("XIRA").build();
        final WorkflowStatus status = WorkflowStatus.builder()
                .id("550e8400-e29b-41d4-a716-446655440100")
                .name("To Do")
                .category(WorkflowStatusCategory.TODO)
                .build();

        final Issue issue1 = Issue.builder()
                .id("issue1-id")
                .key("XIRA-1")
                .title("First Issue")
                .seqNo(1)
                .project(project)
                .status(status)
                .issueType(IssueType.BUG)
                .priority(IssuePriority.HIGH)
                .issueAssignees(Set.of())
                .build();

        when(authService.getAuthenticatedUser()).thenReturn(user);
        when(issueRepository.findAll(any(Specification.class))).thenReturn(List.of(issue1));

        final List<IssueSummaryResponse> result = issueService.getIssues("XIRA", null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getKey()).isEqualTo("XIRA-1");
    }

    @Test
    void givenAssigneeFilter_whenGetIssues_thenReturnsIssuesAssignedToUser() {
        final String userId = "550e8400-e29b-41d4-a716-446655440010";
        final String assigneeId = "550e8400-e29b-41d4-a716-446655440011";
        final XiraUser user = XiraUser.builder().id(userId).email("user@example.com").build();
        final XiraUser assignee = XiraUser.builder().id(assigneeId).email("assignee@example.com").build();
        final Project project = Project.builder().id("project-id").key("XIRA").build();
        final WorkflowStatus status = WorkflowStatus.builder()
                .id("550e8400-e29b-41d4-a716-446655440100")
                .name("To Do")
                .category(WorkflowStatusCategory.TODO)
                .build();

        final Issue issue1 = Issue.builder()
                .id("issue1-id")
                .key("XIRA-1")
                .title("Assigned Issue")
                .seqNo(1)
                .project(project)
                .status(status)
                .issueType(IssueType.BUG)
                .priority(IssuePriority.HIGH)
                .issueAssignees(Set
                        .of(IssueAssignee.builder().issueId("issue1-id").userId(assigneeId).xiraUser(assignee).build()))
                .build();

        when(authService.getAuthenticatedUser()).thenReturn(user);
        when(issueRepository.findAll(any(Specification.class))).thenReturn(List.of(issue1));

        final List<IssueSummaryResponse> result = issueService.getIssues(null, assigneeId, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getKey()).isEqualTo("XIRA-1");
    }

    @Test
    void givenIncludeFinishedTrue_whenGetIssues_thenReturnsAllIssuesIncludingFinished() {
        final String userId = "user-id";
        final XiraUser user = XiraUser.builder().id(userId).email("user@example.com").build();
        final Project project = Project.builder().id("project-id").key("XIRA").build();
        final WorkflowStatus todoStatus = WorkflowStatus.builder()
                .id("todo-id")
                .name("To Do")
                .category(WorkflowStatusCategory.TODO)
                .build();
        final WorkflowStatus doneStatus = WorkflowStatus.builder()
                .id("done-id")
                .name("Done")
                .category(WorkflowStatusCategory.DONE)
                .build();

        final Issue openIssue = Issue.builder()
                .id("issue1-id")
                .key("XIRA-1")
                .title("Open Issue")
                .seqNo(1)
                .project(project)
                .status(todoStatus)
                .issueType(IssueType.BUG)
                .priority(IssuePriority.HIGH)
                .closedAt(null)
                .issueAssignees(Set.of())
                .build();

        final Issue closedIssue = Issue.builder()
                .id("issue2-id")
                .key("XIRA-2")
                .title("Closed Issue")
                .seqNo(2)
                .project(project)
                .status(doneStatus)
                .issueType(IssueType.TASK)
                .priority(IssuePriority.MEDIUM)
                .closedAt(Instant.now())
                .issueAssignees(Set.of())
                .build();

        when(authService.getAuthenticatedUser()).thenReturn(user);
        when(issueRepository.findAll(any(Specification.class))).thenReturn(List.of(openIssue, closedIssue));

        final List<IssueSummaryResponse> result = issueService.getIssues(null, null, true, null);

        assertThat(result).hasSize(2);
    }

    @Test
    void givenIncludeFinishedFalse_whenGetIssues_thenReturnsOnlyUnfinishedIssues() {
        final String userId = "user-id";
        final XiraUser user = XiraUser.builder().id(userId).email("user@example.com").build();
        final Project project = Project.builder().id("project-id").key("XIRA").build();
        final WorkflowStatus status = WorkflowStatus.builder()
                .id("550e8400-e29b-41d4-a716-446655440100")
                .name("To Do")
                .category(WorkflowStatusCategory.TODO)
                .build();

        final Issue openIssue = Issue.builder()
                .id("issue1-id")
                .key("XIRA-1")
                .title("Open Issue")
                .seqNo(1)
                .project(project)
                .status(status)
                .issueType(IssueType.BUG)
                .priority(IssuePriority.HIGH)
                .closedAt(null)
                .issueAssignees(Set.of())
                .build();

        when(authService.getAuthenticatedUser()).thenReturn(user);
        when(issueRepository.findAll(any(Specification.class))).thenReturn(List.of(openIssue));

        final List<IssueSummaryResponse> result = issueService.getIssues(null, null, false, null);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getKey()).isEqualTo("XIRA-1");
    }

    @Test
    void givenSearchFilter_whenGetIssues_thenReturnsMatchingIssues() {
        final String userId = "user-id";
        final XiraUser user = XiraUser.builder().id(userId).email("user@example.com").build();
        final Project project = Project.builder().id("project-id").key("XIRA").build();
        final WorkflowStatus status = WorkflowStatus.builder()
                .id("550e8400-e29b-41d4-a716-446655440100")
                .name("To Do")
                .category(WorkflowStatusCategory.TODO)
                .build();

        final Issue matchingIssue = Issue.builder()
                .id("issue1-id")
                .key("XIRA-1")
                .title("Bug in login")
                .seqNo(1)
                .project(project)
                .status(status)
                .issueType(IssueType.BUG)
                .priority(IssuePriority.HIGH)
                .issueAssignees(Set.of())
                .build();

        when(authService.getAuthenticatedUser()).thenReturn(user);
        when(issueRepository.findAll(any(Specification.class))).thenReturn(List.of(matchingIssue));

        final List<IssueSummaryResponse> result = issueService.getIssues(null, null, null, "login");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getKey()).isEqualTo("XIRA-1");
    }

    @Test
    void givenMultipleIssues_whenGetIssues_thenReturnsSortedBySeqNo() {
        final String userId = "user-id";
        final XiraUser user = XiraUser.builder().id(userId).email("user@example.com").build();
        final Project project = Project.builder().id("project-id").key("XIRA").build();
        final WorkflowStatus status = WorkflowStatus.builder()
                .id("550e8400-e29b-41d4-a716-446655440100")
                .name("To Do")
                .category(WorkflowStatusCategory.TODO)
                .build();

        final Issue issue3 = Issue.builder()
                .id("issue3-id")
                .key("XIRA-3")
                .title("Third Issue")
                .seqNo(3)
                .project(project)
                .status(status)
                .issueType(IssueType.BUG)
                .priority(IssuePriority.HIGH)
                .issueAssignees(Set.of())
                .build();

        final Issue issue1 = Issue.builder()
                .id("issue1-id")
                .key("XIRA-1")
                .title("First Issue")
                .seqNo(1)
                .project(project)
                .status(status)
                .issueType(IssueType.BUG)
                .priority(IssuePriority.HIGH)
                .issueAssignees(Set.of())
                .build();

        final Issue issue2 = Issue.builder()
                .id("issue2-id")
                .key("XIRA-2")
                .title("Second Issue")
                .seqNo(2)
                .project(project)
                .status(status)
                .issueType(IssueType.BUG)
                .priority(IssuePriority.HIGH)
                .issueAssignees(Set.of())
                .build();

        when(authService.getAuthenticatedUser()).thenReturn(user);
        when(issueRepository.findAll(any(Specification.class))).thenReturn(List.of(issue3, issue1, issue2));

        final List<IssueSummaryResponse> result = issueService.getIssues(null, null, null, null);

        assertThat(result).hasSize(3);
        assertThat(result.getFirst().getKey()).isEqualTo("XIRA-1");
        assertThat(result.get(1).getKey()).isEqualTo("XIRA-2");
        assertThat(result.get(2).getKey()).isEqualTo("XIRA-3");
    }

    @Test
    void givenValidIssueKey_whenGetIssueDetails_thenReturnsIssueDetails() {
        final String projectKey = "XIRA";
        final String issueKey = "XIRA-1";
        final String userId = "550e8400-e29b-41d4-a716-446655440000";
        final Instant createdAt = Instant.now().minusSeconds(3600);

        final Project project = Project.builder().id("project-id").key(projectKey).build();
        final Workflow workflow = Workflow.builder().id("workflow-id").project(project).build();
        final WorkflowStatus status = WorkflowStatus.builder()
                .id("550e8400-e29b-41d4-a716-446655440100")
                .name("In Progress")
                .category(WorkflowStatusCategory.IN_PROGRESS)
                .workflow(workflow)
                .build();
        final XiraUser reporter = XiraUser.builder()
                .id(userId)
                .email("reporter@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        final Issue issue = Issue.builder()
                .id("issue-id")
                .key(issueKey)
                .title("Test Issue")
                .description("Test Description")
                .project(project)
                .status(status)
                .issueType(IssueType.BUG)
                .priority(IssuePriority.HIGH)
                .reporter(reporter)
                .createdAt(createdAt)
                .closedAt(null)
                .issueAssignees(Set.of())
                .sprintIssues(Set.of())
                .build();

        when(issueRepository.findWithDetailsByKeyAndProjectKeyIgnoreCase(issueKey, projectKey))
                .thenReturn(Optional.of(issue));

        final IssueDetailsResponse result = issueService.getIssueDetails(projectKey, issueKey);

        assertThat(result.getKey()).isEqualTo(issueKey);
        assertThat(result.getTitle()).isEqualTo("Test Issue");
        assertThat(result.getDescription()).isEqualTo("Test Description");
        assertThat(result.getStatus().getName()).isEqualTo("In Progress");
        assertThat(result.getStatus().getCategory()).isEqualTo(WorkflowStatusResponse.CategoryEnum.IN_PROGRESS);
        assertThat(result.getReporter().getEmail()).isEqualTo("reporter@example.com");
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getClosedAt()).isNull();
        assertThat(result.getSprintName()).isNull();
    }

    @Test
    void givenIssueWithSprint_whenGetIssueDetails_thenReturnsSprintName() {
        final String projectKey = "XIRA";
        final String issueKey = "XIRA-1";
        final Instant createdAt = Instant.now().minusSeconds(3600);

        final Project project = Project.builder().id("project-id").key(projectKey).build();
        final WorkflowStatus status = WorkflowStatus.builder()
                .id("550e8400-e29b-41d4-a716-446655440100")
                .name("To Do")
                .category(WorkflowStatusCategory.TODO)
                .build();
        final XiraUser reporter = XiraUser.builder()
                .id("550e8400-e29b-41d4-a716-446655440001")
                .email("reporter@example.com")
                .firstName("Jane")
                .lastName("Smith")
                .build();
        final Sprint sprint = Sprint.builder().id("sprint-id").name("Sprint 1").project(project).build();

        final Issue issue = Issue.builder()
                .id("issue-id")
                .key(issueKey)
                .title("Test Issue")
                .project(project)
                .status(status)
                .issueType(IssueType.TASK)
                .priority(IssuePriority.MEDIUM)
                .reporter(reporter)
                .createdAt(createdAt)
                .closedAt(null)
                .issueAssignees(Set.of())
                .sprintIssues(Set.of(SprintIssue.builder().sprintId("sprint-id").issueId("issue-id").sprint(sprint)
                        .build()))
                .build();

        when(issueRepository.findWithDetailsByKeyAndProjectKeyIgnoreCase(issueKey, projectKey))
                .thenReturn(Optional.of(issue));

        final IssueDetailsResponse result = issueService.getIssueDetails(projectKey, issueKey);

        assertThat(result.getSprintName()).isEqualTo("Sprint 1");
    }

    @Test
    void givenClosedIssue_whenGetIssueDetails_thenReturnsClosedAt() {
        final String projectKey = "XIRA";
        final String issueKey = "XIRA-1";
        final Instant createdAt = Instant.now().minusSeconds(7200);
        final Instant closedAt = Instant.now().minusSeconds(1800);

        final Project project = Project.builder().id("project-id").key(projectKey).build();
        final WorkflowStatus status = WorkflowStatus.builder()
                .id("550e8400-e29b-41d4-a716-446655440100")
                .name("Done")
                .category(WorkflowStatusCategory.DONE)
                .build();
        final XiraUser reporter = XiraUser.builder()
                .id("550e8400-e29b-41d4-a716-446655440002")
                .email("reporter@example.com")
                .firstName("Alice")
                .lastName("Brown")
                .build();

        final Issue issue = Issue.builder()
                .id("issue-id")
                .key(issueKey)
                .title("Closed Issue")
                .project(project)
                .status(status)
                .issueType(IssueType.BUG)
                .priority(IssuePriority.CRITICAL)
                .reporter(reporter)
                .createdAt(createdAt)
                .closedAt(closedAt)
                .issueAssignees(Set.of())
                .sprintIssues(Set.of())
                .build();

        when(issueRepository.findWithDetailsByKeyAndProjectKeyIgnoreCase(issueKey, projectKey))
                .thenReturn(Optional.of(issue));

        final IssueDetailsResponse result = issueService.getIssueDetails(projectKey, issueKey);

        assertThat(result.getClosedAt()).isNotNull();
        assertThat(result.getStatus().getCategory()).isEqualTo(WorkflowStatusResponse.CategoryEnum.DONE);
    }

    @Test
    void givenIssueWithAssignees_whenGetIssueDetails_thenReturnsAssignees() {
        final String projectKey = "XIRA";
        final String issueKey = "XIRA-1";
        final Instant createdAt = Instant.now().minusSeconds(3600);

        final Project project = Project.builder().id("project-id").key(projectKey).build();
        final WorkflowStatus status = WorkflowStatus.builder()
                .id("550e8400-e29b-41d4-a716-446655440100")
                .name("To Do")
                .category(WorkflowStatusCategory.TODO)
                .build();
        final XiraUser reporter = XiraUser.builder()
                .id("550e8400-e29b-41d4-a716-446655440003")
                .email("reporter@example.com")
                .firstName("Bob")
                .lastName("Wilson")
                .build();
        final XiraUser assignee1 = XiraUser.builder()
                .id("550e8400-e29b-41d4-a716-446655440004")
                .email("assignee1@example.com")
                .firstName("Alice")
                .lastName("Cooper")
                .build();
        final XiraUser assignee2 = XiraUser.builder()
                .id("550e8400-e29b-41d4-a716-446655440005")
                .email("assignee2@example.com")
                .firstName("Charlie")
                .lastName("Davis")
                .build();

        final Issue issue = Issue.builder()
                .id("issue-id")
                .key(issueKey)
                .title("Issue with Assignees")
                .project(project)
                .status(status)
                .issueType(IssueType.STORY)
                .priority(IssuePriority.LOW)
                .reporter(reporter)
                .createdAt(createdAt)
                .closedAt(null)
                .issueAssignees(Set.of(IssueAssignee.builder()
                        .issueId("issue-id")
                        .userId("550e8400-e29b-41d4-a716-446655440004")
                        .xiraUser(assignee1)
                        .build(), IssueAssignee.builder()
                        .issueId("issue-id")
                        .userId("550e8400-e29b-41d4-a716-446655440005")
                        .xiraUser(assignee2)
                        .build()))
                .sprintIssues(Set.of())
                .build();

        when(issueRepository.findWithDetailsByKeyAndProjectKeyIgnoreCase(issueKey, projectKey))
                .thenReturn(Optional.of(issue));

        final IssueDetailsResponse result = issueService.getIssueDetails(projectKey, issueKey);

        assertThat(result.getAssignees()).hasSize(2);
        assertThat(result.getAssignees().getFirst().getFirstName()).isEqualTo("Alice");
    }

    @Test
    void givenInvalidIssueKey_whenGetIssueDetails_thenThrowsEntityNotFoundException() {
        final String projectKey = "XIRA";
        final String issueKey = "XIRA-999";

        when(issueRepository.findWithDetailsByKeyAndProjectKeyIgnoreCase(issueKey, projectKey))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> issueService.getIssueDetails(projectKey, issueKey))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Issue not found");
    }

    @Test
    void givenIssueWithComments_whenGetIssueDetails_thenReturnsComments() {
        final String projectKey = "XIRA";
        final String issueKey = "XIRA-1";
        final Instant createdAt = Instant.now().minusSeconds(3600);
        final Instant comment1CreatedAt = Instant.now().minusSeconds(1800);
        final Instant comment2CreatedAt = Instant.now().minusSeconds(900);

        final Project project = Project.builder().id("project-id").key(projectKey).build();
        final WorkflowStatus status = WorkflowStatus.builder()
                .id("550e8400-e29b-41d4-a716-446655440100")
                .name("In Progress")
                .category(WorkflowStatusCategory.IN_PROGRESS)
                .build();
        final XiraUser reporter = XiraUser.builder()
                .id("550e8400-e29b-41d4-a716-446655440001")
                .email("reporter@example.com")
                .firstName("Reporter")
                .lastName("User")
                .build();
        final XiraUser commenter1 = XiraUser.builder()
                .id("550e8400-e29b-41d4-a716-446655440002")
                .email("commenter1@example.com")
                .firstName("Commenter")
                .lastName("One")
                .build();
        final XiraUser commenter2 = XiraUser.builder()
                .id("550e8400-e29b-41d4-a716-446655440003")
                .email("commenter2@example.com")
                .firstName("Commenter")
                .lastName("Two")
                .build();

        final Issue issue = Issue.builder()
                .id("issue-id")
                .key(issueKey)
                .project(project)
                .status(status)
                .priority(IssuePriority.HIGH)
                .issueType(IssueType.BUG)
                .reporter(reporter)
                .title("Test Issue")
                .description("Test Description")
                .createdAt(createdAt)
                .issueAssignees(new HashSet<>())
                .sprintIssues(new HashSet<>())
                .comments(new HashSet<>())
                .build();

        final IssueComment comment1 = IssueComment.builder()
                .id("550e8400-e29b-41d4-a716-446655440010")
                .issue(issue)
                .author(commenter1)
                .content("First comment")
                .createdAt(comment1CreatedAt)
                .updatedAt(comment1CreatedAt)
                .build();

        final IssueComment comment2 = IssueComment.builder()
                .id("550e8400-e29b-41d4-a716-446655440011")
                .issue(issue)
                .author(commenter2)
                .content("Second comment")
                .createdAt(comment2CreatedAt)
                .updatedAt(comment2CreatedAt)
                .build();

        issue.getComments().add(comment1);
        issue.getComments().add(comment2);

        when(issueRepository.findWithDetailsByKeyAndProjectKeyIgnoreCase(issueKey, projectKey))
                .thenReturn(Optional.of(issue));

        final IssueDetailsResponse result = issueService.getIssueDetails(projectKey, issueKey);

        assertThat(result.getComments()).hasSize(2);
        assertThat(result.getComments().getFirst().getContent()).isEqualTo("First comment");
        assertThat(result.getComments().getFirst().getAuthor().getFirstName()).isEqualTo("Commenter");
        assertThat(result.getComments().get(1).getContent()).isEqualTo("Second comment");
        assertThat(result.getComments().get(1).getAuthor().getFirstName()).isEqualTo("Commenter");
    }

    @Test
    void addIssueRelation_Creates_Blocks_Relation_When_Valid_Request() {
        final String projectKey = "XIRA";
        final String issueKey = "XIRA-1";
        final String relatedIssueKey = "XIRA-2";

        final Issue issue = Issue.builder()
                .id("issue-1-id")
                .key(issueKey)
                .title("Issue 1")
                .build();

        final Issue relatedIssue = Issue.builder()
                .id("issue-2-id")
                .key(relatedIssueKey)
                .title("Issue 2")
                .build();

        final AddIssueRelationRequest request = new AddIssueRelationRequest()
                .relatedIssueKey(relatedIssueKey)
                .relationType(AddIssueRelationRequest.RelationTypeEnum.BLOCKS);

        when(issueRepository.findByKeyAndProjectKeyIgnoreCase(issueKey, projectKey))
                .thenReturn(Optional.of(issue));
        when(issueRepository.findByKeyAndProjectKeyIgnoreCase(relatedIssueKey, projectKey))
                .thenReturn(Optional.of(relatedIssue));
        when(issueRelationRepository.existsByIssueIdAndRelatedIssueId(issue.getId(), relatedIssue.getId()))
                .thenReturn(false);

        issueService.addIssueRelation(projectKey, issueKey, request);

        verify(issueRelationRepository, times(2)).save(any(IssueRelation.class));
    }

    @Test
    void addIssueRelation_Creates_Blocked_By_Relation_When_Valid_Request() {
        final String projectKey = "XIRA";
        final String issueKey = "XIRA-1";
        final String relatedIssueKey = "XIRA-2";

        final Issue issue = Issue.builder()
                .id("issue-1-id")
                .key(issueKey)
                .title("Issue 1")
                .build();

        final Issue relatedIssue = Issue.builder()
                .id("issue-2-id")
                .key(relatedIssueKey)
                .title("Issue 2")
                .build();

        final AddIssueRelationRequest request = new AddIssueRelationRequest()
                .relatedIssueKey(relatedIssueKey)
                .relationType(AddIssueRelationRequest.RelationTypeEnum.BLOCKED_BY);

        when(issueRepository.findByKeyAndProjectKeyIgnoreCase(issueKey, projectKey))
                .thenReturn(Optional.of(issue));
        when(issueRepository.findByKeyAndProjectKeyIgnoreCase(relatedIssueKey, projectKey))
                .thenReturn(Optional.of(relatedIssue));
        when(issueRelationRepository.existsByIssueIdAndRelatedIssueId(issue.getId(), relatedIssue.getId()))
                .thenReturn(false);

        issueService.addIssueRelation(projectKey, issueKey, request);

        verify(issueRelationRepository, times(2)).save(any(IssueRelation.class));
    }

    @Test
    void addIssueRelation_Creates_Relates_To_Relation_When_Valid_Request() {
        final String projectKey = "XIRA";
        final String issueKey = "XIRA-1";
        final String relatedIssueKey = "XIRA-2";

        final Issue issue = Issue.builder()
                .id("issue-1-id")
                .key(issueKey)
                .title("Issue 1")
                .build();

        final Issue relatedIssue = Issue.builder()
                .id("issue-2-id")
                .key(relatedIssueKey)
                .title("Issue 2")
                .build();

        final AddIssueRelationRequest request = new AddIssueRelationRequest()
                .relatedIssueKey(relatedIssueKey)
                .relationType(AddIssueRelationRequest.RelationTypeEnum.RELATES_TO);

        when(issueRepository.findByKeyAndProjectKeyIgnoreCase(issueKey, projectKey))
                .thenReturn(Optional.of(issue));
        when(issueRepository.findByKeyAndProjectKeyIgnoreCase(relatedIssueKey, projectKey))
                .thenReturn(Optional.of(relatedIssue));
        when(issueRelationRepository.existsByIssueIdAndRelatedIssueId(issue.getId(), relatedIssue.getId()))
                .thenReturn(false);
        when(issueRelationRepository.existsByIssueIdAndRelatedIssueId(relatedIssue.getId(), issue.getId()))
                .thenReturn(false);

        issueService.addIssueRelation(projectKey, issueKey, request);

        verify(issueRelationRepository, times(2)).save(any(IssueRelation.class));
    }

    @Test
    void addIssueRelation_Throws_Exception_When_Issue_Not_Found() {
        final String projectKey = "XIRA";
        final String issueKey = "XIRA-1";
        final String relatedIssueKey = "XIRA-2";

        final AddIssueRelationRequest request = new AddIssueRelationRequest()
                .relatedIssueKey(relatedIssueKey)
                .relationType(AddIssueRelationRequest.RelationTypeEnum.BLOCKS);

        when(issueRepository.findByKeyAndProjectKeyIgnoreCase(issueKey, projectKey))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> issueService.addIssueRelation(projectKey, issueKey, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Issue not found");
    }

    @Test
    void addIssueRelation_Throws_Exception_When_Related_Issue_Not_Found() {
        final String projectKey = "XIRA";
        final String issueKey = "XIRA-1";
        final String relatedIssueKey = "XIRA-2";

        final Issue issue = Issue.builder()
                .id("issue-1-id")
                .key(issueKey)
                .title("Issue 1")
                .build();

        final AddIssueRelationRequest request = new AddIssueRelationRequest()
                .relatedIssueKey(relatedIssueKey)
                .relationType(AddIssueRelationRequest.RelationTypeEnum.BLOCKS);

        when(issueRepository.findByKeyAndProjectKeyIgnoreCase(issueKey, projectKey))
                .thenReturn(Optional.of(issue));
        when(issueRepository.findByKeyAndProjectKeyIgnoreCase(relatedIssueKey, projectKey))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> issueService.addIssueRelation(projectKey, issueKey, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Related issue not found");
    }

    @Test
    void addIssueRelation_Throws_Exception_When_Issue_Relates_To_Itself() {
        final String projectKey = "XIRA";
        final String issueKey = "XIRA-1";

        final Issue issue = Issue.builder()
                .id("issue-1-id")
                .key(issueKey)
                .title("Issue 1")
                .build();

        final AddIssueRelationRequest request = new AddIssueRelationRequest()
                .relatedIssueKey(issueKey)
                .relationType(AddIssueRelationRequest.RelationTypeEnum.BLOCKS);

        when(issueRepository.findByKeyAndProjectKeyIgnoreCase(issueKey, projectKey))
                .thenReturn(Optional.of(issue));

        assertThatThrownBy(() -> issueService.addIssueRelation(projectKey, issueKey, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot relate an issue to itself");
    }

    @Test
    void addIssueRelation_Throws_Exception_When_Relation_Already_Exists() {
        final String projectKey = "XIRA";
        final String issueKey = "XIRA-1";
        final String relatedIssueKey = "XIRA-2";

        final Issue issue = Issue.builder()
                .id("issue-1-id")
                .key(issueKey)
                .title("Issue 1")
                .build();

        final Issue relatedIssue = Issue.builder()
                .id("issue-2-id")
                .key(relatedIssueKey)
                .title("Issue 2")
                .build();

        final AddIssueRelationRequest request = new AddIssueRelationRequest()
                .relatedIssueKey(relatedIssueKey)
                .relationType(AddIssueRelationRequest.RelationTypeEnum.BLOCKS);

        when(issueRepository.findByKeyAndProjectKeyIgnoreCase(issueKey, projectKey))
                .thenReturn(Optional.of(issue));
        when(issueRepository.findByKeyAndProjectKeyIgnoreCase(relatedIssueKey, projectKey))
                .thenReturn(Optional.of(relatedIssue));
        when(issueRelationRepository.existsByIssueIdAndRelatedIssueId(issue.getId(), relatedIssue.getId()))
                .thenReturn(true);

        assertThatThrownBy(() -> issueService.addIssueRelation(projectKey, issueKey, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Relation already exists");
    }

    @Test
    void removeIssueRelation_Deletes_Relation_When_Valid_Request() {
        final String projectKey = "XIRA";
        final String issueKey = "XIRA-1";
        final String relatedIssueKey = "XIRA-2";

        final Issue issue = Issue.builder()
                .id("issue-1-id")
                .key(issueKey)
                .title("Issue 1")
                .build();

        final Issue relatedIssue = Issue.builder()
                .id("issue-2-id")
                .key(relatedIssueKey)
                .title("Issue 2")
                .build();

        final IssueRelation relation = IssueRelation.builder()
                .issueId(issue.getId())
                .relatedIssueId(relatedIssue.getId())
                .issue(issue)
                .relatedIssue(relatedIssue)
                .relationType(IssueRelationType.BLOCKS)
                .build();

        when(issueRepository.findByKeyAndProjectKeyIgnoreCase(issueKey, projectKey))
                .thenReturn(Optional.of(issue));
        when(issueRepository.findByKeyAndProjectKeyIgnoreCase(relatedIssueKey, projectKey))
                .thenReturn(Optional.of(relatedIssue));
        when(issueRelationRepository.findByIssueIdAndRelatedIssueId(issue.getId(), relatedIssue.getId()))
                .thenReturn(Optional.of(relation));

        issueService.removeIssueRelation(projectKey, issueKey, relatedIssueKey);

        verify(issueRelationRepository).deleteByIssueIdAndRelatedIssueId(issue.getId(), relatedIssue.getId());
        verify(issueRelationRepository).deleteByIssueIdAndRelatedIssueId(relatedIssue.getId(), issue.getId());
    }

    @Test
    void removeIssueRelation_Throws_Exception_When_Issue_Not_Found() {
        final String projectKey = "XIRA";
        final String issueKey = "XIRA-1";
        final String relatedIssueKey = "XIRA-2";

        when(issueRepository.findByKeyAndProjectKeyIgnoreCase(issueKey, projectKey))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> issueService.removeIssueRelation(projectKey, issueKey, relatedIssueKey))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Issue not found");
    }

    @Test
    void removeIssueRelation_Throws_Exception_When_Related_Issue_Not_Found() {
        final String projectKey = "XIRA";
        final String issueKey = "XIRA-1";
        final String relatedIssueKey = "XIRA-2";

        final Issue issue = Issue.builder()
                .id("issue-1-id")
                .key(issueKey)
                .title("Issue 1")
                .build();

        when(issueRepository.findByKeyAndProjectKeyIgnoreCase(issueKey, projectKey))
                .thenReturn(Optional.of(issue));
        when(issueRepository.findByKeyAndProjectKeyIgnoreCase(relatedIssueKey, projectKey))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> issueService.removeIssueRelation(projectKey, issueKey, relatedIssueKey))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Related issue not found");
    }

    @Test
    void removeIssueRelation_Throws_Exception_When_Relation_Not_Found() {
        final String projectKey = "XIRA";
        final String issueKey = "XIRA-1";
        final String relatedIssueKey = "XIRA-2";

        final Issue issue = Issue.builder()
                .id("issue-1-id")
                .key(issueKey)
                .title("Issue 1")
                .build();

        final Issue relatedIssue = Issue.builder()
                .id("issue-2-id")
                .key(relatedIssueKey)
                .title("Issue 2")
                .build();

        when(issueRepository.findByKeyAndProjectKeyIgnoreCase(issueKey, projectKey))
                .thenReturn(Optional.of(issue));
        when(issueRepository.findByKeyAndProjectKeyIgnoreCase(relatedIssueKey, projectKey))
                .thenReturn(Optional.of(relatedIssue));
        when(issueRelationRepository.findByIssueIdAndRelatedIssueId(issue.getId(), relatedIssue.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> issueService.removeIssueRelation(projectKey, issueKey, relatedIssueKey))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Relation not found");
    }
}
