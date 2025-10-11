package com.jobspring.jobspringbackend.controller;

import com.jobspring.jobspringbackend.dto.ApplicationDTO;
import com.jobspring.jobspringbackend.dto.ApplicationDetailResponse;
import com.jobspring.jobspringbackend.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;


    @PostMapping(value = "/{jobId}/applications", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> apply(@PathVariable Long jobId, @ModelAttribute ApplicationDTO form,
                                      @RequestParam(value = "file", required = false) MultipartFile file, Authentication auth) {
        Long userId = Long.valueOf(auth.getName());
        Long id = applicationService.apply(jobId, userId, form, file);
        return ResponseEntity.created(URI.create("/api/applications/" + id)).build();
    }

    // 查看单个申请详情
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    @GetMapping("/{applicationId}")
    public ResponseEntity<ApplicationDetailResponse> getApplicationDetail(
            @PathVariable Long applicationId,
            @RequestParam(required = false) Long companyId,
            org.springframework.security.core.Authentication auth
    ) {
        // 如果你把“用户ID”放进了 auth.getName()（比如之前用 "200" 这种），可直接解析：
        Long userId = Long.valueOf(auth.getName());

      /*  // 如果 auth.getName() 存的是邮箱/用户名，就查一次用户拿到 id：
        var user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));
        Long userId = user.getId();*/

        var resp = applicationService.getApplicationDetail(userId, companyId, applicationId);
        return ResponseEntity.ok(resp);
    }
}