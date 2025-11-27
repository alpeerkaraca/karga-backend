package com.alpeerkaraca.userservice.service;


import com.alpeerkaraca.common.exception.ResourceNotFoundException;
import com.alpeerkaraca.userservice.dto.UpdateUserProfileDto;
import com.alpeerkaraca.userservice.model.UserProfile;
import com.alpeerkaraca.userservice.repository.UserProfileRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserProfileService {
    final UserProfileRepository userProfileRepository;

    public UserProfileService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    public UserProfile getUserByEmail(String email) {
        return userProfileRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    public UserProfile getUserById(UUID userId) {
        return userProfileRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

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

    public UserProfile updateProfile(UserProfile userProfile) {
        return userProfileRepository.save(userProfile);
    }
}
