package com.exammaster.exammaster_pro.service;

import com.exammaster.exammaster_pro.dto.Requests.*;
import com.exammaster.exammaster_pro.dto.Responses.*;
import com.exammaster.exammaster_pro.entity.*;
import com.exammaster.exammaster_pro.exception.BusinessValidationException;
import com.exammaster.exammaster_pro.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final AuditService auditService;

    @Value("${app.jwt.issuer}")
    private String issuer;
    @Value("${app.jwt.access-token-minutes}")
    private long minutes;

    public AuthResponse register(RegisterRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new BusinessValidationException("Passwords do not match.");
        }
        if (users.existsByEmail(request.email())) {
            throw new BusinessValidationException("Email already registered.");
        }
        if (users.existsByUsername(request.username())) {
            throw new BusinessValidationException("Username already exists.");
        }
        AppUser user = new AppUser();
        user.setCollegeName(request.collegeName());
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.ROLE_USER);
        users.save(user);
        auditService.log(user, "User Registered", "Authentication", "Registration successful.", user.getUsername());
        return new AuthResponse(token(user), user.getUsername(), user.getRole().name(), "Registration successful.");
    }

    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        AppUser user = users.findByUsername(auth.getName()).orElseThrow();
        auditService.log(user, "Login", "Authentication", "Login successful.", user.getUsername());
        return new AuthResponse(token(user), user.getUsername(), user.getRole().name(), "Login successful.");
    }

    public AppUser createUser(UserRequest request) {
        if (users.existsByEmail(request.email())) throw new BusinessValidationException("Email already registered.");
        if (users.existsByUsername(request.username())) throw new BusinessValidationException("Username already exists.");
        AppUser user = new AppUser();
        user.setCollegeName(request.collegeName());
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.ROLE_USER);
        return users.save(user);
    }

    public String token(AppUser user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plus(minutes, ChronoUnit.MINUTES))
                .subject(user.getUsername())
                .claim("scope", user.getRole().name())
                .claim("userId", user.getId())
                .claim("collegeName", user.getCollegeName())
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
