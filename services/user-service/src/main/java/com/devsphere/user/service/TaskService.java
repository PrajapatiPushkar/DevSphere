package com.devsphere.user.service;

import com.devsphere.user.dto.CreateTaskRequest;
import com.devsphere.user.dto.PageResponse;
import com.devsphere.user.dto.TaskResponse;
import com.devsphere.user.dto.UpdateTaskRequest;
import com.devsphere.user.entity.Task;
import com.devsphere.user.entity.TaskPriority;
import com.devsphere.user.entity.TaskStatus;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.repository.GoalRepository;
import com.devsphere.user.repository.TaskRepository;
import com.devsphere.user.specification.TaskSpecification;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);
    private static final int MAX_PAGE_SIZE = 100;

    private final TaskRepository taskRepository;
    private final GoalRepository goalRepository;
    private final MeterRegistry meterRegistry;

    public TaskService(TaskRepository taskRepository, GoalRepository goalRepository) {
        this(taskRepository, goalRepository, new SimpleMeterRegistry());
    }

    @Autowired
    public TaskService(TaskRepository taskRepository, GoalRepository goalRepository, MeterRegistry meterRegistry) {
        this.taskRepository = taskRepository;
        this.goalRepository = goalRepository;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public TaskResponse createTask(Long userId, CreateTaskRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }

        validateGoalOwnership(userId, request.getGoalId());

        log.info("Creating task for userId: {}, title: {}", userId, request.getTitle());

        Task task = new Task(userId, request.getTitle(), request.getPriority());
        task.setDescription(request.getDescription());
        task.setDueDate(request.getDueDate());
        task.setGoalId(request.getGoalId());
        task.setStatus(TaskStatus.TODO);

        Task saved = taskRepository.save(task);
        meterRegistry.counter("devsphere_tasks_created_total", "priority", request.getPriority().name()).increment();

        return new TaskResponse(saved);
    }

    @Transactional(readOnly = true)
    public TaskResponse getTask(Long userId, Long taskId) {
        Task task = taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("RESOURCE_NOT_FOUND", "Task not found with id: " + taskId));
        return new TaskResponse(task);
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> listTasks(Long userId, TaskStatus status, TaskPriority priority, Long goalId, int page, int size) {
        int validatedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int validatedPage = Math.max(page, 0);

        Pageable pageable = PageRequest.of(validatedPage, validatedSize);

        Specification<Task> spec = TaskSpecification.filterTasks(userId, status, priority, goalId);
        Page<Task> taskPage = taskRepository.findAll(spec, pageable);

        Page<TaskResponse> responsePage = taskPage.map(TaskResponse::new);
        return PageResponse.fromPage(responsePage);
    }

    @Transactional
    public TaskResponse updateTask(Long userId, Long taskId, UpdateTaskRequest request) {
        Task task = taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("RESOURCE_NOT_FOUND", "Task not found with id: " + taskId));

        validateGoalOwnership(userId, request.getGoalId());

        log.info("Updating task id: {} for userId: {}", taskId, userId);

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority());
        task.setDueDate(request.getDueDate());
        task.setGoalId(request.getGoalId());

        Task updated = taskRepository.save(task);
        return new TaskResponse(updated);
    }

    @Transactional
    public TaskResponse startTask(Long userId, Long taskId) {
        Task task = taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("RESOURCE_NOT_FOUND", "Task not found with id: " + taskId));

        if (task.getStatus() == TaskStatus.IN_PROGRESS) {
            return new TaskResponse(task);
        }

        if (task.getStatus() != TaskStatus.TODO) {
            throw new IllegalArgumentException("Cannot start task in status: " + task.getStatus() + ". Reopen task first.");
        }

        task.setStatus(TaskStatus.IN_PROGRESS);
        Task updated = taskRepository.save(task);
        return new TaskResponse(updated);
    }

    @Transactional
    public TaskResponse completeTask(Long userId, Long taskId) {
        Task task = taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("RESOURCE_NOT_FOUND", "Task not found with id: " + taskId));

        if (task.getStatus() == TaskStatus.COMPLETED) {
            return new TaskResponse(task);
        }

        if (task.getStatus() != TaskStatus.TODO && task.getStatus() != TaskStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Cannot complete task in status: " + task.getStatus());
        }

        task.setStatus(TaskStatus.COMPLETED);
        task.setCompletedAt(Instant.now());
        Task updated = taskRepository.save(task);
        meterRegistry.counter("devsphere_tasks_completed_total").increment();
        return new TaskResponse(updated);
    }

    @Transactional
    public TaskResponse reopenTask(Long userId, Long taskId) {
        Task task = taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("RESOURCE_NOT_FOUND", "Task not found with id: " + taskId));

        if (task.getStatus() == TaskStatus.TODO || task.getStatus() == TaskStatus.IN_PROGRESS) {
            return new TaskResponse(task);
        }

        task.setStatus(TaskStatus.TODO);
        task.setCompletedAt(null);
        Task updated = taskRepository.save(task);
        meterRegistry.counter("devsphere_tasks_reopened_total").increment();
        return new TaskResponse(updated);
    }

    @Transactional
    public TaskResponse cancelTask(Long userId, Long taskId) {
        Task task = taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("RESOURCE_NOT_FOUND", "Task not found with id: " + taskId));

        if (task.getStatus() == TaskStatus.CANCELLED) {
            return new TaskResponse(task);
        }

        if (task.getStatus() == TaskStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot cancel a completed task. Reopen it first.");
        }

        if (task.getStatus() == TaskStatus.ARCHIVED) {
            throw new IllegalArgumentException("Cannot cancel an archived task.");
        }

        task.setStatus(TaskStatus.CANCELLED);
        Task updated = taskRepository.save(task);
        meterRegistry.counter("devsphere_tasks_cancelled_total").increment();
        return new TaskResponse(updated);
    }

    @Transactional
    public void archiveTask(Long userId, Long taskId) {
        Task task = taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("RESOURCE_NOT_FOUND", "Task not found with id: " + taskId));

        task.setStatus(TaskStatus.ARCHIVED);
        taskRepository.save(task);
    }

    private void validateGoalOwnership(Long userId, Long goalId) {
        if (goalId != null) {
            goalRepository.findByIdAndUserId(goalId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("RESOURCE_NOT_FOUND", "Goal not found with id: " + goalId));
        }
    }
}
