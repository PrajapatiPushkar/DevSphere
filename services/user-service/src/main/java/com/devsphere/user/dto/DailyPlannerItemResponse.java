package com.devsphere.user.dto;

import com.devsphere.user.entity.PlannerEntry;
import com.devsphere.user.entity.Task;
import com.devsphere.user.entity.TaskPriority;
import com.devsphere.user.entity.TaskStatus;
import java.time.LocalTime;

public class DailyPlannerItemResponse {

    private Long plannerEntryId;
    private Long taskId;
    private String taskTitle;
    private TaskStatus taskStatus;
    private TaskPriority taskPriority;
    private Long goalId;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer plannedMinutes;
    private Integer sortOrder;

    public DailyPlannerItemResponse() {
    }

    public DailyPlannerItemResponse(PlannerEntry entry, Task task) {
        this.plannerEntryId = entry.getId();
        this.taskId = entry.getTaskId();
        this.startTime = entry.getStartTime();
        this.endTime = entry.getEndTime();
        this.plannedMinutes = entry.getPlannedMinutes();
        this.sortOrder = entry.getSortOrder();
        if (task != null) {
            this.taskTitle = task.getTitle();
            this.taskStatus = task.getStatus();
            this.taskPriority = task.getPriority();
            this.goalId = task.getGoalId();
        }
    }

    public Long getPlannerEntryId() {
        return plannerEntryId;
    }

    public void setPlannerEntryId(Long plannerEntryId) {
        this.plannerEntryId = plannerEntryId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getTaskTitle() {
        return taskTitle;
    }

    public void setTaskTitle(String taskTitle) {
        this.taskTitle = taskTitle;
    }

    public TaskStatus getTaskStatus() {
        return taskStatus;
    }

    public void setTaskStatus(TaskStatus taskStatus) {
        this.taskStatus = taskStatus;
    }

    public TaskPriority getTaskPriority() {
        return taskPriority;
    }

    public void setTaskPriority(TaskPriority taskPriority) {
        this.taskPriority = taskPriority;
    }

    public Long getGoalId() {
        return goalId;
    }

    public void setGoalId(Long goalId) {
        this.goalId = goalId;
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
