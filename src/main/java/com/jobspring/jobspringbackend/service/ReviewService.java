package com.jobspring.jobspringbackend.service;

import com.jobspring.jobspringbackend.entity.Review;
import com.jobspring.jobspringbackend.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    public List<Review> getAllReviews() {
        return reviewRepository.findAll();
    }
}
