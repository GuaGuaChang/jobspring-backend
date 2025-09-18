package com.jobspring.jobspringbackend.service;

import com.jobspring.jobspringbackend.dto.SkillDTO;
import com.jobspring.jobspringbackend.entity.Skill;
import com.jobspring.jobspringbackend.repository.SkillRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SkillService {
    @Autowired
    private SkillRepository skillRepository;

    public List<SkillDTO> listAll() {
        return skillRepository.findAll()
                .stream()
                .map(this::ConvertToSkillDTO)
                .toList();
    }

    private SkillDTO ConvertToSkillDTO(Skill s) {
        SkillDTO dto = new SkillDTO();
        dto.setId(s.getId());
        dto.setName(s.getName());
        dto.setCategory(s.getCategory());
        return dto;
    }
}
