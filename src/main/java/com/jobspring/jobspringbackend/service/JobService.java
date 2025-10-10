package com.jobspring.jobspringbackend.service;

import com.jobspring.jobspringbackend.dto.JobCreateRequest;
import com.jobspring.jobspringbackend.dto.JobDTO;
import com.jobspring.jobspringbackend.dto.JobResponse;
import com.jobspring.jobspringbackend.dto.JobUpdateRequest;
import com.jobspring.jobspringbackend.entity.Company;
import com.jobspring.jobspringbackend.entity.Job;
import com.jobspring.jobspringbackend.repository.*;
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

    @Autowired
    private CompanyMemberRepository companyMemberRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

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
            dto.setCompanyId(job.getCompany().getId());
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

    // HR：在指定公司下创建岗位（默认上架）
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

    // 编辑岗位（逻辑变成：复制新建 + 老的下线）
    @Transactional
    public JobResponse replaceJob(Long companyId, Long jobId, JobUpdateRequest req) {
        // 找到旧岗位
        Job oldJob = jobRepository.findByIdAndCompanyId(jobId, companyId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));

        // 将旧岗位下线
        oldJob.setStatus(1); // 1 = 下线
        jobRepository.save(oldJob);

        // 新建岗位（复制旧岗位的基础信息 + 更新请求内容）
        Job newJob = new Job();
        newJob.setCompany(oldJob.getCompany());
        newJob.setTitle(req.getTitle() != null ? req.getTitle() : oldJob.getTitle());
        newJob.setLocation(req.getLocation() != null ? req.getLocation() : oldJob.getLocation());
        newJob.setEmploymentType(req.getEmploymentType() != null ? req.getEmploymentType() : oldJob.getEmploymentType());
        newJob.setSalaryMin(req.getSalaryMin() != null ? req.getSalaryMin() : oldJob.getSalaryMin());
        newJob.setSalaryMax(req.getSalaryMax() != null ? req.getSalaryMax() : oldJob.getSalaryMax());
        newJob.setDescription(req.getDescription() != null ? req.getDescription() : oldJob.getDescription());
        newJob.setStatus(0); // 默认新建为上架
        newJob.setPostedAt(LocalDateTime.now());

        jobRepository.save(newJob);

        return toResponse(newJob);
    }


    @Transactional
    public void deactivateJob(Long companyId, Long jobId) {
        Job job = jobRepository.findByIdAndCompanyId(jobId, companyId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));

        // 1. 下线岗位
        job.setStatus(1);
        jobRepository.save(job);

        // 2. 同步更新所有相关申请状态为 7（无效）
        applicationRepository.updateStatusByJobId(jobId, 7);
    }

    // HR 查看本公司岗位（包含上下线）
    public Page<JobResponse> listJobs(Long companyId, Integer status, Pageable pageable) {
        Page<Job> page = (status == null)
                ? jobRepository.findByCompanyId(companyId, pageable)
                : jobRepository.findByCompanyIdAndStatus(companyId, status, pageable);
        return page.map(this::toResponse);
    }

    // 根据 userId 找到 HR 所属的公司
    public Long findCompanyIdByUserId(Long userId) {
        return companyMemberRepository.findFirstByUserIdAndRole(userId, "HR")
                .map(m -> m.getCompany().getId())
                .orElseThrow(() -> new EntityNotFoundException("HR membership not found"));
    }

}
