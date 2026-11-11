package com.parzival.journal.auth.service;

import com.parzival.journal.auth.resource.UserResponse;
import com.parzival.journal.entity.UserEntity;
import com.parzival.journal.exception.ResourceNotFoundException;
import com.parzival.journal.mapper.UserMapper;
import com.parzival.journal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserResponse getProfile(String email) {
        UserEntity user = getUser(email);
        return userMapper.toResponse(user);
    }

    @Transactional
    public void changePassword(
            String email,
            String currentPassword,
            String newPassword
    ) {
        UserEntity user = getUser(email);

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException(
                    "New password must be different from the current password"
            );
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private UserEntity getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
