package com.parzival.journal.controller;

import com.parzival.journal.resource.AdminUserResponse;
import com.parzival.journal.resource.ApiResponse;
import com.parzival.journal.resource.JournalResponse;
import com.parzival.journal.resource.PaginationResponse;
import com.parzival.journal.resource.UpdateUserStatusRequest;
import com.parzival.journal.service.AdminService;
import com.parzival.journal.util.ApiResponseUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin Controller", description = "Controller to use admin APIs")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<PaginationResponse<AdminUserResponse>>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponseUtil.success(
                "Users fetched successfully",
                adminService.getUsers(page, size)
        ));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<AdminUserResponse>> getUser(
            @PathVariable UUID userId
    ) {
        return ResponseEntity.ok(ApiResponseUtil.success(
                "User fetched successfully",
                adminService.getUser(userId)
        ));
    }

    @PatchMapping("/users/{userId}/status")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateUserStatus(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserStatusRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponseUtil.success(
                "User status updated successfully",
                adminService.updateUserStatus(
                        userId,
                        request.getEnabled(),
                        authentication.getName()
                )
        ));
    }

    @GetMapping("/users/{userId}/journals")
    public ResponseEntity<ApiResponse<PaginationResponse<JournalResponse>>> getUserJournals(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponseUtil.success(
                "User journals fetched successfully",
                adminService.getUserJournals(userId, page, size)
        ));
    }
}
