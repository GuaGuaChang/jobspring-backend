package com.jobspring.jobspringbackend.controller;

import com.jobspring.jobspringbackend.dto.LoginRequestDTO;
import com.jobspring.jobspringbackend.dto.RegisterRequestDTO;
import com.jobspring.jobspringbackend.dto.AuthResponseDTO;
import com.jobspring.jobspringbackend.entity.User;
import com.jobspring.jobspringbackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;


import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // register
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@RequestBody RegisterRequestDTO request) {
        // simple check
        if (request.getEmail() == null || request.getPassword() == null || request.getFullName() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword())); // hash store
        user.setFullName(request.getFullName());
        user.setRole(0); // default Candidate
        user.setIsActive(true);

        userRepository.save(user);

        AuthResponseDTO response = buildAuthResponse(user, "dummy-jwt-token");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // login
    @PostMapping("/login")
    public AuthResponseDTO login(@RequestBody LoginRequestDTO request) {
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isEmpty()) {
            throw new RuntimeException("Invalid email or password");
        }

        User user = userOpt.get();
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid email or password");
        }

        return buildAuthResponse(user, "dummy-jwt-token");
    }

    private AuthResponseDTO buildAuthResponse(User user, String token) {
        AuthResponseDTO response = new AuthResponseDTO();
        response.setToken(token);

        AuthResponseDTO.UserDto dto = new AuthResponseDTO.UserDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        response.setUser(dto);

        return response;
    }
}
