package com.jobspring.jobspringbackend.repository;

import com.jobspring.jobspringbackend.entity.Profile;
import com.jobspring.jobspringbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProfileRepository extends JpaRepository<Profile, Long> {
    Optional<Profile> findByUserId(Long userId);
    Optional<Profile> findByUser(User user);
}

