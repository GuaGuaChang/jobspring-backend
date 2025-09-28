package com.jobspring.jobspringbackend.service;

import com.jobspring.jobspringbackend.dto.CompanyDTO;
import com.jobspring.jobspringbackend.entity.Company;
import com.jobspring.jobspringbackend.repository.CompanyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CompanyService {

    @Autowired
    private CompanyRepository companyRepository;

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
}

