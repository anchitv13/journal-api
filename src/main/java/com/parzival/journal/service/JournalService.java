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

import java.time.Instant;
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
    public PaginationResponse<JournalResponse> getAllJournalEntries(
            String email,
            int page,
            int size,
            String sortBy,
            String direction
    ) {
        return getAllJournalEntries(
                email, page, size, sortBy, direction, null, null, null
        );
    }

    @Transactional(readOnly = true)
    public PaginationResponse<JournalResponse> getAllJournalEntries(
            String email,
            int page,
            int size,
            String sortBy,
            String direction,
            String search,
            Instant fromDate,
            Instant toDate
    ) {
        if (!allowedSortFields.contains(sortBy)) {
            throw new IllegalArgumentException("Invalid sort field");
        }

        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Invalid pagination parameters");
        }

        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("fromDate must be earlier than or equal to toDate");
        }

        if (search != null && search.length() > 200) {
            throw new IllegalArgumentException("Search text cannot exceed 200 characters");
        }

        String normalizedSearch = search == null || search.isBlank()
                ? null
                : search.trim();

        UserEntity user = getUser(email);

        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Sort sort = Sort.by(sortDirection, sortBy)
                .and(Sort.by(Sort.Direction.DESC, "id"));

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<JournalEntity> journalPage = journalRepository.searchActiveByUserId(
                user.getId(),
                normalizedSearch,
                fromDate,
                toDate,
                pageable
        );

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

        entity.setDeletedAt(Instant.now());
        entity.setDeletedBy(user.getId());
        journalRepository.save(entity);

        log.info("Journal entry moved to trash successfully");
    }

    @Transactional(readOnly = true)
    public PaginationResponse<JournalResponse> getTrash(
            String email,
            int page,
            int size
    ) {
        validatePageSize(page, size);
        UserEntity user = getUser(email);
        Page<JournalEntity> journalPage = journalRepository
                .findAllByUserIdAndDeletedAtIsNotNull(
                        user.getId(),
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "deletedAt"))
                );

        return toPaginationResponse(journalPage);
    }

    @Transactional
    public void restoreJournalEntry(UUID journalId, String email) {
        UserEntity user = getUser(email);
        JournalEntity entity = journalRepository
                .findByIdAndUserIdAndDeletedAtIsNotNull(journalId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Deleted journal entry not found"));

        entity.setDeletedAt(null);
        entity.setDeletedBy(null);
        entity.setUpdatedAt(Instant.now());
        journalRepository.save(entity);
    }

    @Transactional
    public void permanentlyDeleteJournalEntry(UUID journalId, String email) {
        UserEntity user = getUser(email);
        JournalEntity entity = journalRepository
                .findByIdAndUserIdAndDeletedAtIsNotNull(journalId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Deleted journal entry not found"));

        journalRepository.delete(entity);
    }

    private void validatePageSize(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Invalid pagination parameters");
        }
    }

    private PaginationResponse<JournalResponse> toPaginationResponse(Page<JournalEntity> page) {
        return PaginationResponse.<JournalResponse>builder()
                .content(page.getContent().stream().map(journalMapper::toResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .empty(page.isEmpty())
                .build();
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

        return journalRepository.findByIdAndUserIdAndDeletedAtIsNull(
                journalId,
                userId
        ).orElseThrow(() ->
                new ResourceNotFoundException("Journal entry not found")
        );
    }
}