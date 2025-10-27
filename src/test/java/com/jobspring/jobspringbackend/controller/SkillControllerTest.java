package com.jobspring.jobspringbackend.controller;

import com.jobspring.jobspringbackend.dto.SkillDTO;
import com.jobspring.jobspringbackend.service.SkillService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SkillController.class)
class SkillControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SkillService skillService;

    @Test
    @WithMockUser(username = "user", roles = "CANDIDATE")
    void testGetAllSkills() throws Exception {
        SkillDTO skill1 = new SkillDTO();
        skill1.setId(1L);
        skill1.setName("Java");
        skill1.setCategory("Programming");

        SkillDTO skill2 = new SkillDTO();
        skill2.setId(2L);
        skill2.setName("Excel");
        skill2.setCategory("Office");

        Mockito.when(skillService.listAll()).thenReturn(List.of(skill1, skill2));

        mockMvc.perform(get("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Java"))
                .andExpect(jsonPath("$[0].category").value("Programming"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Excel"))
                .andExpect(jsonPath("$[1].category").value("Office"));
    }

    @Test
    @WithMockUser(username = "user", roles = "CANDIDATE")
    void testGetAllSkills_EmptyList() throws Exception {
        Mockito.when(skillService.listAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }
}