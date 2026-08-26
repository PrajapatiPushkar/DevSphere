package com.devsphere.user.dto;

import java.time.LocalDate;

public class DailyDsaProgressResponse {

    private LocalDate date;
    private int problemsSolved;
    private int totalAttempts;
    private int timeSpentMinutes;

    public DailyDsaProgressResponse() {
    }

    public DailyDsaProgressResponse(LocalDate date, int problemsSolved, int totalAttempts, int timeSpentMinutes) {
        this.date = date;
        this.problemsSolved = problemsSolved;
        this.totalAttempts = totalAttempts;
        this.timeSpentMinutes = timeSpentMinutes;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public int getProblemsSolved() {
        return problemsSolved;
    }

    public void setProblemsSolved(int problemsSolved) {
        this.problemsSolved = problemsSolved;
    }

    public int getTotalAttempts() {
        return totalAttempts;
    }

    public void setTotalAttempts(int totalAttempts) {
        this.totalAttempts = totalAttempts;
    }

    public int getTimeSpentMinutes() {
        return timeSpentMinutes;
    }

    public void setTimeSpentMinutes(int timeSpentMinutes) {
        this.timeSpentMinutes = timeSpentMinutes;
    }
}
