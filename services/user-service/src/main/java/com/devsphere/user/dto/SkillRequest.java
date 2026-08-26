package com.devsphere.user.dto;

import com.devsphere.user.entity.Proficiency;
import com.devsphere.user.entity.SkillCategory;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class SkillRequest {

    @NotBlank(message = "name is required")
    @Size(max = 100, message = "name cannot exceed 100 characters")
    private String name;

    @NotNull(message = "category is required")
    private SkillCategory category;

    @NotNull(message = "proficiency is required")
    private Proficiency proficiency;

    @Min(value = 0, message = "yearsOfExperience must be zero or positive")
    @Max(value = 70, message = "yearsOfExperience must be realistic (max 70)")
    private Integer yearsOfExperience;

    @Min(value = 0, message = "displayOrder must be zero or positive")
    private Integer displayOrder;

    public SkillRequest() {
    }

    public SkillRequest(String name, SkillCategory category, Proficiency proficiency, Integer yearsOfExperience, Integer displayOrder) {
        this.name = name;
        this.category = category;
        this.proficiency = proficiency;
        this.yearsOfExperience = yearsOfExperience;
        this.displayOrder = displayOrder;
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
