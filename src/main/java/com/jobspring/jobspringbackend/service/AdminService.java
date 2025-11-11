package com.jobspring.jobspringbackend.service;


import com.jobspring.jobspringbackend.dto.*;
import com.jobspring.jobspringbackend.entity.Company;

import com.jobspring.jobspringbackend.entity.CompanyMember;
import com.jobspring.jobspringbackend.entity.Job;
import com.jobspring.jobspringbackend.entity.User;
import com.jobspring.jobspringbackend.repository.*;
import com.jobspring.jobspringbackend.exception.BizException;
import com.jobspring.jobspringbackend.exception.ErrorCode;
import com.jobspring.jobspringbackend.repository.CompanyRepository;

import com.jobspring.jobspringbackend.repository.JobRepository;
import com.jobspring.jobspringbackend.repository.SkillRepository;
import com.jobspring.jobspringbackend.repository.UserRepository;
import com.jobspring.jobspringbackend.repository.spec.UserSpecs;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class AdminService {

    private final JobRepository jobRepository;

    private final SkillRepository skillRepository;

    private final UserRepository userRepository;

    private final CompanyRepository companyRepository;

    private final CompanyMemberRepository companyMemberRepository;

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


    private List<String> getJobTags(Long jobId) {
        return skillRepository.findSkillNamesByJobId(jobId);
    }


    @Transactional
    public void makeHr(Long userId, PromoteToHrRequest req) {

        User u = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (Boolean.FALSE.equals(u.getIsActive())) {
            throw new IllegalStateException("User is inactive");
        }
        if (u.getRole() == ROLE_ADMIN) {
            throw new IllegalArgumentException("Cannot change role of ADMIN");
        }
        if (u.getRole() != ROLE_HR && u.getRole() != ROLE_CANDIDATE) {
            throw new IllegalArgumentException("Only candidate can be promoted to HR");
        }


        u.setRole(ROLE_HR);


        Company targetCompany = null;
        boolean overwrite = req == null || req.getOverwriteCompany() == null || req.getOverwriteCompany();

        if (req != null && req.getCompanyId() != null) {
            targetCompany = companyRepository.findById(req.getCompanyId()).orElseThrow(() -> new EntityNotFoundException("Company not found"));
            if (u.getCompany() == null || overwrite) {
                u.setCompany(targetCompany);
            }
        } else if (u.getCompany() != null) {
            targetCompany = u.getCompany();
        }


        if (targetCompany != null) {
            upsertHrMembership(u, targetCompany, overwrite);
        }


        userRepository.save(u);
    }

    private void upsertHrMembership(User user, Company company, boolean overwrite) {

        CompanyMember cm = companyMemberRepository.findFirstByUserIdAndRole(user.getId(), "HR").orElse(null);

        if (cm == null) {

            cm = new CompanyMember();
            cm.setUser(user);
            cm.setCompany(company);
            cm.setRole("HR");
            companyMemberRepository.save(cm);
        } else {

            if (overwrite && !cm.getCompany().getId().equals(company.getId())) {

                cm.setCompany(company);
                companyMemberRepository.save(cm);
            }

        }
    }

    @Transactional(readOnly = true)
    public Page<JobSearchResponse> search(String q, Pageable pageable) {
        Specification<Job> spec = (root, query, cb) -> {

            if (!StringUtils.hasText(q)) return cb.conjunction();


            Join<Object, Object> companyJoin = root.join("company", JoinType.LEFT);


            String[] tokens = Arrays.stream(q.trim().split("\\s+")).filter(StringUtils::hasText).toArray(String[]::new);


            List<Predicate> andPerToken = new ArrayList<>();

            for (String t : tokens) {
                String kw = "%" + t.toLowerCase() + "%";
                List<Predicate> orFields = new ArrayList<>();


                orFields.add(cb.like(cb.lower(root.get("title")), kw));
                orFields.add(cb.like(root.get("description").as(String.class), "%" + t + "%"));
                orFields.add(cb.like(cb.lower(root.get("location")), kw));
                orFields.add(cb.like(cb.lower(companyJoin.get("name")), kw));


                if (t.matches("\\d+")) {
                    long asLong = Long.parseLong(t);
                    orFields.add(cb.equal(root.get("id"), asLong));

                    orFields.add(cb.equal(root.get("status"), (int) asLong));


                    BigDecimal val = new BigDecimal(t);
                    Expression<BigDecimal> jobMin = root.get("salaryMin");
                    Expression<BigDecimal> jobMax = root.get("salaryMax");
                    Predicate salaryLowerOk = cb.or(cb.isNull(jobMin), cb.lessThanOrEqualTo(jobMin, val));
                    Predicate salaryUpperOk = cb.or(cb.isNull(jobMax), cb.greaterThanOrEqualTo(jobMax, val));
                    orFields.add(cb.and(salaryLowerOk, salaryUpperOk));
                }


                Integer et = mapEmploymentType(t);
                if (et != null) {
                    orFields.add(cb.equal(root.get("employmentType"), et));
                }


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
        return new JobSearchResponse(j.getId(), j.getCompany() != null ? j.getCompany().getId() : null, j.getCompany() != null ? j.getCompany().getName() : null, j.getTitle(), j.getLocation(), j.getEmploymentType(), j.getSalaryMin(), j.getSalaryMax(), j.getStatus(), j.getPostedAt());
    }

    private Integer mapEmploymentType(String t) {
        String s = t.toLowerCase();
        if (List.of("1", "full", "fulltime", "full-time").contains(s)) return 1;
        if (List.of("2", "intern").contains(s)) return 2;
        if (List.of("3", "contract").contains(s)) return 3;
        return null;
    }


    private LocalDateTime[] tryParseDateOrDateTime(String t) {
        try {
            LocalDate d = LocalDate.parse(t);
            return new LocalDateTime[]{d.atStartOfDay(), d.plusDays(1).atStartOfDay().minusNanos(1)};
        } catch (DateTimeParseException ignore) {
        }
        try {
            LocalDateTime dt = LocalDateTime.parse(t);

            return new LocalDateTime[]{dt.minusMinutes(1), dt.plusMinutes(1)};
        } catch (DateTimeParseException ignore) {
        }
        return null;
    }

    public Page<UserDTO> searchUsers(String q, Pageable pageable) {

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

