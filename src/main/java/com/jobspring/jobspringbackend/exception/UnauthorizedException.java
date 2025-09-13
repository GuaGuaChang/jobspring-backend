package com.jobspring.jobspringbackend.exception;

public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String m) {
        super(m);
    }
}