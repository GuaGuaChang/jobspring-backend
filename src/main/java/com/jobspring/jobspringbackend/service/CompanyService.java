package com.jobspring.jobspringbackend.service;

import com.jobspring.jobspringbackend.dto.CompanyDTO;
import com.jobspring.jobspringbackend.dto.JobResponse;
import com.jobspring.jobspringbackend.entity.Company;
import com.jobspring.jobspringbackend.entity.Job;
import com.jobspring.jobspringbackend.repository.CompanyRepository;
import com.jobspring.jobspringbackend.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class CompanyService {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private JobRepository jobRepository;

    public CompanyDTO getCompanyById(Long id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Company not found with id: " + id));

        return convertToCompanyDTO(company);
    }

    private CompanyDTO convertToCompanyDTO(Company company) {
        CompanyDTO dto = new CompanyDTO();
        dto.setId(company.getId());
        dto.setName(company.getName());
        dto.setWebsite(company.getWebsite());
        dto.setSize(company.getSize());
        dto.setLogoUrl(company.getLogoUrl());
        dto.setDescription(company.getDescription());
        dto.setCreatedBy(company.getCreatedBy());
        return dto;
    }

    public Page<JobResponse> listCompanyJobs(Long companyId, Integer status, Pageable pageable) {
        Page<Job> page = (status == null)
                ? jobRepository.findByCompanyId(companyId, pageable)
                : jobRepository.findByCompanyIdAndStatus(companyId, status, pageable);
        return page.map(this::toResponse);
    }

    private JobResponse toResponse(Job j) {
        JobResponse r = new JobResponse();
        r.setId(j.getId());
        r.setCompanyId(j.getCompany().getId());
        r.setTitle(j.getTitle());
        r.setLocation(j.getLocation());
        r.setEmploymentType(j.getEmploymentType());
        r.setSalaryMin(j.getSalaryMin());
        r.setSalaryMax(j.getSalaryMax());
        r.setDescription(j.getDescription());
        r.setStatus(j.getStatus());
        r.setPostedAt(j.getPostedAt());
        return r;
    }
}

