package eu.itsonix.genai.xira.mapper;

import java.util.UUID;

import eu.itsonix.genai.xira.jpa.entity.WorkflowStatus;
import eu.itsonix.genai.xira.web.model.WorkflowStatusResponse;

public final class WorkflowMapper {

    private WorkflowMapper() {
    }

    public static WorkflowStatusResponse toWorkflowStatusResponse(final WorkflowStatus workflowStatus) {
        return new WorkflowStatusResponse().id(UUID.fromString(workflowStatus.getId()))
                .name(workflowStatus.getName())
                .category(WorkflowStatusResponse.CategoryEnum.fromValue(workflowStatus.getCategory().name()));
    }
}
