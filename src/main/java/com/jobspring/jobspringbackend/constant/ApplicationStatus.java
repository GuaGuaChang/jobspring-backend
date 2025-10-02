package com.jobspring.jobspringbackend.constant;

public class ApplicationStatus {
    public static final int SUBMIT = 0;    // 有效/激活
    public static final int FILTER = 1;  // 无效/禁用
    public static final int PASS = 2;
    public static final int REJECT = 3;
    public static final int INVALID = 4;

    private ApplicationStatus() {
    }

    public static String getStatusName(int status) {
        return switch (status) {
            case SUBMIT -> "submitted";
            case FILTER -> "filtering";
            case PASS -> "passed";
            case REJECT -> "rejected";
            case INVALID -> "invalid";
            default -> "";
        };
    }
}
