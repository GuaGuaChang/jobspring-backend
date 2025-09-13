package com.jobspring.jobspringbackend.constant;

public class Role {
    public static final int CANDIDATE = 0;  // 候选人
    public static final int HR = 1;         // HR
    public static final int ADMIN = 2;      // Admin

    private Role() {
    } // 防止实例化

    public static String getRoleName(int role) {
        return switch (role) {
            case CANDIDATE -> "candidate";
            case HR -> "HR";
            case ADMIN -> "admin";
            default -> "";
        };
    }
}