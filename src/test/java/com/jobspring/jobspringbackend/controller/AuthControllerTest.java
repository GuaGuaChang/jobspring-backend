package com.jobspring.jobspringbackend.controller;

import com.jobspring.jobspringbackend.dto.AuthResponseDTO;
import com.jobspring.jobspringbackend.entity.User;
import com.jobspring.jobspringbackend.repository.UserRepository;
import com.jobspring.jobspringbackend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void register_created() throws Exception {
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Passw0rd!")).thenReturn("HASH");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0, User.class);
            u.setId(1L);
            return u;
        });
        when(jwtService.generateToken(1L, "a@b.com", 0)).thenReturn("jwt-token");

        String reqJson = """
                {"email":"a@b.com","password":"Passw0rd!","fullName":"Alice"}
                """;

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.user.id").value(1))
                .andExpect(jsonPath("$.user.email").value("a@b.com"))
                .andExpect(jsonPath("$.user.fullName").value("Alice"))
                .andExpect(jsonPath("$.user.role").value(0));
    }

    @Test
    void register_conflict_when_email_exists() throws Exception {
        when(userRepository.findByEmail("dup@x.com")).thenReturn(Optional.of(new User()));

        String reqJson = """
                {"email":"dup@x.com","password":"x","fullName":"Dup"}
                """;

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqJson))
                .andExpect(status().isConflict());
    }

    @Test
    void register_bad_request_when_missing_fullName() throws Exception {
        String reqJson = """
                {"email":"a@b.com","password":"Passw0rd!"}
                """;

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_ok() throws Exception {
        User u = new User();
        u.setId(42L);
        u.setEmail("a@b.com");
        u.setPasswordHash("HASH");
        u.setFullName("Alice");
        u.setRole(0);

        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("Passw0rd!", "HASH")).thenReturn(true);
        when(jwtService.generateToken(42L, "a@b.com", 0)).thenReturn("jwt-42");

        String reqJson = """
                {"email":"a@b.com","password":"Passw0rd!"}
                """;

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-42"))
                .andExpect(jsonPath("$.user.id").value(42))
                .andExpect(jsonPath("$.user.email").value("a@b.com"))
                .andExpect(jsonPath("$.user.fullName").value("Alice"));
    }
}
