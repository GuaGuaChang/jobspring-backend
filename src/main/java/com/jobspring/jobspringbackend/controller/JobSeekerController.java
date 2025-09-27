package com.jobspring.jobspringbackend.controller;

import com.jobspring.jobspringbackend.dto.CompanyDTO;
import com.jobspring.jobspringbackend.dto.JobDTO;
import com.jobspring.jobspringbackend.dto.JobSeekerReviewDTO;
import com.jobspring.jobspringbackend.dto.ReviewDTO;
import com.jobspring.jobspringbackend.entity.Review;
import com.jobspring.jobspringbackend.entity.User;
import com.jobspring.jobspringbackend.repository.UserRepository;
import com.jobspring.jobspringbackend.service.CompanyService;
import com.jobspring.jobspringbackend.service.JobService;
import com.jobspring.jobspringbackend.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

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

    // 获取求职者职位列表
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

    // 搜索职位
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
}
