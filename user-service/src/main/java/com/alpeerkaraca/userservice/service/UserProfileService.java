package com.alpeerkaraca.userservice.service;


import com.alpeerkaraca.common.exception.ResourceNotFoundException;
import com.alpeerkaraca.userservice.dto.UpdateUserProfileDto;
import com.alpeerkaraca.userservice.model.UserProfile;
import com.alpeerkaraca.userservice.repository.UserProfileRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service managing user profiles (CRUD operations).
 */
@Service
public class UserProfileService {
    final UserProfileRepository userProfileRepository;

    public UserProfileService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    /**
     * Retrieves the profile of the user with the given email.
     *
     * @param email User Email.
     * @return The user profile.
     * @throws ResourceNotFoundException If the user is not found.
     */
    public UserProfile getUserByEmail(String email) {
        return userProfileRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    /**
     * Retrieves the profile of the user with the given ID.
     *
     * @param userId User UUID.
     * @return The user profile.
     * @throws ResourceNotFoundException If the user is not found.
     */
    public UserProfile getUserById(UUID userId) {
        return userProfileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found: " + userId));
    }

    /**
     * Updates user profile information.
     * Only non-null fields are updated (Partial Update).
     *
     * @param userProfile Data to update (name, phone, etc.).
     * @param userId      ID of the user to update.
     * @return Updated profile entity.
     */
    @Transactional
    public UserProfile updateProfile(UpdateUserProfileDto userProfile, UUID userId) {
        UserProfile existingProfile = getUserById(userId);

        if (userProfile.getFirstName() != null) {
            existingProfile.setFirstName(userProfile.getFirstName());
        }

        if (userProfile.getLastName() != null) {
            existingProfile.setLastName(userProfile.getLastName());
        }
        if (userProfile.getEmail() != null) {
            existingProfile.setEmail(userProfile.getEmail());
        }

        if (userProfile.getPhoneNumber() != null) {
            existingProfile.setPhoneNumber(userProfile.getPhoneNumber());
        }
        if (userProfile.getRating() != null) {
            existingProfile.setRating(userProfile.getRating());
        }

        return updateProfile(existingProfile);
    }

    @Transactional
    public UserProfile updateProfile(UserProfile userProfile) {
        return userProfileRepository.save(userProfile);
    }
}
