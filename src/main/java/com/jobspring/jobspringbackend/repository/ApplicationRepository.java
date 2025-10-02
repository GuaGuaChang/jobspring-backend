package com.jobspring.jobspringbackend.repository;

import com.jobspring.jobspringbackend.entity.Application;
import com.jobspring.jobspringbackend.entity.Job;
import com.jobspring.jobspringbackend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    boolean existsByJobAndUser(Job job, User user);

    @Query("""
            select a
            from Application a
            where a.job.company.id = :companyId
              and (:jobId is null or a.job.id = :jobId)
              and (:status is null or a.status = :status)
            order by a.appliedAt desc
            """)
    Page<Application> searchByCompany(
            @Param("companyId") Long companyId,
            @Param("jobId") Long jobId,
            @Param("status") Integer status,
            Pageable pageable
    );

    @Modifying
    @Query("UPDATE Application a SET a.status = :newStatus WHERE a.job.id = :jobId")
    int updateStatusByJobId(@Param("jobId") Long jobId, @Param("newStatus") Integer newStatus);

    // 带抓取 Job 和 Company，避免 N+1
    @EntityGraph(attributePaths = {"job", "job.company"})
    @Query("select a from Application a where a.user.id = :userId order by a.appliedAt desc")
    Page<Application> findMyApplications(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"job", "job.company"})
    @Query("select a from Application a where a.user.id = :userId and a.status = :status order by a.appliedAt desc")
    Page<Application> findMyApplicationsByStatus(Long userId, Integer status, Pageable pageable);
}
