package com.jobspring.jobspringbackend.repository;

import com.jobspring.jobspringbackend.entity.Application;
import com.jobspring.jobspringbackend.entity.Job;
import com.jobspring.jobspringbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    boolean existsByJobAndUser(Job job, User user);
}
