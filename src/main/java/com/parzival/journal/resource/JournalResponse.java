package com.parzival.journal.resource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JournalResponse {

    private UUID id;

    private String title;

    private String content;

    private Instant createdAt;

    private Instant updatedAt;
}