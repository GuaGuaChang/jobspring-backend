package com.jobspring.jobspringbackend.service;

import com.jobspring.jobspringbackend.dto.HrJobResponse;
import com.jobspring.jobspringbackend.dto.HrJobSearchCriteria;
import com.jobspring.jobspringbackend.entity.Job;
import com.jobspring.jobspringbackend.entity.User;
import com.jobspring.jobspringbackend.repository.JobRepository;
import com.jobspring.jobspringbackend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HrJobService {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Page<HrJobResponse> search(Long hrUserId, HrJobSearchCriteria c, Pageable pageable) {
        // 1) 找到 HR 的公司
        User u = userRepository.findWithCompanyById(hrUserId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        if (u.getCompany() == null) {
            throw new EntityNotFoundException("No company bound for this HR");
        }
        Long companyId = u.getCompany().getId();

        // 2) 构建仅限该公司的筛选条件
        Specification<Job> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> ps = new ArrayList<>();

            // 限定公司
            ps.add(cb.equal(root.get("company").get("id"), companyId));

            if (StringUtils.hasText(c.title())) {
                ps.add(cb.like(cb.lower(root.get("title")), "%" + c.title().toLowerCase() + "%"));
            }
            if (c.status() != null) {
                ps.add(cb.equal(root.get("status"), c.status()));
            }
            if (StringUtils.hasText(c.location())) {
                ps.add(cb.like(cb.lower(root.get("location")), "%" + c.location().toLowerCase() + "%"));
            }
            if (c.employmentType() != null) {
                ps.add(cb.equal(root.get("employmentType"), c.employmentType()));
            }
            if (c.postedFrom() != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("postedAt"), c.postedFrom()));
            }
            if (c.postedTo() != null) {
                ps.add(cb.lessThanOrEqualTo(root.get("postedAt"), c.postedTo()));
            }
            if (StringUtils.hasText(c.keyword())) {
                String kw = "%" + c.keyword().toLowerCase() + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("title")), kw),
                        cb.like(cb.lower(root.get("description")), kw)
                ));
            }
            // 薪资区间重叠
            if (c.salaryMin() != null || c.salaryMax() != null) {
                var jobMin = root.<BigDecimal>get("salaryMin");
                var jobMax = root.<BigDecimal>get("salaryMax");

                var lowerOk = (c.salaryMax() != null)
                        ? cb.or(cb.isNull(jobMin), cb.lessThanOrEqualTo(jobMin, c.salaryMax()))
                        : cb.conjunction();
                var upperOk = (c.salaryMin() != null)
                        ? cb.or(cb.isNull(jobMax), cb.greaterThanOrEqualTo(jobMax, c.salaryMin()))
                        : cb.conjunction();

                ps.add(cb.and(lowerOk, upperOk));
            }

            return cb.and(ps.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return jobRepository.findAll(spec, pageable).map(this::toResp);
    }

    private HrJobResponse toResp(Job j) {
        return new HrJobResponse(
                j.getId(),
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
