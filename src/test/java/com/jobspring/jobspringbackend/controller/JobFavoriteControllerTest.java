package com.jobspring.jobspringbackend.controller;

import com.jobspring.jobspringbackend.dto.FavoriteJobResponse;
import com.jobspring.jobspringbackend.service.JobFavoriteService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(JobFavoriteController.class)
class JobFavoriteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JobFavoriteService favoriteService;

    @Test
    @WithMockUser(username = "11", roles = "CANDIDATE")
    void testAddFavorite() throws Exception {
        mockMvc.perform(post("/api/job_favorites/{jobId}", 99L)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        Mockito.verify(favoriteService).add(11L, 99L);
    }

    @Test
    @WithMockUser(username = "11", roles = "CANDIDATE")
    void testRemoveFavorite() throws Exception {
        mockMvc.perform(delete("/api/job_favorites/{jobId}", 99L)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        Mockito.verify(favoriteService).remove(11L, 99L);
    }

    @Test
    @WithMockUser(username = "11", roles = "CANDIDATE")
    void testIsFavorited() throws Exception {
        Mockito.when(favoriteService.isFavorited(11L, 99L)).thenReturn(true);

        mockMvc.perform(get("/api/job_favorites/{jobId}/is-favorited", 99L))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @WithMockUser(username = "11", roles = "CANDIDATE")
    void testListFavorites() throws Exception {
        FavoriteJobResponse job = new FavoriteJobResponse();
        Page<FavoriteJobResponse> page = new PageImpl<>(List.of(job));

        Mockito.when(favoriteService.list(eq(11L), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/job_favorites"))
                .andExpect(status().isOk());
    }
}