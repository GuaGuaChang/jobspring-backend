package com.jobspring.jobspringbackend.constant;

public class ApplicationStatus {
    public static final int SUBMIT = 0;    // 有效/激活
    public static final int FILTER = 1;  // 无效/禁用
    public static final int APPOINTMENT = 2;   // 待处理
    public static final int INTERVIEW = 3;   // 已删除
    public static final int PASS = 4;
    public static final int REJECT = 5;
    public static final int WITHDREW = 6;

    private ApplicationStatus() {
    }

    public static String getStatusName(int status) {
        return switch (status) {
            case SUBMIT -> "submitted";
            case FILTER -> "filtering";
            case APPOINTMENT -> "make an appointment to meet";
            case INTERVIEW -> "in the interview";
            case PASS -> "passed";
            case REJECT -> "rejected";
            case WITHDREW -> "withdrawn";
            default -> "";
        };
    }
}
