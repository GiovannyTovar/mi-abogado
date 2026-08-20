package com.miabogado.domain.auth.controller;

import com.miabogado.domain.auth.dto.AuthResponse;
import com.miabogado.domain.auth.dto.GoogleLoginRequest;
import com.miabogado.domain.auth.dto.RefreshRequest;
import com.miabogado.domain.auth.service.AuthService;
import com.miabogado.domain.user.dto.UserResponse;
import com.miabogado.domain.user.mapper.UserMapper;
import com.miabogado.domain.user.repository.UserRepository;
import com.miabogado.shared.error.BusinessException;
import com.miabogado.shared.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @PostMapping("/google")
    public AuthResponse loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request,
                                        @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {
        return authService.loginWithGoogle(request.idToken(), userAgent);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request,
                                @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {
        return authService.refresh(request.refreshToken(), userAgent);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    /** Sesion actual: el cliente Ionic la usa al arrancar para pintar el perfil. */
    @GetMapping("/me")
    public UserResponse me() {
        return userRepository.findById(CurrentUser.require().userId())
                .map(userMapper::toResponse)
                .orElseThrow(() -> BusinessException.notFound("Usuario"));
    }
}
