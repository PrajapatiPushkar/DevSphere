package com.devsphere.user.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public class ReschedulePlannerEntryRequest {

    @NotNull(message = "plannedDate is required")
    private LocalDate plannedDate;

    private LocalTime startTime;

    private LocalTime endTime;

    public ReschedulePlannerEntryRequest() {
    }

    public ReschedulePlannerEntryRequest(LocalDate plannedDate, LocalTime startTime, LocalTime endTime) {
        this.plannedDate = plannedDate;
        this.startTime = startTime;
        this.endTime = endTime;
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
}
