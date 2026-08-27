package com.devsphere.user.dto.compilation;

import com.devsphere.user.entity.Proficiency;
import com.devsphere.user.entity.Skill;
import com.devsphere.user.entity.SkillCategory;

public class CompiledSkillItemResponse {

    private Long id;
    private String name;
    private SkillCategory category;
    private Proficiency proficiency;
    private Integer yearsOfExperience;
    private Integer displayOrder;

    public CompiledSkillItemResponse() {
    }

    public CompiledSkillItemResponse(Skill skill, Integer displayOrder) {
        this.id = skill.getId();
        this.name = skill.getName();
        this.category = skill.getCategory();
        this.proficiency = skill.getProficiency();
        this.yearsOfExperience = skill.getYearsOfExperience();
        this.displayOrder = displayOrder;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SkillCategory getCategory() {
        return category;
    }

    public void setCategory(SkillCategory category) {
        this.category = category;
    }

    public Proficiency getProficiency() {
        return proficiency;
    }

    public void setProficiency(Proficiency proficiency) {
        this.proficiency = proficiency;
    }

    public Integer getYearsOfExperience() {
        return yearsOfExperience;
    }

    public void setYearsOfExperience(Integer yearsOfExperience) {
        this.yearsOfExperience = yearsOfExperience;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}
