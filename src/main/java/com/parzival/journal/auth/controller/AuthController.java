package com.parzival.journal.auth.controller;

import com.parzival.journal.auth.resource.*;
import com.parzival.journal.auth.service.AuthService;
import com.parzival.journal.auth.service.UserProfileService;
import com.parzival.journal.resource.ApiResponse;
import com.parzival.journal.util.ApiResponseUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserProfileService userProfileService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(
            Authentication authentication
    ) {

        UserResponse response = userProfileService.getProfile(
                authentication.getName()
        );

        return ResponseEntity.ok(
                ApiResponseUtil.success(
                        "Profile retrieved successfully",
                        response
                )
        );
    }
}