package eu.itsonix.genai.xira.service;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import eu.itsonix.genai.xira.jpa.entity.XiraUser;
import eu.itsonix.genai.xira.jpa.repository.XiraUserRepository;
import eu.itsonix.genai.xira.web.model.LoginRequest;
import eu.itsonix.genai.xira.web.model.RegisterRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtEncoder jwtEncoder;

    @Mock
    private XiraUserRepository xiraUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void givenValidRegisterRequest_thenCreatesNewUser() {
        final RegisterRequest registerRequest = new RegisterRequest().email("test@example.com")
                .password("password123")
                .firstName("John")
                .lastName("Doe");

        when(xiraUserRepository.existsByEmailIgnoreCase("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(xiraUserRepository.save(any(XiraUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.register(registerRequest);

        final ArgumentCaptor<XiraUser> userCaptor = ArgumentCaptor.forClass(XiraUser.class);
        verify(xiraUserRepository).save(userCaptor.capture());

        final XiraUser savedUser = userCaptor.getValue();
        assertEquals("test@example.com", savedUser.getEmail());
        assertEquals("John", savedUser.getFirstName());
        assertEquals("Doe", savedUser.getLastName());
        assertEquals("encodedPassword", savedUser.getPasswordHash());
        verify(xiraUserRepository).existsByEmailIgnoreCase("test@example.com");
    }

    @Test
    void givenExistingEmail_thenThrowsIllegalStateException() {
        final RegisterRequest registerRequest = new RegisterRequest().email("existing@example.com")
                .password("password123")
                .firstName("Jane")
                .lastName("Smith");

        when(xiraUserRepository.existsByEmailIgnoreCase("existing@example.com")).thenReturn(true);

        final IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> authService.register(registerRequest));

        assertEquals("User with email existing@example.com already exists", exception.getMessage());
        verify(xiraUserRepository).existsByEmailIgnoreCase("existing@example.com");
        verify(xiraUserRepository, never()).save(any(XiraUser.class));
    }

    @Test
    void givenLoginRequest_thenJwtIncludesCorrectClaims() {
        final LoginRequest loginRequest = new LoginRequest().email("user@example.com").password("password");

        final Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("user@example.com");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        final Jwt jwt = mock(Jwt.class);
        when(jwt.getTokenValue()).thenReturn("token");
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt);

        authService.login(loginRequest);

        final ArgumentCaptor<JwtEncoderParameters> paramsCaptor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(paramsCaptor.capture());

        final JwtEncoderParameters params = paramsCaptor.getValue();
        assertNotNull(params.getJwsHeader());
        assertNotNull(params.getClaims());
        assertEquals("xira", params.getClaims().getClaim("iss"));
        assertEquals("user@example.com", params.getClaims().getSubject());
        assertNotNull(params.getClaims().getIssuedAt());
        assertNotNull(params.getClaims().getExpiresAt());

        final Instant issuedAt = params.getClaims().getIssuedAt();
        final Instant expiresAt = params.getClaims().getExpiresAt();
        final long expectedExpiry = 7200;
        assertEquals(expectedExpiry, expiresAt.getEpochSecond() - issuedAt.getEpochSecond());
    }
}
