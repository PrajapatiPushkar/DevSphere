package com.devsphere.user.dto;

import com.devsphere.user.entity.Goal;
import com.devsphere.user.entity.GoalStatus;
import com.devsphere.user.entity.GoalType;
import java.time.Instant;
import java.time.LocalDate;

public class GoalResponse {

    private Long id;
    private Long userId;
    private String title;
    private String description;
    private GoalType goalType;
    private GoalStatus status;
    private Integer targetValue;
    private Integer currentValue;
    private Double progressPercentage;
    private LocalDate targetDate;
    private Instant completedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public GoalResponse() {
    }

    public GoalResponse(Long id, Long userId, String title, String description, GoalType goalType, GoalStatus status, Integer targetValue, Integer currentValue, LocalDate targetDate, Instant completedAt, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.goalType = goalType;
        this.status = status;
        this.targetValue = targetValue;
        this.currentValue = currentValue;
        this.progressPercentage = calculateProgressPercentage(targetValue, currentValue);
        this.targetDate = targetDate;
        this.completedAt = completedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static GoalResponse fromEntity(Goal goal) {
        if (goal == null) {
            return null;
        }
        return new GoalResponse(
                goal.getId(),
                goal.getUserId(),
                goal.getTitle(),
                goal.getDescription(),
                goal.getGoalType(),
                goal.getStatus(),
                goal.getTargetValue(),
                goal.getCurrentValue(),
                goal.getTargetDate(),
                goal.getCompletedAt(),
                goal.getCreatedAt(),
                goal.getUpdatedAt()
        );
    }

    public static Double calculateProgressPercentage(Integer targetValue, Integer currentValue) {
        if (targetValue == null || targetValue <= 0) {
            return null;
        }
        int current = currentValue != null ? currentValue : 0;
        if (current <= 0) {
            return 0.0;
        }
        if (current >= targetValue) {
            return 100.0;
        }
        double percentage = (current * 100.0) / targetValue;
        return Math.round(percentage * 100.0) / 100.0;
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

    public GoalType getGoalType() {
        return goalType;
    }

    public void setGoalType(GoalType goalType) {
        this.goalType = goalType;
    }

    public GoalStatus getStatus() {
        return status;
    }

    public void setStatus(GoalStatus status) {
        this.status = status;
    }

    public Integer getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(Integer targetValue) {
        this.targetValue = targetValue;
        this.progressPercentage = calculateProgressPercentage(targetValue, this.currentValue);
    }

    public Integer getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(Integer currentValue) {
        this.currentValue = currentValue;
        this.progressPercentage = calculateProgressPercentage(this.targetValue, currentValue);
    }

    public Double getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(Double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
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
