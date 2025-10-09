package eu.itsonix.genai.xira.jpa.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import eu.itsonix.genai.xira.jpa.entity.Sprint;
import eu.itsonix.genai.xira.jpa.entity.SprintState;

@Repository
public interface SprintRepository extends JpaRepository<Sprint, String> {

    Optional<Sprint> findByProject_KeyIgnoreCaseAndId(final String projectKey, final String id);

    boolean existsByProjectIdAndStateAndIdNot(final String projectId, final SprintState state, final String id);
}
