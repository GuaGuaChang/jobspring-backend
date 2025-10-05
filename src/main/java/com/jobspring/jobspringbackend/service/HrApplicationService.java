package com.jobspring.jobspringbackend.service;

import com.jobspring.jobspringbackend.dto.ApplicationBriefResponse;
import com.jobspring.jobspringbackend.entity.Application;
import com.jobspring.jobspringbackend.repository.ApplicationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class HrApplicationService {

    private final ApplicationRepository applicationRepository;
    private final HrCompanyService hrCompanyService; // A 方案；如果是 B 方案，就注入 UserRepository 等

    public Page<ApplicationBriefResponse> listCompanyApplications(
            Long hrUserId,
            Long companyId,     // 可选：前端传了就校验；不传就自动推断
            Long jobId,         // 可选
            Integer status,     // 可选
            Pageable pageable
    ) {
        // 允许两种用法：1) 不传 companyId → 从 HR 绑定信息推断；2) 传 companyId → 校验归属
        final Long effectiveCompanyId = (companyId == null)
                ? hrCompanyService.findCompanyIdByUserId(hrUserId)
                : validateAndReturn(hrUserId, companyId);

        Page<Application> page = applicationRepository.searchByCompany(effectiveCompanyId, jobId, status, pageable);
        return page.map(this::toBrief);
    }

    private Long validateAndReturn(Long hrUserId, Long companyId) {
        hrCompanyService.assertHrInCompany(hrUserId, companyId);
        return companyId;
    }

    private ApplicationBriefResponse toBrief(Application a) {
        ApplicationBriefResponse r = new ApplicationBriefResponse();
        r.setId(a.getId());
        r.setJobId(a.getJob().getId());
        r.setJobTitle(a.getJob().getTitle());
        r.setApplicantId(a.getUser().getId());
        r.setApplicantName(a.getUser().getFullName());
        r.setStatus(a.getStatus());
        r.setAppliedAt(a.getAppliedAt());
        r.setResumeUrl(a.getResumeUrl());
        return r;
    }

    private static final Set<Integer> ALLOWED =
            Set.of(0, 1, 2, 3, 4); // 你的状态集合

    @Transactional
    public ApplicationBriefResponse updateStatus(Long hrUserId, Long applicationId, Integer newStatus) {
        if (newStatus == null || !ALLOWED.contains(newStatus)) {
            throw new IllegalArgumentException("Illegal application status：" + newStatus);
        }

        // 取出申请 + 关联的 job & company
        Application app = applicationRepository.findByIdWithJobAndCompany(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Application not found"));

        // HR 归属校验：只能改自己公司的申请
        Long hrCompanyId = hrCompanyService.findCompanyIdByUserId(hrUserId);
        Long appCompanyId = app.getJob().getCompany().getId();
        if (!appCompanyId.equals(hrCompanyId)) {
            throw new AccessDeniedException("No permission to operate applications from other companies");
        }

        if (app.getJob().getStatus() != 0) {
            throw new IllegalStateException("This position is no longer valid and the application status cannot be modified");
        }

        // 更新状态
        app.setStatus(newStatus);

        return toBrief(app);
    }
}