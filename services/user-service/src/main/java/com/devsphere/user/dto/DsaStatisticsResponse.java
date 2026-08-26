package com.devsphere.user.dto;

public class DsaStatisticsResponse {

    private int totalProblems;
    private int solvedProblems;
    private int inProgressProblems;
    private int revisitProblems;
    private int easySolved;
    private int mediumSolved;
    private int hardSolved;
    private int totalTimeSpentMinutes;
    private int totalAttempts;

    public DsaStatisticsResponse() {
    }

    public DsaStatisticsResponse(int totalProblems, int solvedProblems, int inProgressProblems, int revisitProblems, int easySolved, int mediumSolved, int hardSolved, int totalTimeSpentMinutes, int totalAttempts) {
        this.totalProblems = totalProblems;
        this.solvedProblems = solvedProblems;
        this.inProgressProblems = inProgressProblems;
        this.revisitProblems = revisitProblems;
        this.easySolved = easySolved;
        this.mediumSolved = mediumSolved;
        this.hardSolved = hardSolved;
        this.totalTimeSpentMinutes = totalTimeSpentMinutes;
        this.totalAttempts = totalAttempts;
    }

    public int getTotalProblems() {
        return totalProblems;
    }

    public void setTotalProblems(int totalProblems) {
        this.totalProblems = totalProblems;
    }

    public int getSolvedProblems() {
        return solvedProblems;
    }

    public void setSolvedProblems(int solvedProblems) {
        this.solvedProblems = solvedProblems;
    }

    public int getInProgressProblems() {
        return inProgressProblems;
    }

    public void setInProgressProblems(int inProgressProblems) {
        this.inProgressProblems = inProgressProblems;
    }

    public int getRevisitProblems() {
        return revisitProblems;
    }

    public void setRevisitProblems(int revisitProblems) {
        this.revisitProblems = revisitProblems;
    }

    public int getEasySolved() {
        return easySolved;
    }

    public void setEasySolved(int easySolved) {
        this.easySolved = easySolved;
    }

    public int getMediumSolved() {
        return mediumSolved;
    }

    public void setMediumSolved(int mediumSolved) {
        this.mediumSolved = mediumSolved;
    }

    public int getHardSolved() {
        return hardSolved;
    }

    public void setHardSolved(int hardSolved) {
        this.hardSolved = hardSolved;
    }

    public int getTotalTimeSpentMinutes() {
        return totalTimeSpentMinutes;
    }

    public void setTotalTimeSpentMinutes(int totalTimeSpentMinutes) {
        this.totalTimeSpentMinutes = totalTimeSpentMinutes;
    }

    public int getTotalAttempts() {
        return totalAttempts;
    }

    public void setTotalAttempts(int totalAttempts) {
        this.totalAttempts = totalAttempts;
    }
}
