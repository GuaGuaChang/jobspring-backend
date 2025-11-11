package com.jobspring.jobspringbackend.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/me")
public class MeController {
    //  test jwt if it is working
    @GetMapping
    public Map<String, Object> me(Authentication auth) {
        return Map.of("userId", auth.getName(), "authorities", auth.getAuthorities());
    }
}
