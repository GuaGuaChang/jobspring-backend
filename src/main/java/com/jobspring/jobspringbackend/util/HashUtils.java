package com.jobspring.jobspringbackend.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class HashUtils {
    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();

    private HashUtils() {
    }

    public static String hash(String plain) {
        return BCRYPT.encode(plain);
    }

    public static boolean matches(String plain, String hash) {
        return BCRYPT.matches(plain, hash);
    }
}

