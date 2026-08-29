package com.devsphere.user.dto;

import com.devsphere.user.dto.compilation.CompiledResumeResponse;
import com.devsphere.user.entity.ResumeVersion;
import com.devsphere.user.entity.ResumeVersionStatus;
import java.time.Instant;

public class ResumeVersionResponse {

    private Long id;
    private Long resumeProfileId;
    private Long userId;
    private Integer versionNumber;
    private String name;
    private ResumeVersionStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant publishedAt;
    private Instant archivedAt;
    private CompiledResumeResponse snapshot;

    public ResumeVersionResponse() {
    }

    public ResumeVersionResponse(ResumeVersion version) {
        this(version, null);
    }

    public ResumeVersionResponse(ResumeVersion version, CompiledResumeResponse snapshot) {
        this.id = version.getId();
        this.resumeProfileId = version.getResumeProfileId();
        this.userId = version.getUserId();
        this.versionNumber = version.getVersionNumber();
        this.name = version.getName();
        this.status = version.getStatus();
        this.createdAt = version.getCreatedAt();
        this.updatedAt = version.getUpdatedAt();
        this.publishedAt = version.getPublishedAt();
        this.archivedAt = version.getArchivedAt();
        this.snapshot = snapshot;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getResumeProfileId() {
        return resumeProfileId;
    }

    public void setResumeProfileId(Long resumeProfileId) {
        this.resumeProfileId = resumeProfileId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ResumeVersionStatus getStatus() {
        return status;
    }

    public void setStatus(ResumeVersionStatus status) {
        this.status = status;
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

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(Instant archivedAt) {
        this.archivedAt = archivedAt;
    }

    public CompiledResumeResponse getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(CompiledResumeResponse snapshot) {
        this.snapshot = snapshot;
    }
}
