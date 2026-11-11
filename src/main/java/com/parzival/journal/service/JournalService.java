package com.parzival.journal.service;

import com.parzival.journal.entity.JournalEntity;
import com.parzival.journal.entity.UserEntity;
import com.parzival.journal.exception.ResourceNotFoundException;
import com.parzival.journal.mapper.JournalMapper;
import com.parzival.journal.repository.JournalRepository;
import com.parzival.journal.repository.UserRepository;
import com.parzival.journal.resource.JournalRequest;
import com.parzival.journal.resource.JournalResponse;
import com.parzival.journal.resource.PaginationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class JournalService {

    private final JournalRepository journalRepository;
    private final UserRepository userRepository;
    private final JournalMapper journalMapper;

    private final List<String> allowedSortFields = List.of(
            "createdAt",
            "updatedAt",
            "title"
    );

    @Transactional
    public UUID saveJournalEntry(
            JournalRequest request,
            String email
    ) {

        UserEntity user = getUser(email);

        JournalEntity entity = journalMapper.toEntity(request, user);

        JournalEntity savedEntity = journalRepository.save(entity);

        log.info("Journal entry created successfully");

        return savedEntity.getId();
    }

    @Transactional(readOnly = true)
    public PaginationResponse<JournalResponse> getAllJournalEntries(String email, int page, int size, String sortBy, String direction) {

        if (!allowedSortFields.contains(sortBy)) {
            throw new IllegalArgumentException("Invalid sort field");
        }

        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Invalid pagination parameters");
        }

        UserEntity user = getUser(email);

        Sort.Direction sortDirection = Sort.Direction.fromString(direction);

        Sort sort = Sort.by(sortDirection, sortBy).and(Sort.by(Sort.Direction.DESC, "id"));

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<JournalEntity> journalPage = journalRepository.findAllByUserId(user.getId(), pageable);

        return PaginationResponse.<JournalResponse>builder()
                .content(journalPage.getContent()
                        .stream()
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

    @Transactional(readOnly = true)
    public JournalResponse getJournalEntry(
            UUID journalId,
            String email
    ) {

        UserEntity user = getUser(email);

        JournalEntity entity = getJournalEntryForUser(
                journalId,
                user.getId()
        );

        return journalMapper.toResponse(entity);
    }

    @Transactional
    public void updateJournalEntry(
            UUID journalId,
            JournalRequest request,
            String email
    ) {

        UserEntity user = getUser(email);

        JournalEntity entity = getJournalEntryForUser(
                journalId,
                user.getId()
        );

        journalMapper.updateEntity(request, entity);

        journalRepository.save(entity);

        log.info("Journal entry updated successfully");
    }

    @Transactional
    public void deleteJournalEntry(
            UUID journalId,
            String email
    ) {

        UserEntity user = getUser(email);

        JournalEntity entity = getJournalEntryForUser(
                journalId,
                user.getId()
        );

        journalRepository.delete(entity);

        log.info("Journal entry deleted successfully");
    }

    private UserEntity getUser(String email) {

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );
    }

    private JournalEntity getJournalEntryForUser(
            UUID journalId,
            UUID userId
    ) {

        return journalRepository.findByIdAndUserId(
                journalId,
                userId
        ).orElseThrow(() ->
                new ResourceNotFoundException("Journal entry not found")
        );
    }
}