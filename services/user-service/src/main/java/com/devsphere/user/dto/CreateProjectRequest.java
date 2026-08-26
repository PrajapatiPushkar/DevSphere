package com.devsphere.user.dto;

import com.devsphere.user.entity.ProjectType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public class CreateProjectRequest {

    @NotBlank(message = "name is required")
    @Size(max = 255, message = "name cannot exceed 255 characters")
    private String name;

    @Size(max = 2000, message = "description cannot exceed 2000 characters")
    private String description;

    @NotNull(message = "projectType is required")
    private ProjectType projectType;

    @Pattern(regexp = "^(https?://.+)?$", message = "repositoryUrl must be a valid HTTP or HTTPS URL")
    @Size(max = 512, message = "repositoryUrl cannot exceed 512 characters")
    private String repositoryUrl;

    @Pattern(regexp = "^(https?://.+)?$", message = "liveUrl must be a valid HTTP or HTTPS URL")
    @Size(max = 512, message = "liveUrl cannot exceed 512 characters")
    private String liveUrl;

    @Pattern(regexp = "^(https?://.+)?$", message = "documentationUrl must be a valid HTTP or HTTPS URL")
    @Size(max = 512, message = "documentationUrl cannot exceed 512 characters")
    private String documentationUrl;

    private List<String> techStack;

    private LocalDate startDate;

    private LocalDate targetEndDate;

    public CreateProjectRequest() {
    }

    public CreateProjectRequest(String name, String description, ProjectType projectType, String repositoryUrl, String liveUrl, String documentationUrl, List<String> techStack, LocalDate startDate, LocalDate targetEndDate) {
        this.name = name;
        this.description = description;
        this.projectType = projectType;
        this.repositoryUrl = repositoryUrl;
        this.liveUrl = liveUrl;
        this.documentationUrl = documentationUrl;
        this.techStack = techStack;
        this.startDate = startDate;
        this.targetEndDate = targetEndDate;
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
}
