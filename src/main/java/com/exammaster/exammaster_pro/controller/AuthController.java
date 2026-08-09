package com.exammaster.exammaster_pro.controller;

import com.exammaster.exammaster_pro.dto.ApiResponse;
import com.exammaster.exammaster_pro.dto.Requests.*;
import com.exammaster.exammaster_pro.dto.Responses.AuthResponse;
import com.exammaster.exammaster_pro.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService auth;

    @PostMapping("/register")
    ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = auth.register(request);
        return ApiResponse.ok(response.message(), response);
    }

    @PostMapping("/login")
    ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = auth.login(request);
        return ApiResponse.ok(response.message(), response);
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    ApiResponse<AuthResponse> loginForm(@RequestParam String username, @RequestParam String password) {
        AuthResponse response = auth.login(new LoginRequest(username, password));
        return ApiResponse.ok(response.message(), response);
    }
}
