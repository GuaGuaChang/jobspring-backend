package com.jobspring.jobspringbackend.repository;

import com.jobspring.jobspringbackend.entity.CompanyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CompanyMemberRepository extends JpaRepository<CompanyMember, Long> {

    Optional<CompanyMember> findFirstByUserIdAndRole(Long userId, String role);

    boolean existsByUserIdAndCompanyIdAndRole(Long userId, Long companyId, String role);

    @Query("select cm.company.id from CompanyMember cm " +
            "where cm.user.id = :userId and cm.role = 'HR'")
    Optional<Long> findCompanyIdByHrUserId(Long userId);

    @Query("select cm.user.email from CompanyMember cm where cm.company.id = :companyId and cm.role in ('HR','Recruiter')")
    List<String> findHrEmailsByCompanyId(Long companyId);
}