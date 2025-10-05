package eu.itsonix.genai.xira.service;

import jakarta.persistence.EntityNotFoundException;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.itsonix.genai.xira.jpa.entity.XiraUser;
import eu.itsonix.genai.xira.jpa.repository.XiraUserRepository;
import eu.itsonix.genai.xira.web.model.UserResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private XiraUserRepository xiraUserRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void givenValidEmail_thenReturnsUserResponse() {
        final XiraUser user = XiraUser.builder()
                .id("550e8400-e29b-41d4-a716-446655440000")
                .email("user@example.com")
                .firstName("John")
                .lastName("Doe")
                .passwordHash("hash")
                .build();

        when(xiraUserRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));

        final UserResponse response = userService.findByEmail("user@example.com");

        assertThat(response.getId().toString()).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
        assertThat(response.getEmail()).isEqualTo("user@example.com");
        assertThat(response.getFirstName()).isEqualTo("John");
        assertThat(response.getLastName()).isEqualTo("Doe");
    }

    @Test
    void givenNonExistentEmail_thenThrowsEntityNotFound() {
        when(xiraUserRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findByEmail("missing@example.com"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    void givenEmailWithDifferentCase_thenReturnsUserResponse() {
        final XiraUser user = XiraUser.builder()
                .id("550e8400-e29b-41d4-a716-446655440000")
                .email("user@example.com")
                .firstName("Jane")
                .lastName("Smith")
                .passwordHash("hash")
                .build();

        when(xiraUserRepository.findByEmailIgnoreCase("USER@EXAMPLE.COM")).thenReturn(Optional.of(user));

        final UserResponse response = userService.findByEmail("USER@EXAMPLE.COM");

        assertThat(response.getId().toString()).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
        assertThat(response.getEmail()).isEqualTo("user@example.com");
    }
}
