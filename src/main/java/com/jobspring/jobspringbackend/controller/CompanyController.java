package com.jobspring.jobspringbackend.controller;

import com.jobspring.jobspringbackend.dto.CompanyReviewDTO;
import com.jobspring.jobspringbackend.dto.JobResponse;
import com.jobspring.jobspringbackend.entity.Company;
import com.jobspring.jobspringbackend.repository.CompanyRepository;
import com.jobspring.jobspringbackend.service.CompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyRepository companyRepository;

    @Autowired
    private CompanyService companyService;

    public CompanyController(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    // 创建公司
    @PostMapping
    public Company createCompany(@RequestBody Company req) {
        return companyRepository.save(req);
    }

    //查询某公司下的岗位列表
    @GetMapping("/{companyId}/jobs")
    public ResponseEntity<Page<JobResponse>> listCompanyJobs(
            @PathVariable Long companyId,
            @RequestParam(required = false, defaultValue = "0") Integer status,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                companyService.listCompanyJobs(companyId, status, pageable)
        );
    }

    @GetMapping("/{companyId}/reviews")
    public ResponseEntity<Page<CompanyReviewDTO>> getCompanyReviews(
            @PathVariable Long companyId,
            @PageableDefault(size = 10, sort = "submittedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<CompanyReviewDTO> reviews = companyService.getCompanyReviews(companyId, pageable);
        return ResponseEntity.ok(reviews);
    }
}
