package com.jobspring.jobspringbackend.service;

import com.jobspring.jobspringbackend.dto.HrJobResponse;
import com.jobspring.jobspringbackend.dto.HrJobSearchCriteria;
import com.jobspring.jobspringbackend.dto.JobResponse;
import com.jobspring.jobspringbackend.entity.Job;
import com.jobspring.jobspringbackend.entity.User;
import com.jobspring.jobspringbackend.exception.NotFoundException;
import com.jobspring.jobspringbackend.repository.CompanyMemberRepository;
import com.jobspring.jobspringbackend.repository.JobRepository;
import com.jobspring.jobspringbackend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HrJobService {

    private final JobRepository jobRepository;

    private final UserRepository userRepository;

    private final CompanyMemberRepository companyMemberRepository;

    @Transactional(readOnly = true)
    public Page<HrJobResponse> search(Long hrUserId, String q, Pageable pageable) {

        User u = userRepository.findWithCompanyById(hrUserId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        if (u.getCompany() == null) {
            throw new EntityNotFoundException("No company bound for this HR");
        }
        Long companyId = u.getCompany().getId();


        Specification<Job> spec = (root, query, cb) -> {
            List<Predicate> all = new ArrayList<>();

            all.add(cb.equal(root.get("company").get("id"), companyId));

            if (StringUtils.hasText(q)) {
                Join<Object, Object> companyJoin = root.join("company", JoinType.LEFT);
                String[] tokens = Arrays.stream(q.trim().split("\\s+"))
                        .filter(StringUtils::hasText)
                        .toArray(String[]::new);

                List<Predicate> andPerToken = new ArrayList<>();

                for (String t : tokens) {
                    String kwLower = "%" + t.toLowerCase() + "%";
                    List<Predicate> orFields = new ArrayList<>();


                    orFields.add(cb.like(cb.lower(root.get("title")), kwLower));
                    orFields.add(cb.like(root.get("description").as(String.class), "%" + t + "%")); // 修法2
                    orFields.add(cb.like(cb.lower(root.get("location")), kwLower));
                    orFields.add(cb.like(cb.lower(companyJoin.get("name")), kwLower));


                    if (t.matches("\\d+")) {
                        long asLong = Long.parseLong(t);
                        orFields.add(cb.equal(root.get("id"), asLong));
                        orFields.add(cb.equal(root.get("status"), (int) asLong));

                        BigDecimal val = new BigDecimal(t);
                        var jobMin = root.<BigDecimal>get("salaryMin");
                        var jobMax = root.<BigDecimal>get("salaryMax");
                        var lowerOk = cb.or(cb.isNull(jobMin), cb.lessThanOrEqualTo(jobMin, val));
                        var upperOk = cb.or(cb.isNull(jobMax), cb.greaterThanOrEqualTo(jobMax, val));
                        orFields.add(cb.and(lowerOk, upperOk));
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

                all.add(cb.and(andPerToken.toArray(new Predicate[0])));
            }

            return cb.and(all.toArray(new Predicate[0]));
        };

        return jobRepository.findAll(spec, pageable).map(j ->
                new HrJobResponse(
                        j.getId(),
                        j.getTitle(),
                        j.getLocation(),
                        j.getEmploymentType(),
                        j.getSalaryMin(),
                        j.getSalaryMax(),
                        j.getStatus(),
                        j.getPostedAt()
                )
        );
    }

    private Integer mapEmploymentType(String t) {
        String s = t.toLowerCase();
        if (List.of("1", "full", "fulltime", "full-time", "全职").contains(s)) return 1;
        if (List.of("2", "intern", "实习", "实习生").contains(s)) return 2;
        if (List.of("3", "contract", "合同", "合同工", "兼职合同").contains(s)) return 3;
        return null;
    }


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
            return new LocalDateTime[]{dt.minusMinutes(1), dt.plusMinutes(1)};
        } catch (DateTimeParseException ignore) {
        }
        return null;
    }


    public JobResponse getJobForEdit(Long companyId, Long jobId) {
        Job job = jobRepository.findByIdAndCompanyId(jobId, companyId)
                .orElseThrow(() -> new NotFoundException("Job not found or not under your company"));


        JobResponse r = new JobResponse();
        r.setId(job.getId());
        r.setCompanyId(job.getCompany().getId()); //  job.getCompanyId()
        r.setTitle(job.getTitle());
        r.setEmploymentType(job.getEmploymentType());
        r.setSalaryMin(job.getSalaryMin());
        r.setSalaryMax(job.getSalaryMax());
        r.setLocation(job.getLocation());
        r.setDescription(job.getDescription());
        r.setStatus(job.getStatus());
        r.setPostedAt(job.getPostedAt());
        return r;
    }

    public Long findCompanyIdByUserId(Long userId) {
        return companyMemberRepository.findCompanyIdByHrUserId(userId)
                .orElseThrow(() -> new NotFoundException("HR is not bound to any company"));
    }
}
