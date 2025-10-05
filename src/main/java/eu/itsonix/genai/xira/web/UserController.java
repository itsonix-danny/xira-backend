package eu.itsonix.genai.xira.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import eu.itsonix.genai.xira.service.UserService;
import eu.itsonix.genai.xira.web.api.UsersApi;
import eu.itsonix.genai.xira.web.model.UserResponse;

@RestController
@RequiredArgsConstructor
@Validated
public class UserController implements UsersApi {

    private final UserService userService;

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getUsers(final String email) {
        final UserResponse userResponse = userService.findByEmail(email);
        return ResponseEntity.ok(userResponse);
    }
}
