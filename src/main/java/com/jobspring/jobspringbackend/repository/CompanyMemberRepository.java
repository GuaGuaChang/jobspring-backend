package com.jobspring.jobspringbackend.repository;

import com.jobspring.jobspringbackend.entity.CompanyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CompanyMemberRepository extends JpaRepository<CompanyMember, Long> {

    // 找到该用户的 HR 成员记录
    Optional<CompanyMember> findFirstByUserIdAndRole(Long userId, String role);

    // 校验：该用户是否是这家公司的 HR（如需要额外鉴权可用）
    boolean existsByUserIdAndCompanyIdAndRole(Long userId, Long companyId, String role);

    @Query("select cm.company.id from CompanyMember cm " +
            "where cm.user.id = :userId and cm.role = 'HR'")
    Optional<Long> findCompanyIdByHrUserId(Long userId);
}