package com.jobspring.jobspringbackend.util;
import java.security.SecureRandom;

public final class CodeGenerator {
    private static final SecureRandom SR = new SecureRandom();

    private CodeGenerator() {
    }

    public static String numeric6() {
        return String.format("%06d", SR.nextInt(1_000_000));
    }
}

