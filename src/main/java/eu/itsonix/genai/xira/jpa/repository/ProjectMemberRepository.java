package eu.itsonix.genai.xira.jpa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import eu.itsonix.genai.xira.jpa.entity.ProjectMember;
import eu.itsonix.genai.xira.jpa.entity.ProjectMemberId;
import eu.itsonix.genai.xira.jpa.entity.ProjectRole;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, ProjectMemberId> {

    boolean existsByProject_KeyIgnoreCaseAndXiraUser_EmailIgnoreCaseAndRole(String projectKey, String email,
            ProjectRole role);

    boolean existsByProjectIdAndUserId(String projectId, String userId);

    Optional<ProjectMember> findByProjectIdAndUserId(String projectId, String userId);

    List<ProjectMember> findAllByUserId(String userId);
}
