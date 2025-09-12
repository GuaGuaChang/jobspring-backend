package com.jobspring.jobspringbackend.constant;

public class ReviewStatus {
    public static final int PEND = 0;    // 有效/激活// 已删除
    public static final int PASS = 1;
    public static final int WITHDREW = 2;

    private ReviewStatus(){}

    public static String getStatusName(int status) {
        return switch (status) {
            case PEND -> "pending";
            case PASS -> "passed";
            case WITHDREW -> "withdrawn";
            default -> "";
        };
    }
}
