package eu.itsonix.genai.xira.jpa.specification;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.data.jpa.domain.Specification;

import eu.itsonix.genai.xira.jpa.entity.ProjectMember;
import eu.itsonix.genai.xira.jpa.entity.XiraUser;

public final class UserSpecification {

    private UserSpecification() {
    }

    public static Specification<XiraUser> searchUsers(final String email, final String projectKey) {
        return (root, query, cb) -> {
            final List<Predicate> predicates = new ArrayList<>();

            if (email != null) {
                predicates.add(cb.equal(cb.lower(root.get("email")), email.toLowerCase()));
            }

            if (projectKey != null) {
                final Subquery<String> subquery = Objects.requireNonNull(query).subquery(String.class);
                final Root<ProjectMember> pmRoot = subquery.from(ProjectMember.class);
                subquery.select(pmRoot.get("userId"))
                        .where(cb.equal(cb.lower(pmRoot.get("project").get("key")), projectKey.toLowerCase()));

                predicates.add(cb.not(root.get("id").in(subquery)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
