package com.jobspring.jobspringbackend.exception;

public class ConflictException extends RuntimeException {
    public ConflictException(String m) {
        super(m);
    }
}