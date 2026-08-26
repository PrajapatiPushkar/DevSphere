package com.devsphere.user.dto;

import com.devsphere.user.entity.DsaDifficulty;
import com.devsphere.user.entity.DsaPlatform;
import com.devsphere.user.entity.DsaTopic;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdateDsaProblemRequest {

    @NotBlank(message = "title is required")
    @Size(max = 255, message = "title cannot exceed 255 characters")
    private String title;

    private String description;

    @NotNull(message = "platform is required")
    private DsaPlatform platform;

    @Size(max = 512, message = "problemUrl cannot exceed 512 characters")
    private String problemUrl;

    @NotNull(message = "difficulty is required")
    private DsaDifficulty difficulty;

    @NotNull(message = "topic is required")
    private DsaTopic topic;

    @Min(value = 0, message = "timeSpentMinutes must be zero or positive")
    private Integer timeSpentMinutes;

    private String notes;

    private Long taskId;

    private Long goalId;

    public UpdateDsaProblemRequest() {
    }

    public UpdateDsaProblemRequest(String title, String description, DsaPlatform platform, String problemUrl, DsaDifficulty difficulty, DsaTopic topic, Integer timeSpentMinutes, String notes, Long taskId, Long goalId) {
        this.title = title;
        this.description = description;
        this.platform = platform;
        this.problemUrl = problemUrl;
        this.difficulty = difficulty;
        this.topic = topic;
        this.timeSpentMinutes = timeSpentMinutes;
        this.notes = notes;
        this.taskId = taskId;
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

    public Integer getTimeSpentMinutes() {
        return timeSpentMinutes;
    }

    public void setTimeSpentMinutes(Integer timeSpentMinutes) {
        this.timeSpentMinutes = timeSpentMinutes;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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
}
