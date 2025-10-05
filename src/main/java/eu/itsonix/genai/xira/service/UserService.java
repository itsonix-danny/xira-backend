package eu.itsonix.genai.xira.service;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import eu.itsonix.genai.xira.jpa.entity.XiraUser;
import eu.itsonix.genai.xira.jpa.repository.XiraUserRepository;
import eu.itsonix.genai.xira.mapper.UserMapper;
import eu.itsonix.genai.xira.web.model.UserResponse;

@Service
@RequiredArgsConstructor
public class UserService {

    private final XiraUserRepository xiraUserRepository;

    @Transactional(readOnly = true)
    public UserResponse findByEmail(final String email) {
        final XiraUser user = xiraUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        return UserMapper.toUserResponse(user);
    }
}
