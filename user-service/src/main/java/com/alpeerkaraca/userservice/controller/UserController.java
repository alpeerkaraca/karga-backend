package com.alpeerkaraca.userservice.controller;

import com.alpeerkaraca.common.dto.ApiResponse;
import com.alpeerkaraca.common.exception.EmptyBodyException;
import com.alpeerkaraca.userservice.dto.UpdateUserProfileDto;
import com.alpeerkaraca.userservice.model.UserProfile;
import com.alpeerkaraca.userservice.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
    @Validated
    public ResponseEntity<ApiResponse<UserProfile>> updateMe(@RequestBody @Valid UpdateUserProfileDto userProfile) {
        if (userProfile.isAllFieldsNull()) {
            throw new EmptyBodyException("At least one field must be provided.");
        }

        String userIdString = SecurityContextHolder.getContext().getAuthentication().getName();
        UUID userId = UUID.fromString(userIdString);

        UserProfile updatedUser = userProfileService.updateProfile(userProfile, userId);

        return ResponseEntity.ok(ApiResponse.success(updatedUser, "User profile updated successfully."));
    }
}
