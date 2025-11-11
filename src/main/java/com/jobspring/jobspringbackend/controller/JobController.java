package com.jobspring.jobspringbackend.controller;

import com.jobspring.jobspringbackend.dto.JobCreateRequest;
import com.jobspring.jobspringbackend.dto.JobResponse;
import com.jobspring.jobspringbackend.dto.JobUpdateRequest;
import com.jobspring.jobspringbackend.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/hr")
@RequiredArgsConstructor
public class JobController {
    private final JobService jobService;

    @PreAuthorize("hasAnyRole('HR')")
    @PostMapping("/companies/{companyId}/jobs")
    public ResponseEntity<JobResponse> create(@PathVariable Long companyId, @Valid @RequestBody JobCreateRequest req) {
        JobResponse res = jobService.createJob(companyId, req);
        return ResponseEntity.created(URI.create("/api/hr/jobs/" + res.getId())).body(res);
    }

    @PreAuthorize("hasAnyRole('HR')")
    @PatchMapping("/companies/{companyId}/jobs/{jobId}")
    public ResponseEntity<JobResponse> update(@PathVariable Long companyId, @PathVariable Long jobId, @Valid @RequestBody JobUpdateRequest req) {
        JobResponse res = jobService.replaceJob(companyId, jobId, req);
        return ResponseEntity.ok(res);
    }

    @PreAuthorize("hasAnyRole('HR')")
    @PostMapping("/companies/{companyId}/jobs/{jobId}/invalid")
    public ResponseEntity<Void> deactivate(@PathVariable Long companyId, @PathVariable Long jobId) {
        jobService.deactivateJob(companyId, jobId);
        return ResponseEntity.noContent().build();
    }


    @PreAuthorize("hasRole('HR')")
    @GetMapping("/companies/jobs")
    public ResponseEntity<Page<JobResponse>> list(Pageable pageable, @RequestParam(required = false) Integer status, Authentication auth) {
        Long userId = Long.valueOf(auth.getName());
        Long companyId = jobService.findCompanyIdByUserId(userId);
        return ResponseEntity.ok(jobService.listJobs(companyId, status, pageable));
    }
}