package com.jobspring.jobspringbackend.repository;

import com.jobspring.jobspringbackend.entity.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
    Page<Job> findByStatus(@Param("status") Integer status, Pageable pageable);

    // 搜索职位（标题、地点、公司名）
    @Query("SELECT j FROM Job j WHERE j.status = 0 AND " +
            "(LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(j.location) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(j.company.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Job> searchJobs(@Param("keyword") String keyword, Pageable pageable);

    // 创建，修改，删除职位
    boolean existsByIdAndCompany_Id(@Param("jobId") Long jobId, @Param("companyId") Long companyId);
    Optional<Job> findByIdAndCompany_Id(Long jobId, Long companyId);

}
