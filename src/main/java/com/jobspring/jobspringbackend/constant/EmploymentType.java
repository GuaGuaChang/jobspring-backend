package com.jobspring.jobspringbackend.constant;

public class EmploymentType {
    public static final int FULLTIME = 1;  // 全职
    public static final int INTERN = 2;         // 实习
    public static final int CONTRACT = 3;  //合同工

    private EmploymentType() {}

    public static String getEmploymentType(int employmentType){
        return switch (employmentType) {
            case FULLTIME -> "full time job";
            case INTERN -> "internal job";
            case CONTRACT -> "contract worker";
            default -> "";
        };
    }
}
