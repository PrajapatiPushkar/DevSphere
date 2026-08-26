package com.devsphere.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public class UpdatePlannerEntryRequest {

    @NotNull(message = "plannedDate is required")
    private LocalDate plannedDate;

    private LocalTime startTime;

    private LocalTime endTime;

    @Min(value = 1, message = "plannedMinutes must be greater than 0")
    @Max(value = 1440, message = "plannedMinutes cannot exceed 1440 (24 hours)")
    private Integer plannedMinutes;

    @NotNull(message = "sortOrder is required")
    @Min(value = 0, message = "sortOrder must be zero or positive")
    private Integer sortOrder;

    public UpdatePlannerEntryRequest() {
    }

    public UpdatePlannerEntryRequest(LocalDate plannedDate, LocalTime startTime, LocalTime endTime, Integer plannedMinutes, Integer sortOrder) {
        this.plannedDate = plannedDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.plannedMinutes = plannedMinutes;
        this.sortOrder = sortOrder;
    }

    public LocalDate getPlannedDate() {
        return plannedDate;
    }

    public void setPlannedDate(LocalDate plannedDate) {
        this.plannedDate = plannedDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public Integer getPlannedMinutes() {
        return plannedMinutes;
    }

    public void setPlannedMinutes(Integer plannedMinutes) {
        this.plannedMinutes = plannedMinutes;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
