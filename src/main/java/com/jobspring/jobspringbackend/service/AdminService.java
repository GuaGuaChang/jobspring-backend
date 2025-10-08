package com.jobspring.jobspringbackend.service;

import com.jobspring.jobspringbackend.dto.JobDTO;
import com.jobspring.jobspringbackend.entity.Job;
import com.jobspring.jobspringbackend.entity.User;
import com.jobspring.jobspringbackend.repository.JobRepository;
import com.jobspring.jobspringbackend.repository.SkillRepository;
import com.jobspring.jobspringbackend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminService {
    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private UserRepository userRepository;

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

    //将用户角色从 Candidate(0) 设置为 HR(1)。如果本来就是 HR(1) 或 Admin(2)，会做合理校验并给出错误提示。
    @Transactional
    public void makeHr(Long userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (Boolean.FALSE.equals(u.getIsActive())) {
            throw new IllegalStateException("User is inactive");
        }
        if (u.getRole() == 1) {
            return;
        }
        if (u.getRole() == 2) {
            throw new IllegalArgumentException("Cannot change role of ADMIN");
        }
        if (u.getRole() != 0) {
            throw new IllegalArgumentException("Only candidate can be promoted to HR");
        }

        u.setRole(1);
        userRepository.save(u);
    }
}

