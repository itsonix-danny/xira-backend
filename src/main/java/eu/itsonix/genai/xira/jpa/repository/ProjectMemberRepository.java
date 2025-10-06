package eu.itsonix.genai.xira.jpa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import eu.itsonix.genai.xira.jpa.entity.ProjectMember;
import eu.itsonix.genai.xira.jpa.entity.ProjectMemberId;
import eu.itsonix.genai.xira.jpa.entity.ProjectRole;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, ProjectMemberId> {

    boolean existsByProject_KeyIgnoreCaseAndXiraUser_EmailIgnoreCaseAndRole(final String projectKey, final String email,
            final ProjectRole role);

    boolean existsByProject_KeyIgnoreCaseAndXiraUser_EmailIgnoreCase(final String projectKey, final String email);

    boolean existsByProject_KeyIgnoreCaseAndUserId(final String projectKey, final String userId);

    boolean existsByProjectIdAndUserId(final String projectId, final String userId);

    Optional<ProjectMember> findByProjectIdAndUserId(final String projectId, final String userId);

    @EntityGraph(attributePaths = { "project", "project.owner", "project.boards" })
    List<ProjectMember> findAllByUserId(final String userId);

    @EntityGraph(attributePaths = { "project", "project.owner", "project.boards" })
    Optional<ProjectMember> findByProject_KeyIgnoreCaseAndUserId(final String projectKey, final String userId);

    @EntityGraph(attributePaths = { "project", "xiraUser" })
    List<ProjectMember> findAllByProject_KeyIgnoreCase(final String projectKey);
}
