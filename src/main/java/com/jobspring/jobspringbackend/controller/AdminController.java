package com.jobspring.jobspringbackend.controller;

import com.jobspring.jobspringbackend.dto.JobDTO;
import com.jobspring.jobspringbackend.dto.NoteDTO;
import com.jobspring.jobspringbackend.dto.ReviewDTO;
import com.jobspring.jobspringbackend.entity.Job;
import com.jobspring.jobspringbackend.entity.Review;
import com.jobspring.jobspringbackend.entity.User;
import com.jobspring.jobspringbackend.service.AdminService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @GetMapping("/status")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
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

    @GetMapping("/search")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public Page<JobDTO> searchJobs(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("postedAt").descending());
        return adminService.searchJobs(keyword, pageable);
    }

    @GetMapping("/check_review")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
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

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/review/pass/{id}")
    public ResponseEntity<ReviewDTO> passReview(@PathVariable Long id,
                                                @RequestBody NoteDTO request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());

        ReviewDTO review = reviewService.approveReview(id, userId, request.getNote());
        return ResponseEntity.ok(review);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/review/reject/{id}")
    public ResponseEntity<ReviewDTO> rejectReview(@PathVariable Long id,
                                                  @RequestBody NoteDTO request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());

        ReviewDTO review = reviewService.rejectReview(id, userId, request.getNote());
        return ResponseEntity.ok(review);
    }
}
