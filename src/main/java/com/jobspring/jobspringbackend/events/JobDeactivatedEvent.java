package com.jobspring.jobspringbackend.events;

public record JobDeactivatedEvent(Long companyId, Long jobId) {
}
