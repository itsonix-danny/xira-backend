package eu.itsonix.genai.xira.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import eu.itsonix.genai.xira.jpa.entity.IssueComment;
import eu.itsonix.genai.xira.jpa.entity.ProjectRole;
import eu.itsonix.genai.xira.jpa.entity.XiraUser;
import eu.itsonix.genai.xira.mapper.ProjectMapper;
import eu.itsonix.genai.xira.mapper.UserMapper;
import eu.itsonix.genai.xira.jpa.repository.IssueCommentRepository;
import eu.itsonix.genai.xira.jpa.repository.ProjectMemberRepository;
import eu.itsonix.genai.xira.jpa.repository.XiraUserRepository;
import eu.itsonix.genai.xira.web.model.LoginRequest;
import eu.itsonix.genai.xira.web.model.ProjectMembershipResponse;
import eu.itsonix.genai.xira.web.model.RegisterRequest;
import eu.itsonix.genai.xira.web.model.TokenResponse;

@Service
@RequiredArgsConstructor
public class AuthService {

    public static final long TOKEN_EXPIRY_TIME_IN_SECONDS = 7200;
    public static final String ISSUER = "xira";

    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final XiraUserRepository xiraUserRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final IssueCommentRepository issueCommentRepository;
    private final PasswordEncoder passwordEncoder;

    public TokenResponse login(final LoginRequest loginRequest) {
        final Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        final Instant now = Instant.now();

        final JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(TOKEN_EXPIRY_TIME_IN_SECONDS))
                .subject(auth.getName())
                .build();

        final JwsHeader jwsHeader = JwsHeader.with(SignatureAlgorithm.RS256).build();
        final String token = jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();

        final XiraUser user = xiraUserRepository.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        final List<ProjectMembershipResponse> projectMemberships = projectMemberRepository.findAllByUserId(user.getId())
                .stream()
                .map(ProjectMapper::toProjectMembershipResponse)
                .toList();

        return new TokenResponse().accessToken(token)
                .user(UserMapper.toUserResponse(user))
                .projectMemberships(projectMemberships);
    }

    public void register(final RegisterRequest registerRequest) {
        if (xiraUserRepository.existsByEmailIgnoreCase(registerRequest.getEmail())) {
            throw new IllegalStateException(
                    String.format("User with email %s already exists", registerRequest.getEmail()));
        }

        xiraUserRepository.save(XiraUser.builder()
                .email(registerRequest.getEmail())
                .firstName(registerRequest.getFirstName())
                .lastName(registerRequest.getLastName())
                .passwordHash(passwordEncoder.encode(registerRequest.getPassword()))
                .build());
    }

    public XiraUser getAuthenticatedUser() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::getName)
                .flatMap(xiraUserRepository::findByEmailIgnoreCase)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }

    public boolean isProjectAdmin(final String projectKey) {
        final String email = Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::getName)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        return projectMemberRepository.existsByProject_KeyIgnoreCaseAndXiraUser_EmailIgnoreCaseAndRole(projectKey,
                email, ProjectRole.ADMIN);
    }

    public boolean isProjectMember(final String projectKey) {
        final String email = Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::getName)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        return projectMemberRepository.existsByProject_KeyIgnoreCaseAndXiraUser_EmailIgnoreCase(projectKey, email);
    }

    public boolean isCommentAuthor(final String commentId) {
        final String authenticatedUserId = getAuthenticatedUser().getId();
        return issueCommentRepository.findById(commentId)
                .map(IssueComment::getAuthor)
                .map(XiraUser::getId)
                .map(authorId -> authorId.equals(authenticatedUserId))
                .orElse(false);
    }
}
