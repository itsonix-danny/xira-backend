package eu.itsonix.genai.xira.service;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.itsonix.genai.xira.jpa.entity.Project;
import eu.itsonix.genai.xira.jpa.entity.Workflow;
import eu.itsonix.genai.xira.jpa.entity.WorkflowStatus;
import eu.itsonix.genai.xira.jpa.entity.WorkflowStatusCategory;
import eu.itsonix.genai.xira.jpa.repository.WorkflowStatusRepository;
import eu.itsonix.genai.xira.web.model.WorkflowStatusResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {

    @Mock
    private WorkflowStatusRepository workflowStatusRepository;

    @InjectMocks
    private WorkflowService workflowService;

    @Test
    void givenProject_whenCreateDefaultWorkflow_thenCreatesWorkflowWithThreeStatuses() {
        final Project project = Project.builder().id("project-id").key("XIRA").name("Xira").build();

        final Workflow result = workflowService.createDefaultWorkflow(project);

        assertThat(result).isNotNull();
        assertThat(result.getProject()).isEqualTo(project);
        assertThat(result.getWorkflowStatuses()).hasSize(3);

        assertThat(result.getWorkflowStatuses().getFirst().getName()).isEqualTo("To Do");
        assertThat(result.getWorkflowStatuses().getFirst().getCategory()).isEqualTo(WorkflowStatusCategory.TODO);
        assertThat(result.getWorkflowStatuses().getFirst().getStatusOrder()).isEqualTo(1);

        assertThat(result.getWorkflowStatuses().get(1).getName()).isEqualTo("In Progress");
        assertThat(result.getWorkflowStatuses().get(1).getCategory()).isEqualTo(WorkflowStatusCategory.IN_PROGRESS);
        assertThat(result.getWorkflowStatuses().get(1).getStatusOrder()).isEqualTo(2);

        assertThat(result.getWorkflowStatuses().get(2).getName()).isEqualTo("Done");
        assertThat(result.getWorkflowStatuses().get(2).getCategory()).isEqualTo(WorkflowStatusCategory.DONE);
        assertThat(result.getWorkflowStatuses().get(2).getStatusOrder()).isEqualTo(3);
    }

    @Test
    void givenValidProjectKey_whenGetWorkflowStatuses_thenReturnsStatusesSortedByOrder() {
        final String projectKey = "XIRA";

        final List<WorkflowStatus> workflowStatuses = List.of(
                WorkflowStatus.builder()
                        .id("550e8400-e29b-41d4-a716-446655440001")
                        .name("To Do")
                        .category(WorkflowStatusCategory.TODO)
                        .statusOrder(1)
                        .build(),
                WorkflowStatus.builder()
                        .id("550e8400-e29b-41d4-a716-446655440002")
                        .name("In Progress")
                        .category(WorkflowStatusCategory.IN_PROGRESS)
                        .statusOrder(2)
                        .build(),
                WorkflowStatus.builder()
                        .id("550e8400-e29b-41d4-a716-446655440003")
                        .name("Done")
                        .category(WorkflowStatusCategory.DONE)
                        .statusOrder(3)
                        .build());

        when(workflowStatusRepository.findByWorkflowProjectKeyIgnoreCaseOrderByStatusOrderAsc(projectKey))
                .thenReturn(workflowStatuses);

        final List<WorkflowStatusResponse> result = workflowService.getWorkflowStatuses(projectKey);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getName()).isEqualTo("To Do");
        assertThat(result.get(0).getCategory()).isEqualTo(WorkflowStatusResponse.CategoryEnum.TODO);
        assertThat(result.get(1).getName()).isEqualTo("In Progress");
        assertThat(result.get(1).getCategory()).isEqualTo(WorkflowStatusResponse.CategoryEnum.IN_PROGRESS);
        assertThat(result.get(2).getName()).isEqualTo("Done");
        assertThat(result.get(2).getCategory()).isEqualTo(WorkflowStatusResponse.CategoryEnum.DONE);
    }
}