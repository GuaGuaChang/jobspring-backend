package com.jobspring.jobspringbackend.service;

import com.jobspring.jobspringbackend.dto.*;
import com.jobspring.jobspringbackend.entity.Company;
import com.jobspring.jobspringbackend.entity.Job;
import com.jobspring.jobspringbackend.entity.User;
import com.jobspring.jobspringbackend.repository.CompanyRepository;
import com.jobspring.jobspringbackend.repository.JobRepository;
import com.jobspring.jobspringbackend.repository.SkillRepository;
import com.jobspring.jobspringbackend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import jakarta.persistence.criteria.*;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminService {
    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    private static final int ROLE_CANDIDATE = 0;
    private static final int ROLE_HR = 1;
    private static final int ROLE_ADMIN = 2;

    public Page<JobDTO> searchJobs(String keyword, Pageable pageable) {
        Page<Job> jobs = jobRepository.adminSearchJobs(keyword, pageable);
        return jobs.map(this::convertToJobSeekerDTO);
    }

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


    @Transactional
    public void makeHr(Long userId, PromoteToHrRequest req) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (Boolean.FALSE.equals(u.getIsActive())) {
            throw new IllegalStateException("User is inactive");
        }
        if (u.getRole() == ROLE_ADMIN) {
            throw new IllegalArgumentException("Cannot change role of ADMIN");
        }
        if (u.getRole() != ROLE_HR && u.getRole() != ROLE_CANDIDATE) {
            throw new IllegalArgumentException("Only candidate can be promoted to HR");
        }

        // 幂等升 HR
        u.setRole(ROLE_HR);

        // 可选：绑定/覆盖公司
        if (req != null && req.getCompanyId() != null) {
            boolean overwrite = req.getOverwriteCompany() == null || req.getOverwriteCompany();
            if (u.getCompany() == null || overwrite) {
                Company c = companyRepository.findById(req.getCompanyId())
                        .orElseThrow(() -> new EntityNotFoundException("Company not found"));
                u.setCompany(c);
            }
        }

        userRepository.save(u);
    }

    @Transactional(readOnly = true)
    public Page<JobSearchResponse> search(JobSearchCriteria c, Pageable pageable) {
        Specification<Job> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();

            // 标题模糊
            if (StringUtils.hasText(c.title())) {
                ps.add(cb.like(cb.lower(root.get("title")), "%" + c.title().toLowerCase() + "%"));
            }
            // 状态
            if (c.status() != null) {
                ps.add(cb.equal(root.get("status"), c.status()));
            }
            // 公司
            if (c.companyId() != null) {
                ps.add(cb.equal(root.get("company").get("id"), c.companyId()));
            }
            // 地点模糊
            if (StringUtils.hasText(c.location())) {
                ps.add(cb.like(cb.lower(root.get("location")), "%" + c.location().toLowerCase() + "%"));
            }
            // 用工类型
            if (c.employmentType() != null) {
                ps.add(cb.equal(root.get("employmentType"), c.employmentType()));
            }
            // 发布时间范围
            if (c.postedFrom() != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("postedAt"), c.postedFrom()));
            }
            if (c.postedTo() != null) {
                ps.add(cb.lessThanOrEqualTo(root.get("postedAt"), c.postedTo()));
            }
            // 关键字：标题或描述模糊
            if (StringUtils.hasText(c.keyword())) {
                String kw = "%" + c.keyword().toLowerCase() + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("title")), kw),
                        cb.like(cb.lower(root.get("description")), kw)
                ));
            }
            // 薪资区间重叠：(jobMin..jobMax) 与 (qMin..qMax) 有交集
            // 允许 jobMin 或 jobMax 为 null（视作无下限/无上限）
            if (c.salaryMin() != null || c.salaryMax() != null) {
                Expression<BigDecimal> jobMin = root.get("salaryMin");
                Expression<BigDecimal> jobMax = root.get("salaryMax");

                Predicate lowerOk;
                if (c.salaryMax() != null) {
                    // jobMin <= qMax OR jobMin IS NULL
                    lowerOk = cb.or(cb.isNull(jobMin), cb.lessThanOrEqualTo(jobMin, c.salaryMax()));
                } else {
                    // 只传了 qMin：不限制下界
                    lowerOk = cb.conjunction();
                }

                Predicate upperOk;
                if (c.salaryMin() != null) {
                    // jobMax >= qMin OR jobMax IS NULL
                    upperOk = cb.or(cb.isNull(jobMax), cb.greaterThanOrEqualTo(jobMax, c.salaryMin()));
                } else {
                    // 只传了 qMax：不限制上界
                    upperOk = cb.conjunction();
                }

                ps.add(cb.and(lowerOk, upperOk));
            }

            return cb.and(ps.toArray(new Predicate[0]));
        };

        return jobRepository.findAll(spec, pageable).map(this::toResp);
    }

    private JobSearchResponse toResp(Job j) {
        return new JobSearchResponse(
                j.getId(),
                j.getCompany() != null ? j.getCompany().getId() : null,
                j.getCompany() != null ? j.getCompany().getName() : null,
                j.getTitle(),
                j.getLocation(),
                j.getEmploymentType(),
                j.getSalaryMin(),
                j.getSalaryMax(),
                j.getStatus(),
                j.getPostedAt()
        );
    }
}

