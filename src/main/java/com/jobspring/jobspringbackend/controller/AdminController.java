package com.jobspring.jobspringbackend.controller;

import com.jobspring.jobspringbackend.dto.ApplicationBriefResponse;
import com.jobspring.jobspringbackend.dto.JobDTO;
import com.jobspring.jobspringbackend.dto.NoteDTO;
import com.jobspring.jobspringbackend.dto.ReviewDTO;
import com.jobspring.jobspringbackend.entity.Job;
import com.jobspring.jobspringbackend.entity.Review;
import com.jobspring.jobspringbackend.service.AdminService;
import com.jobspring.jobspringbackend.service.HrApplicationService;
import com.jobspring.jobspringbackend.service.JobService;
import com.jobspring.jobspringbackend.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @GetMapping("/check_review_debug")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> checkReviewDebug() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No authentication found");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("User principal: ").append(auth.getPrincipal()).append("\n");
        sb.append("Authorities: ");
        auth.getAuthorities().forEach(a -> sb.append(a.getAuthority()).append(" "));
        sb.append("\n");
        sb.append("Is authenticated: ").append(auth.isAuthenticated()).append("\n");

        return ResponseEntity.ok(sb.toString());
    }


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
        List<Review> reviews = reviewService.getAllReviews();
        List<ReviewDTO> reviewDTOs = reviews.stream()
                .map(review -> {
                    ReviewDTO reviewDTO = new ReviewDTO();
                    reviewDTO.setId(review.getId());
                    reviewDTO.setApplicationId(review.getApplication().getId());
                    reviewDTO.setTitle(review.getTitle());
                    reviewDTO.setContent(review.getContent());
                    reviewDTO.setRating(review.getRating());
                    reviewDTO.setStatus(review.getStatus());
                    reviewDTO.setSubmittedAt(review.getSubmittedAt());
                    reviewDTO.setReviewedById(review.getReviewedBy() != null ? review.getReviewedBy().getId() : null);
                    reviewDTO.setReviewNote(review.getReviewNote());
                    reviewDTO.setPublicAt(review.getPublicAt());
                    return reviewDTO;
                })
                .collect(Collectors.toList());
        return new ResponseEntity<>(reviewDTOs, HttpStatus.OK);
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
}
