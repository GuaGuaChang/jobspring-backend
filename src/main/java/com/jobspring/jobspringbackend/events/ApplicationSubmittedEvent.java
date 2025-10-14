package com.jobspring.jobspringbackend.events;

public record ApplicationSubmittedEvent(
        Long applicationId,
        Long jobId,
        Long companyId,
        String jobTitle,
        Long applicantUserId,
        String applicantName,
        String applicantEmail
) {
}