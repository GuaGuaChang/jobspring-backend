package com.jobspring.jobspringbackend.service;

import com.jobspring.jobspringbackend.repository.CompanyMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

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
}