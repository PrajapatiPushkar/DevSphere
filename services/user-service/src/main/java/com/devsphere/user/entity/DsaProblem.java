package com.devsphere.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(
    name = "dsa_problems",
    indexes = {
        @Index(name = "idx_dsa_user_status", columnList = "user_id, status"),
        @Index(name = "idx_dsa_user_created", columnList = "user_id, created_at")
    }
)
public class DsaProblem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "goal_id")
    private Long goalId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 50)
    private DsaPlatform platform;

    @Column(name = "problem_url", length = 512)
    private String problemUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, length = 30)
    private DsaDifficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(name = "topic", nullable = false, length = 50)
    private DsaTopic topic;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DsaProblemStatus status;

    @Column(name = "solved_at")
    private Instant solvedAt;

    @Column(name = "time_spent_minutes")
    private Integer timeSpentMinutes;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public DsaProblem() {
    }

    public DsaProblem(Long userId, String title, DsaPlatform platform, DsaDifficulty difficulty, DsaTopic topic) {
        this.userId = userId;
        this.title = title;
        this.platform = platform;
        this.difficulty = difficulty;
        this.topic = topic;
        this.status = DsaProblemStatus.TODO;
        this.attemptCount = 0;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.status == null) {
            this.status = DsaProblemStatus.TODO;
        }
        if (this.attemptCount == null) {
            this.attemptCount = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
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

    public Long getGoalId() {
        return goalId;
    }

    public void setGoalId(Long goalId) {
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

    public DsaProblemStatus getStatus() {
        return status;
    }

    public void setStatus(DsaProblemStatus status) {
        this.status = status;
    }

    public Instant getSolvedAt() {
        return solvedAt;
    }

    public void setSolvedAt(Instant solvedAt) {
        this.solvedAt = solvedAt;
    }

    public Integer getTimeSpentMinutes() {
        return timeSpentMinutes;
    }

    public void setTimeSpentMinutes(Integer timeSpentMinutes) {
        this.timeSpentMinutes = timeSpentMinutes;
    }

    public Integer getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(Integer attemptCount) {
        this.attemptCount = attemptCount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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
