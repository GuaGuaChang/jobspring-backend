package com.jobspring.jobspringbackend.repository;

import com.jobspring.jobspringbackend.entity.Job;
import com.jobspring.jobspringbackend.entity.JobFavorite;
import com.jobspring.jobspringbackend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobFavoriteRepository extends JpaRepository<JobFavorite, Long> {
    boolean existsByUserAndJob(User user, Job job);
    void deleteByUserAndJob(User user, Job job);
    Page<JobFavorite> findByUserId(Long userId, Pageable pageable);
    long countByJobId(Long jobId);
}