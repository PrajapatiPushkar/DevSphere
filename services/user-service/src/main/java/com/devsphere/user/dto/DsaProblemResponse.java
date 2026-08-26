package com.devsphere.user.dto;

import com.devsphere.user.entity.DsaDifficulty;
import com.devsphere.user.entity.DsaPlatform;
import com.devsphere.user.entity.DsaProblem;
import com.devsphere.user.entity.DsaProblemStatus;
import com.devsphere.user.entity.DsaTopic;
import java.time.Instant;

public class DsaProblemResponse {

    private Long id;
    private Long userId;
    private Long taskId;
    private Long goalId;
    private String title;
    private String description;
    private DsaPlatform platform;
    private String problemUrl;
    private DsaDifficulty difficulty;
    private DsaTopic topic;
    private DsaProblemStatus status;
    private Instant solvedAt;
    private Integer timeSpentMinutes;
    private Integer attemptCount;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;

    public DsaProblemResponse() {
    }

    public DsaProblemResponse(DsaProblem problem) {
        this.id = problem.getId();
        this.userId = problem.getUserId();
        this.taskId = problem.getTaskId();
        this.goalId = problem.getGoalId();
        this.title = problem.getTitle();
        this.description = problem.getDescription();
        this.platform = problem.getPlatform();
        this.problemUrl = problem.getProblemUrl();
        this.difficulty = problem.getDifficulty();
        this.topic = problem.getTopic();
        this.status = problem.getStatus();
        this.solvedAt = problem.getSolvedAt();
        this.timeSpentMinutes = problem.getTimeSpentMinutes();
        this.attemptCount = problem.getAttemptCount();
        this.notes = problem.getNotes();
        this.createdAt = problem.getCreatedAt();
        this.updatedAt = problem.getUpdatedAt();
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

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getGoalId() {
        return goalId;
    }

    public void setGoalId(Long goalId) {
        this.goalId = goalId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public DsaPlatform getPlatform() {
        return platform;
    }

    public void setPlatform(DsaPlatform platform) {
        this.platform = platform;
    }

    public String getProblemUrl() {
        return problemUrl;
    }

    public void setProblemUrl(String problemUrl) {
        this.problemUrl = problemUrl;
    }

    public DsaDifficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(DsaDifficulty difficulty) {
        this.difficulty = difficulty;
    }

    public DsaTopic getTopic() {
        return topic;
    }

    public void setTopic(DsaTopic topic) {
        this.topic = topic;
    }

    public DsaProblemStatus getStatus() {
        return status;
    }

    public void setStatus(DsaProblemStatus status) {
        this.status = status;
    }

    public Instant getSolvedAt() {
        return solvedAt;
    }

    public void setSolvedAt(Instant solvedAt) {
        this.solvedAt = solvedAt;
    }

    public Integer getTimeSpentMinutes() {
        return timeSpentMinutes;
    }

    public void setTimeSpentMinutes(Integer timeSpentMinutes) {
        this.timeSpentMinutes = timeSpentMinutes;
    }

    public Integer getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(Integer attemptCount) {
        this.attemptCount = attemptCount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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
