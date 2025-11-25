package com.alpeerkaraca.userservice.controller;

import com.alpeerkaraca.common.dto.ApiResponse;
import com.alpeerkaraca.userservice.model.UserProfile;
import com.alpeerkaraca.userservice.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserProfileService userProfileService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfile>> getMe() {
        String userIdString = SecurityContextHolder.getContext().getAuthentication().getName();
        UUID userId = UUID.fromString(userIdString);

        UserProfile userProfile = userProfileService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(userProfile, "User profile retrieved successfully."));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserProfile>> updateMe(@RequestBody UserProfile userProfile) {
        String userIdString = SecurityContextHolder.getContext().getAuthentication().getName();
        UUID userId = UUID.fromString(userIdString);
        if (Objects.equals(userId, userProfile.getUserId())) {
            throw new IllegalArgumentException("Credentials could not be verified.");
        }

        UserProfile updatedUser = userProfileService.updateProfile(userProfile);

        return ResponseEntity.ok(ApiResponse.success(updatedUser, "User profile updated successfully."));
    }
}
