package com.jobspring.jobspringbackend.controller;

import com.jobspring.jobspringbackend.dto.ApplicationDTO;
import com.jobspring.jobspringbackend.service.ApplicationService;
import lombok.RequiredArgsConstructor;
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
//  @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<Void> apply(@PathVariable Long jobId, @ModelAttribute ApplicationDTO form,
                                          @RequestPart(name = "file", required = false) MultipartFile file, Authentication auth)
    {

            Long userId = Long.valueOf(auth.getName());
            Long id = applicationService.apply(jobId, userId, form, file);
            return ResponseEntity.created(URI.create("/api/applications/" + id)).build();
    }

}
