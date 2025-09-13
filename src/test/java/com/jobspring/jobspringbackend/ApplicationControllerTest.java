package com.jobspring.jobspringbackend;

import com.jobspring.jobspringbackend.controller.ApplicationController;
import com.jobspring.jobspringbackend.dto.ApplicationDTO;
import com.jobspring.jobspringbackend.service.ApplicationService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ApplicationController.class)
@AutoConfigureMockMvc   // 如遇 401，可改成 @AutoConfigureMockMvc(addFilters = false)
class ApplicationControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean                 // ✅ 由 Boot 注入到测试上下文的 Mock，不要自己写构造器
    private ApplicationService applicationService;

    @Test
    @WithMockUser(username = "200") // Authentication.getName() = "200"
    void apply_withFile_returns201AndLocation() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cv.pdf", "application/pdf", "dummy".getBytes());

        Mockito.when(applicationService.apply(eq(123L), eq(200L),
                        any(ApplicationDTO.class), any()))
                .thenReturn(555L);

        mvc.perform(
                        multipart("/api/applications/{jobId}/applications", 123L)
                                .file(file)
                                .param("resumeProfile", "I am a good fit.")
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                )
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/applications/555"));
    }

    @Test
    @WithMockUser(username = "200")
    void apply_withoutFile_returns201() throws Exception {
        Mockito.when(applicationService.apply(eq(321L), eq(200L),
                        any(ApplicationDTO.class), isNull()))
                .thenReturn(777L);

        mvc.perform(
                        multipart("/api/applications/{jobId}/applications", 321L)
                                .param("resumeProfile", "Only text content")
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                )
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/applications/777"));
    }
}
