package com.devsphere.user.service;

import com.devsphere.user.dto.CreateGoalRequest;
import com.devsphere.user.dto.GoalResponse;
import com.devsphere.user.dto.PageResponse;
import com.devsphere.user.dto.UpdateGoalRequest;
import com.devsphere.user.entity.Goal;
import com.devsphere.user.entity.GoalStatus;
import com.devsphere.user.entity.GoalType;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.repository.GoalRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoalService {

    private static final Logger log = LoggerFactory.getLogger(GoalService.class);
    private static final int MAX_PAGE_SIZE = 100;

    private final GoalRepository goalRepository;
    private final MeterRegistry meterRegistry;

    public GoalService(GoalRepository goalRepository) {
        this(goalRepository, new SimpleMeterRegistry());
    }

    @Autowired
    public GoalService(GoalRepository goalRepository, MeterRegistry meterRegistry) {
        this.goalRepository = goalRepository;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public GoalResponse createGoal(Long userId, CreateGoalRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }

        log.info("Creating new goal for userId: {}, title: {}", userId, request.getTitle());

        Goal goal = new Goal(userId, request.getTitle(), request.getGoalType());
        goal.setDescription(request.getDescription());
        goal.setTargetValue(request.getTargetValue());
        goal.setCurrentValue(request.getCurrentValue() != null ? request.getCurrentValue() : 0);
        goal.setTargetDate(request.getTargetDate());
        goal.setStatus(GoalStatus.ACTIVE);

        Goal saved = goalRepository.save(goal);
        meterRegistry.counter("devsphere_goals_created_total", "goal_type", request.getGoalType().name()).increment();

        log.info("Goal created successfully with id: {} for userId: {}", saved.getId(), userId);
        return GoalResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<GoalResponse> getGoals(Long userId, GoalStatus status, GoalType goalType, int page, int size) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        if (size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Page size must not exceed " + MAX_PAGE_SIZE);
        }

        int pageNum = Math.max(0, page);
        int pageSize = Math.max(1, size);

        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Goal> goalPage;
        if (status != null && goalType != null) {
            goalPage = goalRepository.findAllByUserIdAndStatusAndGoalType(userId, status, goalType, pageable);
        } else if (status != null) {
            goalPage = goalRepository.findAllByUserIdAndStatus(userId, status, pageable);
        } else if (goalType != null) {
            goalPage = goalRepository.findAllByUserIdAndGoalType(userId, goalType, pageable);
        } else {
            goalPage = goalRepository.findAllByUserId(userId, pageable);
        }

        Page<GoalResponse> responsePage = goalPage.map(GoalResponse::fromEntity);
        return PageResponse.fromPage(responsePage);
    }

    @Transactional(readOnly = true)
    public GoalResponse getGoalById(Long userId, Long goalId) {
        if (userId == null || goalId == null) {
            throw new IllegalArgumentException("User ID and Goal ID must not be null");
        }

        log.info("Fetching goal with id: {} for userId: {}", goalId, userId);
        Goal goal = goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("RESOURCE_NOT_FOUND", "Goal not found with id: " + goalId));

        return GoalResponse.fromEntity(goal);
    }

    @Transactional
    public GoalResponse updateGoal(Long userId, Long goalId, UpdateGoalRequest request) {
        if (userId == null || goalId == null) {
            throw new IllegalArgumentException("User ID and Goal ID must not be null");
        }

        log.info("Updating goal with id: {} for userId: {}", goalId, userId);
        Goal goal = goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("RESOURCE_NOT_FOUND", "Goal not found with id: " + goalId));

        GoalStatus oldStatus = goal.getStatus();
        GoalStatus newStatus = request.getStatus();

        goal.setTitle(request.getTitle());
        goal.setDescription(request.getDescription());
        goal.setGoalType(request.getGoalType());
        goal.setStatus(newStatus);
        goal.setTargetValue(request.getTargetValue());
        goal.setCurrentValue(request.getCurrentValue() != null ? request.getCurrentValue() : 0);
        goal.setTargetDate(request.getTargetDate());

        if (newStatus == GoalStatus.COMPLETED && oldStatus != GoalStatus.COMPLETED) {
            goal.setCompletedAt(Instant.now());
            meterRegistry.counter("devsphere_goals_completed_total", "goal_type", request.getGoalType().name()).increment();
        } else if (newStatus != GoalStatus.COMPLETED) {
            goal.setCompletedAt(null);
        }

        Goal updated = goalRepository.save(goal);
        log.info("Goal updated successfully with id: {} for userId: {}", updated.getId(), userId);
        return GoalResponse.fromEntity(updated);
    }

    @Transactional
    public void archiveGoal(Long userId, Long goalId) {
        if (userId == null || goalId == null) {
            throw new IllegalArgumentException("User ID and Goal ID must not be null");
        }

        log.info("Archiving goal with id: {} for userId: {}", goalId, userId);
        Goal goal = goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("RESOURCE_NOT_FOUND", "Goal not found with id: " + goalId));

        goal.setStatus(GoalStatus.ARCHIVED);
        goalRepository.save(goal);
        log.info("Goal logically archived with id: {} for userId: {}", goalId, userId);
    }
}
