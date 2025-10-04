package eu.itsonix.genai.xira.jpa.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import eu.itsonix.genai.xira.jpa.entity.ProjectMember;
import eu.itsonix.genai.xira.jpa.entity.ProjectMemberId;
import eu.itsonix.genai.xira.jpa.entity.ProjectRole;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, ProjectMemberId> {

    boolean existsByProject_KeyAndXiraUser_EmailAndRole(final String projectKey, final String email,
            final ProjectRole role);

    boolean existsByProject_KeyIgnoreCaseAndXiraUser_EmailIgnoreCaseAndRole(String projectKey, String email,
            ProjectRole role);

    boolean existsByProject_KeyIgnoreCaseAndXiraUser_EmailIgnoreCase(String projectKey, String email);

    Optional<ProjectMember> findByProject_KeyIgnoreCaseAndXiraUser_EmailIgnoreCase(String projectKey, String email);
}