package com.tienda.controller;

import com.tienda.dto.*;
import com.tienda.entity.User;
import com.tienda.security.JwtUtil;
import com.tienda.service.AuditService;
import com.tienda.service.RegistrationService;
import com.tienda.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final RegistrationService registrationService;
    private final AuditService auditService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil,
                          UserService userService,
                          RegistrationService registrationService,
                          AuditService auditService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
        this.registrationService = registrationService;
        this.auditService = auditService;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = userService.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        try {
            auditService.log(user.getEmail(), "USER_LOGIN", "USER", user.getId(), null, null);
        } catch (Exception e) {
            log.error("Error al auditar USER_LOGIN: {}", e.getMessage());
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        LoginResponse response = LoginResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .role(user.getRole())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .userId(user.getId())
                .build();

        return ResponseEntity.ok(ApiResponse.ok("Login exitoso", response));
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(@RequestHeader("Authorization") String bearerToken) {
        String token = bearerToken.replace("Bearer ", "");

        if (!jwtUtil.validateToken(token) || !jwtUtil.isRefreshToken(token)) {
            return ResponseEntity.status(401).body(ApiResponse.error("Token invalido"));
        }

        String email = jwtUtil.getEmailFromToken(token);
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String newToken = jwtUtil.generateToken(email, user.getRole());
        String newRefreshToken = jwtUtil.generateRefreshToken(email);

        LoginResponse response = LoginResponse.builder()
                .token(newToken)
                .refreshToken(newRefreshToken)
                .role(user.getRole())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .userId(user.getId())
                .build();

        return ResponseEntity.ok(ApiResponse.ok("Token renovado", response));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        registrationService.submitRegistration(request);
        return ResponseEntity.ok(ApiResponse.ok("Solicitud de registro enviada. Espera la aprobacion del administrador.", null));
    }
}
