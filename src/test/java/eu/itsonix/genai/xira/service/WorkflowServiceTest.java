package eu.itsonix.genai.xira.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.itsonix.genai.xira.jpa.entity.Project;
import eu.itsonix.genai.xira.jpa.entity.Workflow;
import eu.itsonix.genai.xira.jpa.entity.WorkflowStatusCategory;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {

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
}