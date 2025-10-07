package eu.itsonix.genai.xira.jpa.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import eu.itsonix.genai.xira.jpa.entity.Board;
import eu.itsonix.genai.xira.jpa.entity.BoardType;

@Repository
public interface BoardRepository extends JpaRepository<Board, String> {

    boolean existsByProjectIdAndType(final String projectId, final BoardType type);

    Integer countByProjectId(final String projectId);

    @EntityGraph(attributePaths = "project")
    Optional<Board> findByProjectKeyIgnoreCaseAndBoardNumber(final String projectKey, final Integer boardNumber);

    @EntityGraph(attributePaths = "project")
    Optional<Board> findByProjectKeyIgnoreCaseAndBoardNumberAndType(final String projectKey, final Integer boardNumber,
            final BoardType type);
}
