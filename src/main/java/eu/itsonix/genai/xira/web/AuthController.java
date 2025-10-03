package eu.itsonix.genai.xira.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import eu.itsonix.genai.xira.service.AuthService;
import eu.itsonix.genai.xira.web.api.AuthApi;
import eu.itsonix.genai.xira.web.model.LoginRequest;
import eu.itsonix.genai.xira.web.model.RegisterRequest;
import eu.itsonix.genai.xira.web.model.TokenResponse;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AuthController implements AuthApi {

    private final AuthService authService;

    @Override
    public ResponseEntity<TokenResponse> login(final LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @Override
    public ResponseEntity<Void> register(final RegisterRequest registerRequest) {
        authService.register(registerRequest);
        return ResponseEntity.status(201).build();
    }
}
