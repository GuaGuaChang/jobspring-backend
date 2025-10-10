package com.jobspring.jobspringbackend.controller;

import com.jobspring.jobspringbackend.dto.*;
import com.jobspring.jobspringbackend.entity.Company;
import com.jobspring.jobspringbackend.entity.Job;
import com.jobspring.jobspringbackend.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    @Autowired
    private JobService jobService;

    @Autowired
    private AdminService adminService;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private HrApplicationService hrApplicationService;

    @Autowired
    private CompanyService companyService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/status")
    public ResponseEntity<List<Map<String, Object>>> getAllJobStatus() {
        List<Job> jobs = jobService.getAllJobs();
        List<Map<String, Object>> response = jobs.stream()
                .map(job -> {
                    Map<String, Object> jobInfo = new HashMap<>();
                    jobInfo.put("id", job.getId());
                    jobInfo.put("title", job.getTitle());
                    jobInfo.put("company", job.getCompany().getName());
                    jobInfo.put("companyId", job.getCompany().getId());
                    jobInfo.put("status", job.getStatus());
                    return jobInfo;
                })
                .collect(Collectors.toList());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/search")
    public Page<JobDTO> searchJobs(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("postedAt").descending());
        return adminService.searchJobs(keyword, pageable);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/check_review")
    public ResponseEntity<List<ReviewDTO>> checkReview() {
        List<ReviewDTO> reviewDTOs = reviewService.getAllReviews()
                .stream()
                .map(reviewService::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(reviewDTOs);
    }


    // 指定公司查看（会做归属校验）
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/companies/{companyId}/applications")
    public ResponseEntity<Page<ApplicationBriefResponse>> listByCompany(
            @PathVariable Long companyId,
            @RequestParam(required = false) Long jobId,
            @RequestParam(required = false) Integer status,
            Pageable pageable,
            Authentication auth
    ) {
        Long hrUserId = Long.parseLong(auth.getName());
        Page<ApplicationBriefResponse> page = hrApplicationService
                .listCompanyApplications(hrUserId, companyId, jobId, status, pageable);
        return ResponseEntity.ok(page);
    }

    // 下线岗位（快捷端点，可选）
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/companies/{companyId}/jobs/{jobId}/invalid")
    public ResponseEntity<Void> deactivate(@PathVariable Long companyId,
                                           @PathVariable Long jobId) {
        jobService.deactivateJob(companyId, jobId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/review/pass/{id}")
    public ResponseEntity<ReviewDTO> passReview(@PathVariable Long id,
                                                @RequestBody NoteDTO request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());

        ReviewDTO review = reviewService.approveReview(id, userId, request.getNote());
        return ResponseEntity.ok(review);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/review/reject/{id}")
    public ResponseEntity<ReviewDTO> rejectReview(@PathVariable Long id,
                                                  @RequestBody NoteDTO request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());

        ReviewDTO review = reviewService.rejectReview(id, userId, request.getNote());
        return ResponseEntity.ok(review);
    }

    //将某用户角色设置为 HR（1）
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{userId}/make-hr")
    public ResponseEntity<Void> makeHr(@PathVariable Long userId) {
        adminService.makeHr(userId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/company/list")
    public ResponseEntity<Page<CompanyDTO>> getAllCompanies(Pageable pageable) {
        Page<CompanyDTO> companies = companyService.getAllCompanies(pageable);
        return ResponseEntity.ok(companies);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/search_user")
    public ResponseEntity<Page<UserDTO>> searchUsers(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String fullName,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) Long id,
            Pageable pageable) {

        Page<UserDTO> result = adminService.searchUsers(email, fullName, phone, id, pageable);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/company/create")
    public ResponseEntity<CompanyDTO> createCompany(@RequestBody CompanyDTO companyDTO) {
        CompanyDTO savedCompany = companyService.createCompany(companyDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedCompany);
    }
}
