package com.jobspring.jobspringbackend.constant;

public class ProfileInvisibility {
    public static final int PRIVATE = 0;  // 候选人
    public static final int COMPANY = 1;         // HR
    public static final int PUBLIC = 2;      // Admin

    private ProfileInvisibility() {
    } // 防止实例化

    public static String getVisibility(int visibility) {
        return switch (visibility) {
            case PRIVATE -> "private";
            case COMPANY -> "public for company";
            case PUBLIC -> "public";
            default -> "";
        };
    }
}
