package com.jobspring.jobspringbackend.service;

import com.jobspring.jobspringbackend.dto.ApplicationBriefResponse;
import com.jobspring.jobspringbackend.entity.Application;
import com.jobspring.jobspringbackend.entity.Job;
import com.jobspring.jobspringbackend.entity.Company;
import com.jobspring.jobspringbackend.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JobseekerApplicationService {

    private final ApplicationRepository applicationRepository;

    @Transactional(readOnly = true)
    public Page<ApplicationBriefResponse> listMine(Long userId, Integer status, Pageable pageable) {
        Page<Application> page = (status == null) ? applicationRepository.findMyApplications(userId, pageable) : applicationRepository.findMyApplicationsByStatus(userId, status, pageable);

        return page.map(this::toBrief);
    }

    private ApplicationBriefResponse toBrief(Application a) {
        ApplicationBriefResponse dto = new ApplicationBriefResponse();
        dto.setId(a.getId());
        dto.setStatus(a.getStatus());
        dto.setAppliedAt(a.getAppliedAt());
        dto.setResumeUrl(a.getResumeUrl());

        Job j = a.getJob();
        if (j != null) {
            dto.setJobId(j.getId());
            dto.setJobTitle(j.getTitle());

            Company c = j.getCompany();
            if (c != null) {
                dto.setCompanyId(c.getId());
                dto.setCompanyName(c.getName());
            }
        }
        return dto;
    }
}