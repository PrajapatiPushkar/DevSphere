package com.devsphere.user.dto;

import com.devsphere.user.entity.DeveloperProject;
import com.devsphere.user.entity.ProjectStatus;
import com.devsphere.user.entity.ProjectType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class ProjectResponse {

    private Long id;
    private Long userId;
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
    private Instant createdAt;
    private Instant updatedAt;

    public ProjectResponse() {
    }

    public ProjectResponse(DeveloperProject project) {
        this.id = project.getId();
        this.userId = project.getUserId();
        this.name = project.getName();
        this.description = project.getDescription();
        this.status = project.getStatus();
        this.projectType = project.getProjectType();
        this.repositoryUrl = project.getRepositoryUrl();
        this.liveUrl = project.getLiveUrl();
        this.documentationUrl = project.getDocumentationUrl();
        this.techStack = project.getTechStack();
        this.startDate = project.getStartDate();
        this.targetEndDate = project.getTargetEndDate();
        this.completedAt = project.getCompletedAt();
        this.createdAt = project.getCreatedAt();
        this.updatedAt = project.getUpdatedAt();
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
