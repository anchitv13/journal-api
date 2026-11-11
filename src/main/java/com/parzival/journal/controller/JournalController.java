package com.parzival.journal.controller;

import com.parzival.journal.resource.ApiResponse;
import com.parzival.journal.resource.JournalRequest;
import com.parzival.journal.resource.JournalResponse;
import com.parzival.journal.resource.PaginationResponse;
import com.parzival.journal.service.JournalService;
import com.parzival.journal.util.ApiResponseUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/journals")
@Tag(name = "Journal Controller", description = "Controller to use Journal APIs")
@RequiredArgsConstructor
public class JournalController {

    private final JournalService journalService;

    @PostMapping
    public ResponseEntity<ApiResponse<UUID>> createJournal(
            @Valid @RequestBody JournalRequest request,
            Authentication authentication
    ) {
        UUID journalId = journalService.saveJournalEntry(
                request,
                authentication.getName()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseUtil.success(
                        "Journal created successfully",
                        journalId
                ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PaginationResponse<JournalResponse>>> getAllJournals(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction,
            @RequestParam(required = false) String search,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate
    ) {
        return ResponseEntity.ok(
                ApiResponseUtil.success(
                        "Journals fetched successfully",
                        journalService.getAllJournalEntries(
                                authentication.getName(),
                                page,
                                size,
                                sortBy,
                                direction,
                                search,
                                fromDate,
                                toDate
                        )
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JournalResponse>> getJournal(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                ApiResponseUtil.success(
                        "Journal fetched successfully",
                        journalService.getJournalEntry(id, authentication.getName())
                )
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> updateJournal(
            @PathVariable UUID id,
            @Valid @RequestBody JournalRequest request,
            Authentication authentication
    ) {
        journalService.updateJournalEntry(
                id,
                request,
                authentication.getName()
        );

        return ResponseEntity.ok(
                ApiResponseUtil.success("Journal updated successfully", null)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteJournal(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        journalService.deleteJournalEntry(id, authentication.getName());

        return ResponseEntity.ok(
                ApiResponseUtil.success("Journal deleted successfully", null)
        );
    }
}
