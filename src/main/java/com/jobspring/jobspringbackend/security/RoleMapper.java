package com.jobspring.jobspringbackend.security;

public class RoleMapper {

    public static String toRoleName(int role) {
        return switch (role) {
            case 0 -> Roles.CANDIDATE;
            case 1 -> Roles.HR;
            case 2 -> Roles.ADMIN;
            default -> throw new IllegalArgumentException("Unknown role: " + role);
        };
    }

    public static int toRoleInt(String roleName) {
        return switch (roleName) {
            case Roles.CANDIDATE -> 0;
            case Roles.HR -> 1;
            case Roles.ADMIN -> 2;
            default -> throw new IllegalArgumentException("Unknown role name: " + roleName);
        };
    }
}
