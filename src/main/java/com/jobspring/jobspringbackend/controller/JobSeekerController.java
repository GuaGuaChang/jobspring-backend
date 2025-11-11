package com.jobspring.jobspringbackend.controller;

import com.jobspring.jobspringbackend.dto.*;
import com.jobspring.jobspringbackend.repository.UserRepository;
import com.jobspring.jobspringbackend.service.CompanyService;
import com.jobspring.jobspringbackend.service.JobService;
import com.jobspring.jobspringbackend.service.JobseekerApplicationService;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/job_seeker")
public class JobSeekerController {

    @Autowired
    private JobService jobService;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobseekerApplicationService jobseekerApplicationService;


    @GetMapping("/job_list")
    public Page<JobDTO> getJobList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "postedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return jobService.getJobSeekerJobs(pageable);
    }


    @GetMapping("/job_list/search")
    public Page<JobDTO> searchJobs(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("postedAt").descending());
        return jobService.searchJobSeekerJobs(keyword, pageable);
    }

    @GetMapping("/company/{id}")
    public CompanyDTO getCompany(@PathVariable Long id) {
        return companyService.getCompanyById(id);
    }

    @PostMapping("/postReview")
    public ResponseEntity<ReviewDTO> postReview(@RequestBody JobSeekerReviewDTO reviewDTO) {
        // Get the current user ID from the security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());

        // Create the review
        ReviewDTO review = reviewService.createReview(reviewDTO, userId);

        return new ResponseEntity<>(review, HttpStatus.CREATED);
    }


    //GET /api/me/applications?status=0&page=0&size=10&sort=appliedAt,desc
    @GetMapping("/applications")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<Page<ApplicationBriefResponse>> jobseekerApplications(
            @RequestParam(required = false) Integer status,
            Pageable pageable,
            Authentication auth) {


        Long userId = Long.valueOf(auth.getName());
        Page<ApplicationBriefResponse> page = jobseekerApplicationService.listMine(userId, status, pageable);
        return ResponseEntity.ok(page);
    }
}