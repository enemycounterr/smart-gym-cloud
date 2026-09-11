package com.sprint.auth.controller;

import com.sprint.auth.dto.request.UpdateRoleRequest;
import com.sprint.auth.model.User;
import com.sprint.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class UserAdminController {
    private final AuthService authService;

    public UserAdminController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/users/{userId}/revoke")
    public ResponseEntity<Void> revokeUserTokens(@PathVariable Long userId) {
        this.authService.revokeAllUserTokens(userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/users/{userId}/role")
    public ResponseEntity<Void> updateUserRole(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateRoleRequest request
    ) {
        this.authService.updateRole(userId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        this.authService.deleteUser(id, currentUser);
    }

}
