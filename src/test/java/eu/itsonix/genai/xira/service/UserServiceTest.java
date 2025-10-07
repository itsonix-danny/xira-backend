package eu.itsonix.genai.xira.service;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import eu.itsonix.genai.xira.jpa.entity.XiraUser;
import eu.itsonix.genai.xira.jpa.repository.XiraUserRepository;
import eu.itsonix.genai.xira.web.model.UserResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private XiraUserRepository xiraUserRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void givenNoFilters_thenReturnsAllUsers() {
        final XiraUser user1 = XiraUser.builder()
                .id("550e8400-e29b-41d4-a716-446655440000")
                .email("user1@example.com")
                .firstName("John")
                .lastName("Doe")
                .passwordHash("hash")
                .build();

        final XiraUser user2 = XiraUser.builder()
                .id("550e8400-e29b-41d4-a716-446655440001")
                .email("user2@example.com")
                .firstName("Jane")
                .lastName("Smith")
                .passwordHash("hash")
                .build();

        when(xiraUserRepository.findAll(ArgumentMatchers.<Specification<XiraUser>>any()))
                .thenReturn(List.of(user1, user2));

        final List<UserResponse> responses = userService.searchUsers(null, null);

        assertThat(responses).hasSize(2);
        assertThat(responses.getFirst().getEmail()).isEqualTo("user1@example.com");
        assertThat(responses.get(1).getEmail()).isEqualTo("user2@example.com");
    }

    @Test
    void givenExactEmail_thenReturnsSingleUserInList() {
        final XiraUser user = XiraUser.builder()
                .id("550e8400-e29b-41d4-a716-446655440000")
                .email("user@example.com")
                .firstName("John")
                .lastName("Doe")
                .passwordHash("hash")
                .build();

        when(xiraUserRepository.findAll(ArgumentMatchers.<Specification<XiraUser>>any())).thenReturn(List.of(user));

        final List<UserResponse> responses = userService.searchUsers("user@example.com", null);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().getId().toString()).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
        assertThat(responses.getFirst().getEmail()).isEqualTo("user@example.com");
        assertThat(responses.getFirst().getFirstName()).isEqualTo("John");
        assertThat(responses.getFirst().getLastName()).isEqualTo("Doe");
    }

    @Test
    void givenNonExistentEmail_thenReturnsEmptyList() {
        when(xiraUserRepository.findAll(ArgumentMatchers.<Specification<XiraUser>>any())).thenReturn(List.of());

        final List<UserResponse> responses = userService.searchUsers("missing@example.com", null);

        assertThat(responses).isEmpty();
    }

    @Test
    void givenEmailWithDifferentCase_thenReturnsSingleUserInList() {
        final XiraUser user = XiraUser.builder()
                .id("550e8400-e29b-41d4-a716-446655440000")
                .email("user@example.com")
                .firstName("Jane")
                .lastName("Smith")
                .passwordHash("hash")
                .build();

        when(xiraUserRepository.findAll(ArgumentMatchers.<Specification<XiraUser>>any())).thenReturn(List.of(user));

        final List<UserResponse> responses = userService.searchUsers("USER@EXAMPLE.COM", null);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().getId().toString()).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
        assertThat(responses.getFirst().getEmail()).isEqualTo("user@example.com");
    }

    @Test
    void givenProjectKeyOnly_thenReturnsUsersNotInProject() {
        final XiraUser user1 = XiraUser.builder()
                .id("550e8400-e29b-41d4-a716-446655440000")
                .email("user1@example.com")
                .firstName("John")
                .lastName("Doe")
                .passwordHash("hash")
                .build();

        final XiraUser user2 = XiraUser.builder()
                .id("550e8400-e29b-41d4-a716-446655440001")
                .email("user2@example.com")
                .firstName("Jane")
                .lastName("Smith")
                .passwordHash("hash")
                .build();

        when(xiraUserRepository.findAll(ArgumentMatchers.<Specification<XiraUser>>any()))
                .thenReturn(List.of(user1, user2));

        final List<UserResponse> responses = userService.searchUsers(null, "PROJ");

        assertThat(responses).hasSize(2);
        assertThat(responses.getFirst().getEmail()).isEqualTo("user1@example.com");
        assertThat(responses.get(1).getEmail()).isEqualTo("user2@example.com");
    }

    @Test
    void givenEmailAndProjectKey_whenUserNotInProject_thenReturnsUser() {
        final XiraUser user = XiraUser.builder()
                .id("550e8400-e29b-41d4-a716-446655440000")
                .email("user@example.com")
                .firstName("John")
                .lastName("Doe")
                .passwordHash("hash")
                .build();

        when(xiraUserRepository.findAll(ArgumentMatchers.<Specification<XiraUser>>any())).thenReturn(List.of(user));

        final List<UserResponse> responses = userService.searchUsers("user@example.com", "PROJ");

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().getId().toString()).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
        assertThat(responses.getFirst().getEmail()).isEqualTo("user@example.com");
    }

    @Test
    void givenEmailAndProjectKey_whenUserInProject_thenReturnsEmptyList() {
        when(xiraUserRepository.findAll(ArgumentMatchers.<Specification<XiraUser>>any())).thenReturn(List.of());

        final List<UserResponse> responses = userService.searchUsers("user@example.com", "PROJ");

        assertThat(responses).isEmpty();
    }
}
