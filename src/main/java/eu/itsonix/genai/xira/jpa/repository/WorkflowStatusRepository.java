package eu.itsonix.genai.xira.jpa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import eu.itsonix.genai.xira.jpa.entity.WorkflowStatus;

@Repository
public interface WorkflowStatusRepository extends JpaRepository<WorkflowStatus, String> {

    List<WorkflowStatus> findByWorkflowProjectIdOrderByStatusOrderAsc(final String projectId);

    List<WorkflowStatus> findByWorkflowProjectKeyIgnoreCaseOrderByStatusOrderAsc(final String projectKey);

    Optional<WorkflowStatus> findFirstByWorkflowProjectIdOrderByStatusOrderAsc(final String projectId);

    Optional<WorkflowStatus> findByIdAndWorkflowProjectKeyIgnoreCase(final String id, final String projectKey);
}