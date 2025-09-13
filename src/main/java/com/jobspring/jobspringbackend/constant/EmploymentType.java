package com.jobspring.jobspringbackend.constant;

public class EmploymentType {
    public static final int FULL_TIME = 1;  // 全职
    public static final int INTERNSHIP = 2;         // 实习
    public static final int CONTRACT = 3;  //合同工

    private EmploymentType() {
    }

    public static String getEmploymentType(int employmentType) {
        return switch (employmentType) {
            case FULL_TIME -> "Full-time";
            case INTERNSHIP -> "Internship";
            case CONTRACT -> "Contract";
            default -> "";
        };
    }
}
