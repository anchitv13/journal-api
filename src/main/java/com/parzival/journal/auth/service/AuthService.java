package com.parzival.journal.auth.service;

import com.parzival.journal.auth.resource.*;
import com.parzival.journal.entity.Role;
import com.parzival.journal.entity.UserEntity;
import com.parzival.journal.mapper.UserMapper;
import com.parzival.journal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public UserResponse register(RegisterRequest request) {

        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "Email is already registered"
            );
        }

        UserEntity userEntity = UserEntity.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .enabled(true)
                .build();

        UserEntity savedUser = userRepository.save(userEntity);

        log.info("User registered successfully");

        return userMapper.toResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        String email = request.getEmail().trim().toLowerCase();

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException(
                        "Invalid email or password"
                ));

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPasswordHash()
        )) {
            throw new BadCredentialsException(
                    "Invalid email or password"
            );
        }

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new BadCredentialsException(
                    "Invalid email or password"
            );
        }

        String token = jwtService.generateToken(user);

        return LoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpiration() / 1000)
                .build();
    }
}