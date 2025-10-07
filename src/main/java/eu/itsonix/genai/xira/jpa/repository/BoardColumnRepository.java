package eu.itsonix.genai.xira.jpa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import eu.itsonix.genai.xira.jpa.entity.BoardColumn;

@Repository
public interface BoardColumnRepository extends JpaRepository<BoardColumn, String> {

    @EntityGraph(attributePaths = { "board", "boardColumnWorkflowStatuses", "boardColumnWorkflowStatuses.workflowStatus" })
    List<BoardColumn> findByBoardIdOrderByColumnOrder(final String boardId);
}