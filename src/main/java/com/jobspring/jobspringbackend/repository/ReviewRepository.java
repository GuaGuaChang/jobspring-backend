package com.jobspring.jobspringbackend.repository;

import com.jobspring.jobspringbackend.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findAll();

    @Query("""
                SELECT r FROM Review r
                JOIN r.application a
                JOIN a.job j
                WHERE j.company.id = :companyId
            """)
    Page<Review> findByCompanyId(@Param("companyId") Long companyId, Pageable pageable);
}
