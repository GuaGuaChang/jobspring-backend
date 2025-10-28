package com.jobspring.jobspringbackend.service;

import com.jobspring.jobspringbackend.dto.JobSeekerReviewDTO;
import com.jobspring.jobspringbackend.dto.ReviewDTO;
import com.jobspring.jobspringbackend.entity.*;
import com.jobspring.jobspringbackend.exception.BizException;
import com.jobspring.jobspringbackend.exception.ErrorCode;
import com.jobspring.jobspringbackend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private ApplicationRepository applicationRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private ReviewService service;

    private Review review;
    private Application app;
    private User admin;

    @BeforeEach
    void setup() {
        app = new Application();
        app.setId(100L);

        admin = new User();
        admin.setId(200L);
        admin.setFullName("Admin User");

        review = new Review();
        review.setId(1L);
        review.setApplication(app);
        review.setTitle("Great experience");
        review.setContent("Good interview process");
        review.setRating(5);
        review.setStatus(0);
        review.setSubmittedAt(LocalDateTime.now());
    }

    // ========== getAllReviews ==========
    @Test
    void getAllReviews_shouldReturnAll() {
        when(reviewRepository.findAll()).thenReturn(List.of(review));

        List<Review> result = service.getAllReviews();

        assertEquals(1, result.size());
        assertEquals("Great experience", result.get(0).getTitle());
        verify(reviewRepository, times(1)).findAll();
    }

    // ========== createReview ==========
    @Test
    void createReview_shouldCreateSuccessfully() {
        JobSeekerReviewDTO req = new JobSeekerReviewDTO();
        req.setApplicationId(100L);
        req.setTitle("New Review");
        req.setContent("Nice company");
        req.setRating(4);
        req.setImageUrl("img.png");

        when(applicationRepository.findById(100L)).thenReturn(Optional.of(app));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> {
            Review saved = inv.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        ReviewDTO result = service.createReview(req, 999L);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("New Review", result.getTitle());
        assertEquals(4, result.getRating());
        assertNull(result.getReviewedById());
        verify(reviewRepository, times(1)).save(any(Review.class));
    }

    @Test
    void createReview_shouldThrow_whenApplicationNotFound() {
        JobSeekerReviewDTO req = new JobSeekerReviewDTO();
        req.setApplicationId(404L);

        when(applicationRepository.findById(404L)).thenReturn(Optional.empty());

        BizException ex = assertThrows(BizException.class, () ->
                service.createReview(req, 1L)
        );

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Application not found"));
    }

    // ========== approveReview ==========
    @Test
    void approveReview_shouldApproveSuccessfully() {
        review.setStatus(0);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(userRepository.findById(200L)).thenReturn(Optional.of(admin));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewDTO result = service.approveReview(1L, 200L, "Approved!");

        assertEquals(1, result.getStatus());
        assertEquals("Approved!", result.getReviewNote());
        assertEquals(200L, result.getReviewedById());
        assertNotNull(result.getPublicAt());
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void approveReview_shouldThrow_whenNotPending() {
        review.setStatus(2);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        BizException ex = assertThrows(BizException.class, () ->
                service.approveReview(1L, 200L, "Already handled")
        );

        assertEquals(ErrorCode.CONFLICT, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("cannot approve"));
    }

    @Test
    void approveReview_shouldThrow_whenAdminNotFound() {
        review.setStatus(0);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(userRepository.findById(200L)).thenReturn(Optional.empty());

        BizException ex = assertThrows(BizException.class, () ->
                service.approveReview(1L, 200L, "Note")
        );

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Admin not found"));
    }

    // ========== rejectReview ==========
    @Test
    void rejectReview_shouldRejectSuccessfully() {
        review.setStatus(0);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(userRepository.findById(200L)).thenReturn(Optional.of(admin));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewDTO result = service.rejectReview(1L, 200L, "Inappropriate content");

        assertEquals(2, result.getStatus());
        assertEquals("Inappropriate content", result.getReviewNote());
        assertEquals(200L, result.getReviewedById());
        assertNotNull(result.getPublicAt());
        verify(reviewRepository, times(1)).save(any(Review.class));
    }

    @Test
    void rejectReview_shouldThrow_whenReviewNotFound() {
        when(reviewRepository.findById(404L)).thenReturn(Optional.empty());

        BizException ex = assertThrows(BizException.class, () ->
                service.rejectReview(404L, 200L, "Missing")
        );

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Review not found"));
    }
}
