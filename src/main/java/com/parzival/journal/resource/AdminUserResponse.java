package com.parzival.journal.resource;

import com.parzival.journal.entity.Role;
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
public class AdminUserResponse {

    private UUID id;
    private String email;
    private Role role;
    private boolean enabled;
    private Instant createdAt;
}
