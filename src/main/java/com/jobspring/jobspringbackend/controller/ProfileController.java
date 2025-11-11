package com.jobspring.jobspringbackend.controller;


import com.jobspring.jobspringbackend.dto.ProfileRequestDTO;
import com.jobspring.jobspringbackend.dto.ProfileResponseDTO;
import com.jobspring.jobspringbackend.dto.ProfileUpdateResponseDTO;
import com.jobspring.jobspringbackend.repository.UserRepository;
import com.jobspring.jobspringbackend.service.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @Autowired
    private ProfileService profileService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public ProfileResponseDTO getMyProfile(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }

        try {
            String userIdStr = authentication.getName();
            Long userId = Long.parseLong(userIdStr);

            return profileService.getCompleteProfile(userId);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid user ID format in token");
        }
    }

    @PostMapping
    public ProfileUpdateResponseDTO createOrUpdateProfile(@AuthenticationPrincipal String userIdStr, @RequestBody ProfileRequestDTO request) {

        if (userIdStr == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }

        try {
            return profileService.createOrUpdateProfile(Long.valueOf(userIdStr), request);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update profile: " + e.getMessage());
        }
    }
}
