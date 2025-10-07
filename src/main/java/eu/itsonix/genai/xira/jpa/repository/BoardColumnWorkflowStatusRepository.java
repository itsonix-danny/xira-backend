package eu.itsonix.genai.xira.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import eu.itsonix.genai.xira.jpa.entity.BoardColumnWorkflowStatus;
import eu.itsonix.genai.xira.jpa.entity.BoardColumnWorkflowStatusId;

@Repository
public interface BoardColumnWorkflowStatusRepository
        extends JpaRepository<BoardColumnWorkflowStatus, BoardColumnWorkflowStatusId> {
}