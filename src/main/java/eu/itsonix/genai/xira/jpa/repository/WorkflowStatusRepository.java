package eu.itsonix.genai.xira.jpa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import eu.itsonix.genai.xira.jpa.entity.WorkflowStatus;

@Repository
public interface WorkflowStatusRepository extends JpaRepository<WorkflowStatus, String> {

    List<WorkflowStatus> findByWorkflowProjectIdOrderByStatusOrderAsc(final String projectId);
}