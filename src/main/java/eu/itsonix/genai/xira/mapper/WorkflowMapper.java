package eu.itsonix.genai.xira.mapper;

import eu.itsonix.genai.xira.jpa.entity.WorkflowStatus;
import eu.itsonix.genai.xira.web.model.WorkflowStatusResponse;

import java.util.UUID;

public final class WorkflowMapper {

    private WorkflowMapper() {
    }

    public static WorkflowStatusResponse toWorkflowStatusResponse(final WorkflowStatus workflowStatus) {
        return new WorkflowStatusResponse().id(UUID.fromString(workflowStatus.getId()))
                .name(workflowStatus.getName())
                .category(WorkflowStatusResponse.CategoryEnum.fromValue(workflowStatus.getCategory().name()));
    }
}
