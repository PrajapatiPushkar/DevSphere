package com.devsphere.user.dto.publicresume;

import com.devsphere.user.dto.compilation.CompiledEducationResponse;
import java.time.LocalDate;

public class PublicEducationResponse {

    private String institutionName;
    private String degree;
    private String fieldOfStudy;
    private String location;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean currentlyStudying;
    private String description;
    private Integer displayOrder;

    public PublicEducationResponse() {
    }

    public PublicEducationResponse(String institutionName, String degree, String fieldOfStudy,
                                   String location, LocalDate startDate, LocalDate endDate,
                                   Boolean currentlyStudying, String description, Integer displayOrder) {
        this.institutionName = institutionName;
        this.degree = degree;
        this.fieldOfStudy = fieldOfStudy;
        this.location = location;
        this.startDate = startDate;
        this.endDate = endDate;
        this.currentlyStudying = currentlyStudying;
        this.description = description;
        this.displayOrder = displayOrder;
    }

    public PublicEducationResponse(CompiledEducationResponse compiled) {
        if (compiled != null) {
            this.institutionName = compiled.getInstitutionName();
            this.degree = compiled.getDegree();
            this.fieldOfStudy = compiled.getFieldOfStudy();
            this.location = compiled.getLocation();
            this.startDate = compiled.getStartDate();
            this.endDate = compiled.getEndDate();
            this.currentlyStudying = compiled.getCurrentlyStudying();
            this.description = compiled.getDescription();
            this.displayOrder = compiled.getDisplayOrder();
        }
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
