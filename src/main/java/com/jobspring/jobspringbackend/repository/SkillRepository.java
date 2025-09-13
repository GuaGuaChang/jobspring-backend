package com.jobspring.jobspringbackend.repository;

import com.jobspring.jobspringbackend.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {

    // 查询职位所需的技能名称
    @Query("SELECT s.name FROM Skill s " +
            "JOIN JobSkill js ON s.id = js.skill.id " +
            "WHERE js.job.id = :jobId " +
            "ORDER BY js.weight DESC")
    List<String> findSkillNamesByJobId(@Param("jobId") Long jobId);
}