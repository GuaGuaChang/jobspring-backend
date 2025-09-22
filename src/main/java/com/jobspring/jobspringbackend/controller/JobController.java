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
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/hr")
@RequiredArgsConstructor
public class JobController {
    private final JobService jobService;

    /** 新建岗位（默认上架） */
    @PreAuthorize("hasAnyRole('HR')")
    @PostMapping("/companies/{companyId}/jobs")
    public ResponseEntity<JobResponse> create(@PathVariable Long companyId,
                                              @Valid @RequestBody JobCreateRequest req) {
        JobResponse res = jobService.createJob(companyId, req);
        return ResponseEntity.created(URI.create("/api/hr/jobs/" + res.getId())).body(res);
    }

    /** 编辑岗位（含上下线） */
    @PreAuthorize("hasAnyRole('HR')")
    @PatchMapping("/companies/{companyId}/jobs/{jobId}")
    public ResponseEntity<JobResponse> update(@PathVariable Long companyId,
                                              @PathVariable Long jobId,
                                              @Valid @RequestBody JobUpdateRequest req) {
        return ResponseEntity.ok(jobService.updateJob(companyId, jobId, req));
    }

    /** 下线岗位（快捷端点，可选） */
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    @PostMapping("/companies/{companyId}/jobs/{jobId}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable Long companyId,
                                           @PathVariable Long jobId) {
        jobService.deactivateJob(companyId, jobId);
        return ResponseEntity.noContent().build();
    }

    /** HR 后台列表（演示：分页；必要时补 status/company 过滤） */
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    @GetMapping("/companies/{companyId}/jobs")
    public ResponseEntity<Page<JobResponse>> list(@PathVariable Long companyId,
                                                  @RequestParam(required = false) Integer status,
                                                  Pageable pageable) {
        return ResponseEntity.ok(jobService.listJobs(companyId, status, pageable));
    }
}
