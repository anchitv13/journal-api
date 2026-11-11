package com.parzival.journal.repository;

import com.parzival.journal.entity.JournalEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JournalRepository
        extends JpaRepository<JournalEntity, UUID> {

    Page<JournalEntity> findAllByUserId(UUID userId, Pageable pageable);

    Optional<JournalEntity> findByIdAndUserId(UUID journalId, UUID userId);
}