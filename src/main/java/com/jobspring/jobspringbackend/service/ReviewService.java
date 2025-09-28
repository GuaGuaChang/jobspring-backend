package com.jobspring.jobspringbackend.service;

import com.jobspring.jobspringbackend.dto.JobSeekerReviewDTO;
import com.jobspring.jobspringbackend.dto.ReviewDTO;
import com.jobspring.jobspringbackend.entity.Application;
import com.jobspring.jobspringbackend.entity.Review;
import com.jobspring.jobspringbackend.entity.User;
import com.jobspring.jobspringbackend.exception.BizException;
import com.jobspring.jobspringbackend.exception.ErrorCode;
import com.jobspring.jobspringbackend.repository.ApplicationRepository;
import com.jobspring.jobspringbackend.repository.ReviewRepository;
import com.jobspring.jobspringbackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private UserRepository userRepository;


    public List<Review> getAllReviews() {
        return reviewRepository.findAll();
    }

    @Transactional
    public ReviewDTO createReview(JobSeekerReviewDTO jobseekerReviewDTO, Long userId) {
        // Retrieve the application by ID
        Application application = applicationRepository.findById(jobseekerReviewDTO.getApplicationId())
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "Application not found"));


        // Create a new review
        Review review = new Review();
        review.setApplication(application);
        review.setTitle(jobseekerReviewDTO.getTitle());
        review.setContent(jobseekerReviewDTO.getContent());
        review.setRating(jobseekerReviewDTO.getRating());
        review.setStatus(0); // Default status 0
        review.setSubmittedAt(LocalDateTime.now());
        review.setReviewedBy(null);
        review.setReviewNote(null); // Set reviewNote to null
        review.setPublicAt(null); // Set publicAt to null initially

        Review saved = reviewRepository.save(review);

        return toDto(saved);
    }

    @Transactional
    public ReviewDTO approveReview(Long id, Long adminId, String note) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "Review not found"));

        if (review.getStatus() != 0) {
            throw new BizException(ErrorCode.CONFLICT, "Review is not pending, cannot approve");
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "Admin not found"));

        review.setStatus(1); // approved
        review.setReviewedBy(admin);
        review.setReviewNote(note);
        review.setPublicAt(LocalDateTime.now());

        Review saved = reviewRepository.save(review);

        return toDto(saved);
    }

    @Transactional
    public ReviewDTO rejectReview(Long id, Long adminId, String note) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "Review not found"));

        if (review.getStatus() != 0) {
            throw new BizException(ErrorCode.CONFLICT, "Review is not pending, cannot reject");
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "Admin not found"));

        review.setStatus(2); // withdraw
        review.setReviewedBy(admin);
        review.setReviewNote(note);
        review.setPublicAt(LocalDateTime.now());

        Review saved = reviewRepository.save(review);

        return toDto(saved);
    }

    private ReviewDTO toDto(Review review) {
        ReviewDTO dto = new ReviewDTO();
        dto.setId(review.getId());
        dto.setApplicationId(review.getApplication().getId());
        dto.setTitle(review.getTitle());
        dto.setContent(review.getContent());
        dto.setRating(review.getRating());
        dto.setStatus(review.getStatus());
        dto.setSubmittedAt(review.getSubmittedAt());
        dto.setReviewedById(review.getReviewedBy() != null ? review.getReviewedBy().getId() : null);
        dto.setReviewNote(review.getReviewNote());
        dto.setPublicAt(review.getPublicAt());
        return dto;
    }
}
