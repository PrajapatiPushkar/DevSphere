package com.devsphere.user.dto.compilation;

import com.devsphere.user.entity.Education;
import java.time.LocalDate;

public class CompiledEducationResponse {

    private Long id;
    private String institutionName;
    private String degree;
    private String fieldOfStudy;
    private String location;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean currentlyStudying;
    private String description;
    private Integer displayOrder;

    public CompiledEducationResponse() {
    }

    public CompiledEducationResponse(Education edu, Integer displayOrder) {
        this.id = edu.getId();
        this.institutionName = edu.getInstitutionName();
        this.degree = edu.getDegree();
        this.fieldOfStudy = edu.getFieldOfStudy();
        this.location = edu.getLocation();
        this.startDate = edu.getStartDate();
        this.endDate = edu.getEndDate();
        this.currentlyStudying = edu.getCurrentlyStudying();
        this.description = edu.getDescription();
        this.displayOrder = displayOrder;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    public String getDegree() {
        return degree;
    }

    public void setDegree(String degree) {
        this.degree = degree;
    }

    public String getFieldOfStudy() {
        return fieldOfStudy;
    }

    public void setFieldOfStudy(String fieldOfStudy) {
        this.fieldOfStudy = fieldOfStudy;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Boolean getCurrentlyStudying() {
        return currentlyStudying;
    }

    public void setCurrentlyStudying(Boolean currentlyStudying) {
        this.currentlyStudying = currentlyStudying;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}
