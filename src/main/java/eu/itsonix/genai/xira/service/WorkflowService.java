package eu.itsonix.genai.xira.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import eu.itsonix.genai.xira.jpa.entity.Project;
import eu.itsonix.genai.xira.jpa.entity.Workflow;
import eu.itsonix.genai.xira.jpa.entity.WorkflowStatus;
import eu.itsonix.genai.xira.jpa.entity.WorkflowStatusCategory;
import eu.itsonix.genai.xira.jpa.repository.WorkflowStatusRepository;

@Service
@RequiredArgsConstructor
public class WorkflowService {

    private static final String STATUS_TODO = "To Do";
    private static final String STATUS_IN_PROGRESS = "In Progress";
    private static final String STATUS_DONE = "Done";

    private final WorkflowStatusRepository workflowStatusRepository;

    public Workflow createDefaultWorkflow(final Project project) {
        final Workflow workflow = Workflow.builder().project(project).build();

        final List<WorkflowStatus> workflowStatuses = List.of(
                WorkflowStatus.builder()
                        .workflow(workflow)
                        .name(STATUS_TODO)
                        .category(WorkflowStatusCategory.TODO)
                        .statusOrder(1)
                        .build(),
                WorkflowStatus.builder()
                        .workflow(workflow)
                        .name(STATUS_IN_PROGRESS)
                        .category(WorkflowStatusCategory.IN_PROGRESS)
                        .statusOrder(2)
                        .build(),
                WorkflowStatus.builder()
                        .workflow(workflow)
                        .name(STATUS_DONE)
                        .category(WorkflowStatusCategory.DONE)
                        .statusOrder(3)
                        .build());

        workflow.setWorkflowStatuses(workflowStatuses);

        return workflow;
    }

    public Map<WorkflowStatusCategory, WorkflowStatus> getDefaultStatusesByCategory(final String projectId) {
        return workflowStatusRepository.findByWorkflowProjectIdOrderByStatusOrderAsc(projectId)
                .stream()
                .collect(Collectors.toMap(WorkflowStatus::getCategory, Function.identity(), (first, _) -> first));
    }
}