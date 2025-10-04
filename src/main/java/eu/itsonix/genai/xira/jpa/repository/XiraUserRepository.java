package eu.itsonix.genai.xira.jpa.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import eu.itsonix.genai.xira.jpa.entity.XiraUser;

public interface XiraUserRepository extends JpaRepository<XiraUser, String> {

    Optional<XiraUser> findByEmailIgnoreCase(final String email);

    boolean existsByEmailIgnoreCase(final String email);
}
