package com.jobspring.jobspringbackend.service;

import com.jobspring.jobspringbackend.dto.ApplicationDTO;
import com.jobspring.jobspringbackend.dto.ApplicationDetailResponse;
import com.jobspring.jobspringbackend.entity.Application;
import com.jobspring.jobspringbackend.entity.Job;
import com.jobspring.jobspringbackend.entity.User;
import com.jobspring.jobspringbackend.repository.ApplicationRepository;
import com.jobspring.jobspringbackend.repository.JobRepository;
import com.jobspring.jobspringbackend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Optional;

// ApplicationService
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final JobRepository jobRepo;
    private final UserRepository userRepo;
    private final ApplicationRepository appRepo;
    @Value("${app.upload.dir:/tmp/uploads}")
    private String uploadDir;        // 物理目录
    @Value("${app.upload.public-base:/uploads}")
    private String publicBase;       // 对外 URL 前缀（由静态资源映射或 Nginx 提供）

    private final ApplicationRepository applicationRepository;
    private final HrCompanyService hrCompanyService;
    private final UserRepository userRepository;

    @Transactional
    public Long apply(Long jobId, Long userId, ApplicationDTO form, MultipartFile file) {
        Job job = jobRepo.findById(jobId).orElseThrow(() -> new EntityNotFoundException("Job not found"));
        if (job.getStatus() != 0) {
            throw new IllegalStateException("Job inactive");
        }

        User user = userRepo.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));
        if (appRepo.existsByJobAndUser(job, user)) {
            throw new IllegalArgumentException("Already applied");
        }

        Application app = new Application();
        app.setJob(job);
        app.setUser(user);
        app.setStatus(0);
        app.setAppliedAt(LocalDateTime.now());
        app.setResumeProfile(form.getResumeProfile());

        if (user.getProfile() != null) {
            app.setProfile(user.getProfile());
        }

        // 优先：用户 Profile 的简历 URL
        String resumeUrl = null;
        if (user.getProfile() != null && user.getProfile().getFileUrl() != null) {
            resumeUrl = user.getProfile().getFileUrl();
        } else if (file != null && !file.isEmpty()) {
            // 次选：表单上传的文件
            validateFile(file);
            resumeUrl = saveToLocal(file, "applications/" + jobId + "/" + userId);
        }

        app.setResumeUrl(resumeUrl);

        appRepo.save(app);
        return app.getId();
    }

    private String saveToLocal(MultipartFile file, String keyPrefix) {
        try {
            String safePrefix = keyPrefix.replaceAll("[^a-zA-Z0-9/_-]", "_");
            String filename = System.currentTimeMillis() + "_" +
                    (file.getOriginalFilename() == null ? "file"
                            : java.nio.file.Path.of(file.getOriginalFilename()).getFileName().toString());

            java.nio.file.Path root = java.nio.file.Paths.get(uploadDir, safePrefix).normalize();
            java.nio.file.Files.createDirectories(root);
            java.nio.file.Path target = root.resolve(filename).normalize();

            file.transferTo(target);

            String urlPath = String.join("/", publicBase.replaceAll("/+$", ""),
                    safePrefix.replaceAll("^/+", "").replaceAll("/+$", ""), filename);
            return urlPath.startsWith("/") ? urlPath : "/" + urlPath;
        } catch (java.io.IOException e) {
            throw new IllegalStateException("File upload failed", e); // 运行时异常
        }
    }

    private void validateFile(MultipartFile f) {
        if (f.getSize() > 10 * 1024 * 1024) throw new IllegalArgumentException("File too large");
        String ct = Optional.ofNullable(f.getContentType()).orElse("");
        if (!(ct.equals("application/pdf") || ct.equals("application/zip")
                || ct.startsWith("image/") || ct.equals("application/msword")
                || ct.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))) {
            throw new IllegalArgumentException("Unsupported file type");
        }
    }

    public ApplicationDetailResponse getApplicationDetail(Long hrUserId, Long companyId, Long applicationId) {
        // 推断或校验公司 ID
        Long effectiveCompanyId = (companyId == null)
                ? hrCompanyService.findCompanyIdByUserId(hrUserId)
                : validateAndReturn(hrUserId, companyId);

        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Application not found"));

        // 校验这份申请属于该 HR 的公司
        Long jobCompanyId = app.getJob().getCompany().getId();
        if (!jobCompanyId.equals(effectiveCompanyId)) {
            throw new SecurityException("You are not allowed to access this application");
        }

        return toDetail(app);
    }

    private Long validateAndReturn(Long userId, Long companyId) {
        // 查询用户角色
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        // 如果是 Admin，则直接返回，不做 HR 公司归属校验
        if (user.getRole() == 2) {  // 2 = Admin
            return companyId;
        }

        // 否则正常 HR 校验
        hrCompanyService.assertHrInCompany(userId, companyId);
        return companyId;
    }

    private ApplicationDetailResponse toDetail(Application a) {
        ApplicationDetailResponse r = new ApplicationDetailResponse();
        r.setId(a.getId());
        r.setJobId(a.getJob().getId());
        r.setJobTitle(a.getJob().getTitle());
        r.setApplicantId(a.getUser().getId());
        r.setApplicantName(a.getUser().getFullName());
        r.setApplicantEmail(a.getUser().getEmail());
        r.setStatus(a.getStatus());
        r.setAppliedAt(a.getAppliedAt());
        r.setResumeUrl(a.getResumeUrl());
        r.setResumeProfile(a.getResumeProfile());
        return r;
    }
}
