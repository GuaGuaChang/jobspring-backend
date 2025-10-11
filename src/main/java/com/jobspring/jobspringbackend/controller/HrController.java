package com.jobspring.jobspringbackend.controller;

import com.jobspring.jobspringbackend.dto.ApplicationBriefResponse;
import com.jobspring.jobspringbackend.service.HrApplicationService;
import com.jobspring.jobspringbackend.service.HrCompanyService;
import com.jobspring.jobspringbackend.service.HrJobService;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.jobspring.jobspringbackend.dto.HrJobResponse;
import com.jobspring.jobspringbackend.dto.HrJobSearchCriteria;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;


@RestController
@RequestMapping("/api/hr")
@RequiredArgsConstructor
public class HrController {

    private final HrApplicationService hrApplicationService;

    private final HrCompanyService hrCompanyService;

    private final HrJobService hrJobService;

    // 方式一：自动取 HR 自己的公司
    @PreAuthorize("hasRole('HR')")
    @GetMapping("/applications")
    public ResponseEntity<Page<ApplicationBriefResponse>> listMine(
            @RequestParam(required = false) Long jobId,
            @RequestParam(required = false) Integer status,
            Pageable pageable,
            Authentication auth
    ) {
        Long hrUserId = Long.parseLong(auth.getName());
        Page<ApplicationBriefResponse> page = hrApplicationService
                .listCompanyApplications(hrUserId, null, jobId, status, pageable);
        return ResponseEntity.ok(page);
    }

   /* // 方式二：显式指定公司（会做归属校验）
    @PreAuthorize("hasRole('HR')")
    @GetMapping("/companies/{companyId}/applications")
    public ResponseEntity<Page<ApplicationBriefResponse>> listByCompany(
            @PathVariable Long companyId,
            @RequestParam(required = false) Long jobId,
            @RequestParam(required = false) Integer status,
            Pageable pageable,
            Authentication auth
    ) {
        Long hrUserId = Long.parseLong(auth.getName());
        Page<ApplicationBriefResponse> page = hrApplicationService
                .listCompanyApplications(hrUserId, companyId, jobId, status, pageable);
        return ResponseEntity.ok(page);
    }*/

    // 获取当前 HR 所在公司 ID
    @GetMapping("/company-id")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<Map<String, Long>> myCompanyId(Authentication auth) {
        Long userId = Long.valueOf(auth.getName());
        Long companyId = hrCompanyService.getCompanyIdOfHr(userId);
        return ResponseEntity.ok(Map.of("companyId", companyId));
    }

    //更新某个申请状态
    @PatchMapping("/applications/{applicationId}/status")
    @PreAuthorize("hasAnyRole('HR')")
    public ResponseEntity<ApplicationBriefResponse> updateStatus(
            @PathVariable Long applicationId,
            @RequestBody UpdateStatusBody body,
            Authentication auth) {

        Long hrUserId = Long.valueOf(auth.getName());
        ApplicationBriefResponse res =
                hrApplicationService.updateStatus(hrUserId, applicationId, body.getStatus());
        return ResponseEntity.ok(res);
    }

    @Data
    public static class UpdateStatusBody {
        @NotNull
        private Integer status;
    }

    // HR 获取自己公司的名字（仅名称）
    @PreAuthorize("hasRole('HR')")
    @GetMapping("/company-name")
    public ResponseEntity<String> myCompanyName(Authentication auth) {
        Long userId = Long.valueOf(auth.getName());
        String name = hrCompanyService.getMyCompanyName(userId);
        return ResponseEntity.ok(name);
    }

    // HR 搜索自己公司内的岗位（分页 + 排序）
    @PreAuthorize("hasRole('HR')")
    @GetMapping("/jobs")
    public ResponseEntity<Page<HrJobResponse>> search(
            Authentication auth,
            @RequestParam(required = false) String q,
            Pageable pageable
    ) {
        Long userId = Long.valueOf(auth.getName());
        return ResponseEntity.ok(hrJobService.search(userId, q, pageable));
    }
}