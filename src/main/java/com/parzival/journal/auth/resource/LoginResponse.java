package com.parzival.journal.auth.resource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {

    private String accessToken;

    @Builder.Default
    private String tokenType = "Bearer";

    private long expiresIn;
}