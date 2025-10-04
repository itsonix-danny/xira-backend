package eu.itsonix.genai.xira.jpa.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import eu.itsonix.genai.xira.jpa.entity.Project;

public interface ProjectRepository extends JpaRepository<Project, String> {

    boolean existsByKeyIgnoreCase(final String key);

    Optional<Project> findByKeyIgnoreCase(final String key);
}
