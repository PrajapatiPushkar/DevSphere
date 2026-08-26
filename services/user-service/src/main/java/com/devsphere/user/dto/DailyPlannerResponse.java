package com.devsphere.user.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DailyPlannerResponse {

    private LocalDate date;
    private int totalEntries;
    private int completedEntries;
    private int pendingEntries;
    private int totalPlannedMinutes;
    private double completionPercentage;
    private List<DailyPlannerItemResponse> entries = new ArrayList<>();

    public DailyPlannerResponse() {
    }

    public DailyPlannerResponse(LocalDate date, List<DailyPlannerItemResponse> entries) {
        this.date = date;
        this.entries = entries != null ? entries : new ArrayList<>();
        this.totalEntries = this.entries.size();
        this.completedEntries = (int) this.entries.stream()
                .filter(item -> item.getTaskStatus() == com.devsphere.user.entity.TaskStatus.COMPLETED)
                .count();
        this.pendingEntries = this.totalEntries - this.completedEntries;
        this.totalPlannedMinutes = this.entries.stream()
                .filter(item -> item.getPlannedMinutes() != null)
                .mapToInt(DailyPlannerItemResponse::getPlannedMinutes)
                .sum();
        if (this.totalEntries > 0) {
            double calc = ((double) this.completedEntries / (double) this.totalEntries) * 100.0;
            this.completionPercentage = Math.round(calc * 100.0) / 100.0;
        } else {
            this.completionPercentage = 0.0;
        }
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public int getTotalEntries() {
        return totalEntries;
    }

    public void setTotalEntries(int totalEntries) {
        this.totalEntries = totalEntries;
    }

    public int getCompletedEntries() {
        return completedEntries;
    }

    public void setCompletedEntries(int completedEntries) {
        this.completedEntries = completedEntries;
    }

    public int getPendingEntries() {
        return pendingEntries;
    }

    public void setPendingEntries(int pendingEntries) {
        this.pendingEntries = pendingEntries;
    }

    public int getTotalPlannedMinutes() {
        return totalPlannedMinutes;
    }

    public void setTotalPlannedMinutes(int totalPlannedMinutes) {
        this.totalPlannedMinutes = totalPlannedMinutes;
    }

    public double getCompletionPercentage() {
        return completionPercentage;
    }

    public void setCompletionPercentage(double completionPercentage) {
        this.completionPercentage = completionPercentage;
    }

    public List<DailyPlannerItemResponse> getEntries() {
        return entries;
    }

    public void setEntries(List<DailyPlannerItemResponse> entries) {
        this.entries = entries;
    }
}
