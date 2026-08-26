package com.devsphere.user.dto;

import com.devsphere.user.entity.GoalStatus;
import com.devsphere.user.entity.GoalType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class UpdateGoalRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotNull(message = "Goal type is required")
    private GoalType goalType;

    @NotNull(message = "Goal status is required")
    private GoalStatus status;

    @Min(value = 0, message = "Target value must not be negative")
    private Integer targetValue;

    @Min(value = 0, message = "Current value must not be negative")
    private Integer currentValue;

    private LocalDate targetDate;

    public UpdateGoalRequest() {
    }

    public UpdateGoalRequest(String title, String description, GoalType goalType, GoalStatus status, Integer targetValue, Integer currentValue, LocalDate targetDate) {
        this.title = title;
        this.description = description;
        this.goalType = goalType;
        this.status = status;
        this.targetValue = targetValue;
        this.currentValue = currentValue;
        this.targetDate = targetDate;
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
    }

    public Integer getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(Integer currentValue) {
        this.currentValue = currentValue;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }
}
