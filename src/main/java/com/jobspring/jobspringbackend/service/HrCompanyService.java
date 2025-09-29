package com.jobspring.jobspringbackend.service;

import com.jobspring.jobspringbackend.repository.CompanyMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HrCompanyService {

    private final CompanyMemberRepository memberRepo;

    public Long findCompanyIdByUserId(Long userId) {
        return memberRepo.findFirstByUserIdAndRole(userId, "HR")
                .map(cm -> cm.getCompany().getId())
                .orElseThrow(() -> new AccessDeniedException("Not HR or no company bound."));
    }

    public void assertHrInCompany(Long userId, Long companyId) {
        if (!memberRepo.existsByUserIdAndCompanyIdAndRole(userId, companyId, "HR")) {
            throw new AccessDeniedException("No permission for this company.");
        }
    }

    @Transactional(readOnly = true)
    public Long getCompanyIdOfHr(Long userId) {
        return memberRepo.findCompanyIdByHrUserId(userId)
                .orElseThrow(() -> new AccessDeniedException("当前用户未绑定公司或不是 HR"));
    }
}