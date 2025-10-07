package eu.itsonix.genai.xira.jpa.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import eu.itsonix.genai.xira.jpa.entity.XiraUser;

public interface XiraUserRepository extends JpaRepository<XiraUser, String>, JpaSpecificationExecutor<XiraUser> {

    Optional<XiraUser> findByEmailIgnoreCase(final String email);

    boolean existsByEmailIgnoreCase(final String email);
}
