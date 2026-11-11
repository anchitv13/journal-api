package com.parzival.journal.auth.service;

import com.parzival.journal.auth.resource.UserResponse;
import com.parzival.journal.entity.UserEntity;
import com.parzival.journal.mapper.UserMapper;
import com.parzival.journal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public UserResponse getProfile(String email) {

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );

        return userMapper.toResponse(user);
    }
}