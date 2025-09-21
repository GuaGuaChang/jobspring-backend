package com.jobspring.jobspringbackend.service;

import com.jobspring.jobspringbackend.dto.JobCreateRequest;
import com.jobspring.jobspringbackend.dto.JobDTO;
import com.jobspring.jobspringbackend.dto.JobResponse;
import com.jobspring.jobspringbackend.dto.JobUpdateRequest;
import com.jobspring.jobspringbackend.entity.Company;
import com.jobspring.jobspringbackend.entity.Job;
import com.jobspring.jobspringbackend.repository.JobRepository;
import com.jobspring.jobspringbackend.repository.SkillRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class JobService {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private SkillRepository skillRepository;

    // 为求职者获取职位列表
    public Page<JobDTO> getJobSeekerJobs(Pageable pageable) {
        Page<Job> jobs = jobRepository.findByStatus(0, pageable);
        return jobs.map(this::convertToJobSeekerDTO);
    }

    // 搜索职位（求职者用）
    public Page<JobDTO> searchJobSeekerJobs(String keyword, Pageable pageable) {
        Page<Job> jobs = jobRepository.searchJobs(keyword, pageable);
        return jobs.map(this::convertToJobSeekerDTO);
    }

    // 转换方法
    private JobDTO convertToJobSeekerDTO(Job job) {
        JobDTO dto = new JobDTO();
        dto.setId(job.getId());
        dto.setTitle(job.getTitle());
        dto.setLocation(job.getLocation());
        dto.setSalaryMin(job.getSalaryMin());
        dto.setSalaryMax(job.getSalaryMax());
        dto.setPostedAt(job.getPostedAt());

        dto.setEmploymentType(getEmploymentTypeName(job.getEmploymentType()));

        if (job.getCompany() != null) {
            dto.setCompany(job.getCompany().getName());
        }

        dto.setDescription(job.getDescription());

        dto.setTags(getJobTags(job.getId()));

        return dto;
    }

    // 获取工作类型名称
    private String getEmploymentTypeName(Integer type) {
        if (type == null) return "未知";
        return switch (type) {
            case 1 -> "Full-time";
            case 2 -> "Internship";
            case 3 -> "Contract";
            default -> "未知";
        };
    }

    // 获取职位标签（技能）
    private List<String> getJobTags(Long jobId) {
        return skillRepository.findSkillNamesByJobId(jobId);
    }

    private void validateSalaryRange(JobCreateRequest req) {
        if (req.getSalaryMin() != null && req.getSalaryMax() != null
                && req.getSalaryMin().compareTo(req.getSalaryMax()) > 0) {
            throw new IllegalArgumentException("salaryMin cannot be greater than salaryMax");
        }
    }

    private void validateSalaryRange(JobUpdateRequest req) {
        if (req.getSalaryMin() != null && req.getSalaryMax() != null
                && req.getSalaryMin().compareTo(req.getSalaryMax()) > 0) {
            throw new IllegalArgumentException("salaryMin cannot be greater than salaryMax");
        }
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

    /** HR/ADMIN：在指定公司下创建岗位（默认上架） */
    @Transactional
    public JobResponse createJob(Long companyId, JobCreateRequest req) {
        validateSalaryRange(req);

        Job j = new Job();
        Company c = new Company();
        c.setId(companyId);
        j.setCompany(c);

        j.setTitle(req.getTitle());
        j.setLocation(req.getLocation());
        j.setEmploymentType(req.getEmploymentType());
        j.setSalaryMin(req.getSalaryMin());
        j.setSalaryMax(req.getSalaryMax());
        j.setDescription(req.getDescription());
        j.setStatus(0); // 上架
        j.setPostedAt(LocalDateTime.now());

        jobRepository.save(j);
        return toResponse(j);
    }

    /** HR/ADMIN：编辑岗位（含上下线） */
    @Transactional
    public JobResponse updateJob(Long companyId, Long jobId, JobUpdateRequest req) {
        Job j = jobRepository.findByIdAndCompany_Id(jobId, companyId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));

        validateSalaryRange(req);

        if (req.getTitle() != null) j.setTitle(req.getTitle());
        if (req.getLocation() != null) j.setLocation(req.getLocation());
        if (req.getEmploymentType() != null) j.setEmploymentType(req.getEmploymentType());
        if (req.getSalaryMin() != null) j.setSalaryMin(req.getSalaryMin());
        if (req.getSalaryMax() != null) j.setSalaryMax(req.getSalaryMax());
        if (req.getDescription() != null) j.setDescription(req.getDescription());
        if (req.getStatus() != null && !Objects.equals(j.getStatus(), req.getStatus())) {
            j.setStatus(req.getStatus()); // 0 上架 / 1 下线
        }

        return toResponse(j);
    }

    /** HR/ADMIN：下线岗位（快捷端点） */
    @Transactional
    public void deactivateJob(Long companyId, Long jobId) {
        Job j = jobRepository.findByIdAndCompany_Id(jobId, companyId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));
        j.setStatus(1);
    }

    /** HR 查看本公司岗位（包含上下线） */
    public Page<JobResponse> listJobs(Long companyId, Integer status, Pageable pageable) {
        return jobRepository.findAll(pageable)
                .map(this::toResponse)
                .map(r -> r);
    }
}
