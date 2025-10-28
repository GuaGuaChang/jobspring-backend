package com.jobspring.jobspringbackend.service;

import com.jobspring.jobspringbackend.dto.SkillDTO;
import com.jobspring.jobspringbackend.entity.Skill;
import com.jobspring.jobspringbackend.repository.SkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkillServiceTest {

    @Mock
    private SkillRepository skillRepository;

    @InjectMocks
    private SkillService service;

    private Skill skill1;
    private Skill skill2;

    @BeforeEach
    void setup() {
        skill1 = new Skill();
        skill1.setId(1L);
        skill1.setName("Java");
        skill1.setCategory("Programming");

        skill2 = new Skill();
        skill2.setId(2L);
        skill2.setName("Docker");
        skill2.setCategory("DevOps");
    }

    // ✅ 正常情况
    @Test
    void listAll_shouldReturnSkillDTOs() {
        when(skillRepository.findAll()).thenReturn(List.of(skill1, skill2));

        List<SkillDTO> result = service.listAll();

        assertEquals(2, result.size());
        assertEquals("Java", result.get(0).getName());
        assertEquals("Programming", result.get(0).getCategory());
        assertEquals("Docker", result.get(1).getName());
        assertEquals("DevOps", result.get(1).getCategory());

        verify(skillRepository, times(1)).findAll();
    }

    // ✅ 空列表情况
    @Test
    void listAll_shouldReturnEmptyList_whenNoSkillsFound() {
        when(skillRepository.findAll()).thenReturn(List.of());

        List<SkillDTO> result = service.listAll();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(skillRepository, times(1)).findAll();
    }
}
