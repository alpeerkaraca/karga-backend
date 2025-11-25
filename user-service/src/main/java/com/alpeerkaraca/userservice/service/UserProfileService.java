package com.alpeerkaraca.userservice.service;


import com.alpeerkaraca.userservice.model.UserProfile;
import com.alpeerkaraca.userservice.repository.UserProfileRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserProfileService {
    final UserProfileRepository userProfileRepository;

    public UserProfileService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    public UserProfile getUserByEmail(String email) {
        return userProfileRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found."));
    }

    public UserProfile getUserById(UUID userId) {
        return userProfileRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("User not found."));
    }

    public UserProfile updateProfile(UserProfile userProfile) {
        return userProfileRepository.save(userProfile);
    }
}
