package com.jobspring.jobspringbackend.controller;

import com.jobspring.jobspringbackend.entity.Company;
import com.jobspring.jobspringbackend.repository.CompanyRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {
    // TODO: Company related endpoints
    private final CompanyRepository companyRepository;

    public CompanyController(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    // 创建公司
    @PostMapping
    public Company createCompany(@RequestBody Company req) {
        return companyRepository.save(req);
    }
}
