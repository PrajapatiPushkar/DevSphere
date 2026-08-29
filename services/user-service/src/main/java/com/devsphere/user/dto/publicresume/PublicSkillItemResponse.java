package com.devsphere.user.dto.publicresume;

import com.devsphere.user.dto.compilation.CompiledSkillItemResponse;
import com.devsphere.user.entity.Proficiency;
import com.devsphere.user.entity.SkillCategory;

public class PublicSkillItemResponse {

    private String name;
    private SkillCategory category;
    private Proficiency proficiency;
    private Integer yearsOfExperience;
    private Integer displayOrder;

    public PublicSkillItemResponse() {
    }

    public PublicSkillItemResponse(String name, SkillCategory category, Proficiency proficiency, Integer yearsOfExperience, Integer displayOrder) {
        this.name = name;
        this.category = category;
        this.proficiency = proficiency;
        this.yearsOfExperience = yearsOfExperience;
        this.displayOrder = displayOrder;
    }

    public PublicSkillItemResponse(CompiledSkillItemResponse compiled) {
        if (compiled != null) {
            this.name = compiled.getName();
            this.category = compiled.getCategory();
            this.proficiency = compiled.getProficiency();
            this.yearsOfExperience = compiled.getYearsOfExperience();
            this.displayOrder = compiled.getDisplayOrder();
        }
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
