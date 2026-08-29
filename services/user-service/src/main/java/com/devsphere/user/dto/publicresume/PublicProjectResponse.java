package com.devsphere.user.dto.publicresume;

import com.devsphere.user.dto.compilation.CompiledProjectResponse;
import com.devsphere.user.entity.ProjectStatus;
import com.devsphere.user.entity.ProjectType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class PublicProjectResponse {

    private String name;
    private String description;
    private ProjectStatus status;
    private ProjectType projectType;
    private String repositoryUrl;
    private String liveUrl;
    private String documentationUrl;
    private List<String> techStack;
    private LocalDate startDate;
    private LocalDate targetEndDate;
    private Instant completedAt;
    private Integer displayOrder;

    public PublicProjectResponse() {
    }

    public PublicProjectResponse(String name, String description, ProjectStatus status, ProjectType projectType,
                                 String repositoryUrl, String liveUrl, String documentationUrl,
                                 List<String> techStack, LocalDate startDate, LocalDate targetEndDate,
                                 Instant completedAt, Integer displayOrder) {
        this.name = name;
        this.description = description;
        this.status = status;
        this.projectType = projectType;
        this.repositoryUrl = repositoryUrl;
        this.liveUrl = liveUrl;
        this.documentationUrl = documentationUrl;
        this.techStack = techStack;
        this.startDate = startDate;
        this.targetEndDate = targetEndDate;
        this.completedAt = completedAt;
        this.displayOrder = displayOrder;
    }

    public PublicProjectResponse(CompiledProjectResponse compiled) {
        if (compiled != null) {
            this.name = compiled.getName();
            this.description = compiled.getDescription();
            this.status = compiled.getStatus();
            this.projectType = compiled.getProjectType();
            this.repositoryUrl = compiled.getRepositoryUrl();
            this.liveUrl = compiled.getLiveUrl();
            this.documentationUrl = compiled.getDocumentationUrl();
            this.techStack = compiled.getTechStack();
            this.startDate = compiled.getStartDate();
            this.targetEndDate = compiled.getTargetEndDate();
            this.completedAt = compiled.getCompletedAt();
            this.displayOrder = compiled.getDisplayOrder();
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectStatus status) {
        this.status = status;
    }

    public ProjectType getProjectType() {
        return projectType;
    }

    public void setProjectType(ProjectType projectType) {
        this.projectType = projectType;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public String getLiveUrl() {
        return liveUrl;
    }

    public void setLiveUrl(String liveUrl) {
        this.liveUrl = liveUrl;
    }

    public String getDocumentationUrl() {
        return documentationUrl;
    }

    public void setDocumentationUrl(String documentationUrl) {
        this.documentationUrl = documentationUrl;
    }

    public List<String> getTechStack() {
        return techStack;
    }

    public void setTechStack(List<String> techStack) {
        this.techStack = techStack;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getTargetEndDate() {
        return targetEndDate;
    }

    public void setTargetEndDate(LocalDate targetEndDate) {
        this.targetEndDate = targetEndDate;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}
