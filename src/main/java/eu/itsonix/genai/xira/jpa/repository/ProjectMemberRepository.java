package eu.itsonix.genai.xira.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import eu.itsonix.genai.xira.jpa.entity.ProjectMember;
import eu.itsonix.genai.xira.jpa.entity.ProjectMemberId;
import eu.itsonix.genai.xira.jpa.entity.ProjectRole;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, ProjectMemberId> {

    boolean existsByProject_KeyAndXiraUser_EmailAndRole(final String projectKey, final String email,
            final ProjectRole role);
}