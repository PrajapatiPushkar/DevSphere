package com.devsphere.user.dto.compilation;

import com.devsphere.user.entity.DeveloperProject;
import com.devsphere.user.entity.ProjectStatus;
import com.devsphere.user.entity.ProjectType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class CompiledProjectResponse {

    private Long id;
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

    public CompiledProjectResponse() {
    }

    public CompiledProjectResponse(DeveloperProject proj, Integer displayOrder) {
        this.id = proj.getId();
        this.name = proj.getName();
        this.description = proj.getDescription();
        this.status = proj.getStatus();
        this.projectType = proj.getProjectType();
        this.repositoryUrl = proj.getRepositoryUrl();
        this.liveUrl = proj.getLiveUrl();
        this.documentationUrl = proj.getDocumentationUrl();
        this.techStack = proj.getTechStack();
        this.startDate = proj.getStartDate();
        this.targetEndDate = proj.getTargetEndDate();
        this.completedAt = proj.getCompletedAt();
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
