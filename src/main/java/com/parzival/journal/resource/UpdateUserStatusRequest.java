package com.parzival.journal.resource;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserStatusRequest {

    @NotNull(message = "enabled is required")
    private Boolean enabled;
}
