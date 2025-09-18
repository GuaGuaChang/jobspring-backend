package com.jobspring.jobspringbackend.controller;

import com.jobspring.jobspringbackend.dto.SkillDTO;
import com.jobspring.jobspringbackend.service.SkillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/skills")
public class SkillController {

    @Autowired
    private SkillService skillService;

    @GetMapping
    public List<SkillDTO> getAllSkills() {
        return skillService.listAll();
    }

}
