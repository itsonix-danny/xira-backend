package eu.itsonix.genai.xira.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import eu.itsonix.genai.xira.jpa.entity.Board;
import eu.itsonix.genai.xira.jpa.entity.BoardType;

@Repository
public interface BoardRepository extends JpaRepository<Board, String> {

    boolean existsByProjectIdAndType(String projectId, BoardType type);
}
