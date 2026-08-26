package com.devsphere.user.dto;

import com.devsphere.user.entity.PlannerEntry;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public class PlannerEntryResponse {

    private Long id;
    private Long userId;
    private Long taskId;
    private LocalDate plannedDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer sortOrder;
    private Integer plannedMinutes;
    private Instant createdAt;
    private Instant updatedAt;

    public PlannerEntryResponse() {
    }

    public PlannerEntryResponse(PlannerEntry entry) {
        this.id = entry.getId();
        this.userId = entry.getUserId();
        this.taskId = entry.getTaskId();
        this.plannedDate = entry.getPlannedDate();
        this.startTime = entry.getStartTime();
        this.endTime = entry.getEndTime();
        this.sortOrder = entry.getSortOrder();
        this.plannedMinutes = entry.getPlannedMinutes();
        this.createdAt = entry.getCreatedAt();
        this.updatedAt = entry.getUpdatedAt();
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

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Integer getPlannedMinutes() {
        return plannedMinutes;
    }

    public void setPlannedMinutes(Integer plannedMinutes) {
        this.plannedMinutes = plannedMinutes;
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
