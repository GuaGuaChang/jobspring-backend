package com.jobspring.jobspringbackend.service;

import com.jobspring.jobspringbackend.dto.ApplicationBriefResponse;
import com.jobspring.jobspringbackend.entity.Application;
import com.jobspring.jobspringbackend.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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
}