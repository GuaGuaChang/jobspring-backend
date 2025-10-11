package com.jobspring.jobspringbackend.service;


import com.jobspring.jobspringbackend.dto.*;
import com.jobspring.jobspringbackend.entity.Company;

import com.jobspring.jobspringbackend.entity.Job;
import com.jobspring.jobspringbackend.entity.User;
import com.jobspring.jobspringbackend.repository.CompanyRepository;

import com.jobspring.jobspringbackend.repository.JobRepository;
import com.jobspring.jobspringbackend.repository.SkillRepository;
import com.jobspring.jobspringbackend.repository.UserRepository;
import com.jobspring.jobspringbackend.repository.spec.UserSpecs;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
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
    public Page<JobSearchResponse> search(String q, Pageable pageable) {
        Specification<Job> spec = (root, query, cb) -> {
            // 空查询：不加任何条件，等价“查全部”（仍支持分页/排序）
            if (!StringUtils.hasText(q)) return cb.conjunction();

            // 预先 join company 以便按公司名查
            Join<Object, Object> companyJoin = root.join("company", JoinType.LEFT);

            // 分词：例如 "java 上海 12000" => ["java","上海","12000"]
            String[] tokens = Arrays.stream(q.trim().split("\\s+"))
                    .filter(StringUtils::hasText)
                    .toArray(String[]::new);

            // 对每个 token：在多字段中 OR；多个 token 之间 AND
            List<Predicate> andPerToken = new ArrayList<>();

            for (String t : tokens) {
                String kw = "%" + t.toLowerCase() + "%";
                List<Predicate> orFields = new ArrayList<>();

                // 文本字段模糊：title / description / location / company.name
                orFields.add(cb.like(cb.lower(root.get("title")), kw));
                orFields.add(cb.like(root.get("description").as(String.class), "%" + t + "%"));
                orFields.add(cb.like(cb.lower(root.get("location")), kw));
                orFields.add(cb.like(cb.lower(companyJoin.get("name")), kw));

                // 数字：尝试当作 id / 状态 / 薪资值（薪资“区间重叠”）
                if (t.matches("\\d+")) {
                    long asLong = Long.parseLong(t);
                    orFields.add(cb.equal(root.get("id"), asLong));
                    // 状态（0/1…）
                    orFields.add(cb.equal(root.get("status"), (int) asLong));

                    // 薪资值：value 落在 [salaryMin, salaryMax] 内（NULL 视为无界）
                    BigDecimal val = new BigDecimal(t);
                    Expression<BigDecimal> jobMin = root.get("salaryMin");
                    Expression<BigDecimal> jobMax = root.get("salaryMax");
                    Predicate salaryLowerOk = cb.or(cb.isNull(jobMin), cb.lessThanOrEqualTo(jobMin, val));
                    Predicate salaryUpperOk = cb.or(cb.isNull(jobMax), cb.greaterThanOrEqualTo(jobMax, val));
                    orFields.add(cb.and(salaryLowerOk, salaryUpperOk));
                }

                // 用工类型关键词 → employmentType
                // 1=全职 FULL_TIME, 2=实习 INTERN, 3=合同 CONTRACT
                Integer et = mapEmploymentType(t);
                if (et != null) {
                    orFields.add(cb.equal(root.get("employmentType"), et));
                }

                // 日期：如果 token 是可解析日期/时间，则匹配当天/该时间附近
                // 例：2025-10-10 或 2025-10-10T15:00:00
                LocalDateTime[] range = tryParseDateOrDateTime(t);
                if (range != null) {
                    orFields.add(cb.between(root.get("postedAt"), range[0], range[1]));
                }

                andPerToken.add(cb.or(orFields.toArray(new Predicate[0])));
            }

            return cb.and(andPerToken.toArray(new Predicate[0]));
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

    private Integer mapEmploymentType(String t) {
        String s = t.toLowerCase();
        if (List.of("1", "full", "fulltime", "full-time").contains(s)) return 1;
        if (List.of("2", "intern").contains(s)) return 2;
        if (List.of("3", "contract").contains(s)) return 3;
        return null;
    }

    /**
     * 解析日期/时间：
     * - "2025-10-10" -> 当天 [00:00:00, 23:59:59.999999999]
     * - "2025-10-10T15:12:00" -> 这一分钟/秒附近，你也可以放宽到 ±12h
     */
    private LocalDateTime[] tryParseDateOrDateTime(String t) {
        try {
            LocalDate d = LocalDate.parse(t);
            return new LocalDateTime[]{
                    d.atStartOfDay(),
                    d.plusDays(1).atStartOfDay().minusNanos(1)
            };
        } catch (DateTimeParseException ignore) {
        }
        try {
            LocalDateTime dt = LocalDateTime.parse(t);
            // 给 1 分钟窗口（可按需要调整）
            return new LocalDateTime[]{
                    dt.minusMinutes(1),
                    dt.plusMinutes(1)
            };
        } catch (DateTimeParseException ignore) {
        }
        return null;
    }

    public Page<UserDTO> searchUsers(String q, Pageable pageable) {
        // 允许 q 为空：返回全部
        if (q == null || q.isBlank()) {
            return userRepository.findAll(pageable).map(this::toDTO);
        }
        String norm = q.trim();

        Page<User> page = userRepository.findAll(UserSpecs.fuzzySearch(norm), pageable);
        return page.map(this::toDTO);
    }


    private UserDTO toDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setFullName(user.getFullName());
        dto.setRole(user.getRole());
        dto.setIsActive(user.getIsActive());
        return dto;
    }
}

