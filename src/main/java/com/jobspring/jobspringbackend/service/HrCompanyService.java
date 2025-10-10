package com.jobspring.jobspringbackend.service;

import com.jobspring.jobspringbackend.entity.User;
import com.jobspring.jobspringbackend.repository.CompanyMemberRepository;
import com.jobspring.jobspringbackend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HrCompanyService {

    private final CompanyMemberRepository memberRepo;

    private final UserRepository userRepository;

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
                .orElseThrow(() -> new AccessDeniedException("The current user is not bound to a company or is not an HR"));
    }

    @Transactional(readOnly = true)
    public String getMyCompanyName(Long userId) {
        User u = userRepository.findWithCompanyById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        if (u.getCompany() == null) {
            throw new EntityNotFoundException("No company bound for this HR");
        }
        return u.getCompany().getName();
    }
}