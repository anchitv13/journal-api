package com.parzival.journal.service;

import com.parzival.journal.entity.JournalEntity;
import com.parzival.journal.entity.UserEntity;
import com.parzival.journal.exception.ResourceNotFoundException;
import com.parzival.journal.mapper.JournalMapper;
import com.parzival.journal.repository.JournalRepository;
import com.parzival.journal.repository.UserRepository;
import com.parzival.journal.resource.AdminUserResponse;
import com.parzival.journal.resource.JournalResponse;
import com.parzival.journal.resource.PaginationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final JournalRepository journalRepository;
    private final JournalMapper journalMapper;

    @Transactional(readOnly = true)
    public PaginationResponse<AdminUserResponse> getUsers(int page, int size) {
        validatePagination(page, size);

        Page<UserEntity> userPage = userRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        return PaginationResponse.<AdminUserResponse>builder()
                .content(userPage.getContent().stream()
                        .map(this::toUserResponse)
                        .toList())
                .page(userPage.getNumber())
                .size(userPage.getSize())
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .first(userPage.isFirst())
                .last(userPage.isLast())
                .empty(userPage.isEmpty())
                .build();
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getUser(UUID userId) {
        return toUserResponse(findUser(userId));
    }

    @Transactional
    public AdminUserResponse updateUserStatus(
            UUID userId,
            boolean enabled,
            String authenticatedEmail
    ) {
        UserEntity user = findUser(userId);

        if (user.getEmail().equalsIgnoreCase(authenticatedEmail)) {
            throw new IllegalArgumentException(
                    "You cannot change your own account status"
            );
        }

        user.setEnabled(enabled);
        return toUserResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public PaginationResponse<JournalResponse> getUserJournals(
            UUID userId,
            int page,
            int size
    ) {
        validatePagination(page, size);
        UserEntity user = findUser(userId);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<JournalEntity> journalPage =
                journalRepository.findAllByUserIdAndDeletedAtIsNull(user.getId(), pageable);

        return PaginationResponse.<JournalResponse>builder()
                .content(journalPage.getContent().stream()
                        .map(journalMapper::toResponse)
                        .toList())
                .page(journalPage.getNumber())
                .size(journalPage.getSize())
                .totalElements(journalPage.getTotalElements())
                .totalPages(journalPage.getTotalPages())
                .first(journalPage.isFirst())
                .last(journalPage.isLast())
                .empty(journalPage.isEmpty())
                .build();
    }

    private UserEntity findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private AdminUserResponse toUserResponse(UserEntity user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .enabled(Boolean.TRUE.equals(user.getEnabled()))
                .createdAt(user.getCreatedAt())
                .build();
    }

    private void validatePagination(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Invalid pagination parameters. Page must be >= 0 and size must be between 1 and 100"
            );
        }
    }
}
