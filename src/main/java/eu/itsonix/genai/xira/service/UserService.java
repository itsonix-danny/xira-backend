package eu.itsonix.genai.xira.service;

import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import eu.itsonix.genai.xira.jpa.entity.XiraUser;
import eu.itsonix.genai.xira.jpa.repository.XiraUserRepository;
import eu.itsonix.genai.xira.jpa.specification.UserSpecification;
import eu.itsonix.genai.xira.mapper.UserMapper;
import eu.itsonix.genai.xira.web.model.UserResponse;

@Service
@RequiredArgsConstructor
public class UserService {

    private final XiraUserRepository xiraUserRepository;

    @Transactional(readOnly = true)
    public List<UserResponse> searchUsers(final String email, final String projectKey) {
        final Specification<XiraUser> spec = UserSpecification.searchUsers(email, projectKey);

        return xiraUserRepository.findAll(spec).stream().map(UserMapper::toUserResponse).toList();
    }
}
