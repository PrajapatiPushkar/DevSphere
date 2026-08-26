package com.devsphere.user.dto;

import com.devsphere.user.entity.Proficiency;
import com.devsphere.user.entity.Skill;
import com.devsphere.user.entity.SkillCategory;
import java.time.Instant;

public class SkillResponse {

    private Long id;
    private Long userId;
    private String name;
    private SkillCategory category;
    private Proficiency proficiency;
    private Integer yearsOfExperience;
    private Integer displayOrder;
    private Instant createdAt;
    private Instant updatedAt;

    public SkillResponse() {
    }

    public SkillResponse(Skill skill) {
        this.id = skill.getId();
        this.userId = skill.getUserId();
        this.name = skill.getName();
        this.category = skill.getCategory();
        this.proficiency = skill.getProficiency();
        this.yearsOfExperience = skill.getYearsOfExperience();
        this.displayOrder = skill.getDisplayOrder();
        this.createdAt = skill.getCreatedAt();
        this.updatedAt = skill.getUpdatedAt();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
