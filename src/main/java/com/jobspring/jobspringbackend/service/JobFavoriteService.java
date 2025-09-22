package com.jobspring.jobspringbackend.service;

import com.jobspring.jobspringbackend.dto.FavoriteJobResponse;
import com.jobspring.jobspringbackend.entity.Job;
import com.jobspring.jobspringbackend.entity.JobFavorite;
import com.jobspring.jobspringbackend.entity.User;
import com.jobspring.jobspringbackend.repository.JobFavoriteRepository;
import com.jobspring.jobspringbackend.repository.JobRepository;
import com.jobspring.jobspringbackend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobFavoriteService {
    private final JobFavoriteRepository favoriteRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;

    @Transactional
    public void add(Long userId, Long jobId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));
        if (job.getStatus() != null && job.getStatus() != 0) {
            throw new IllegalStateException("Job inactive");
        }
        if (!favoriteRepository.existsByUserAndJob(user, job)) {
            JobFavorite fav = new JobFavorite();
            fav.setUser(user);
            fav.setJob(job);
            favoriteRepository.save(fav);
        }
    }

    @Transactional
    public void remove(Long userId, Long jobId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));
        favoriteRepository.deleteByUserAndJob(user, job);
    }

    public boolean isFavorited(Long userId, Long jobId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));
        return favoriteRepository.existsByUserAndJob(user, job);
    }

    public Page<FavoriteJobResponse> list(Long userId, Pageable pageable) {
        return favoriteRepository.findByUserId(userId, pageable)
                .map(f -> {
                    FavoriteJobResponse r = new FavoriteJobResponse();
                    r.setJobId(f.getJob().getId());
                    r.setTitle(f.getJob().getTitle());
                    r.setCompany(f.getJob().getCompany() != null ? f.getJob().getCompany().getName() : null);
                    r.setLocation(f.getJob().getLocation());
                    r.setEmploymentType(f.getJob().getEmploymentType());
                    r.setStatus(f.getJob().getStatus());
                    r.setFavoritedAt(f.getCreatedAt());
                    return r;
                });
    }

    public long countByJob(Long jobId) {
        return favoriteRepository.countByJobId(jobId);
    }
}