package com.parzival.journal.repository;

import com.parzival.journal.entity.JournalEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface JournalRepository
        extends JpaRepository<JournalEntity, UUID> {

    Page<JournalEntity> findAllByUserId(UUID userId, Pageable pageable);

    @Query("""
            SELECT j
            FROM JournalEntity j
            WHERE j.user.id = :userId
              AND (
                    :search IS NULL
                    OR LOWER(j.title) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(j.content) LIKE LOWER(CONCAT('%', :search, '%'))
              )
              AND (:fromDate IS NULL OR j.createdAt >= :fromDate)
              AND (:toDate IS NULL OR j.createdAt <= :toDate)
            """)
    Page<JournalEntity> searchByUserId(
            @Param("userId") UUID userId,
            @Param("search") String search,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            Pageable pageable
    );

    Optional<JournalEntity> findByIdAndUserId(UUID journalId, UUID userId);
}
