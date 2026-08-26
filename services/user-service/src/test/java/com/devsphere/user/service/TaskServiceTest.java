package com.devsphere.user.service;

import com.devsphere.user.dto.CreateTaskRequest;
import com.devsphere.user.dto.PageResponse;
import com.devsphere.user.dto.TaskResponse;
import com.devsphere.user.dto.UpdateTaskRequest;
import com.devsphere.user.entity.Goal;
import com.devsphere.user.entity.GoalType;
import com.devsphere.user.entity.Task;
import com.devsphere.user.entity.TaskPriority;
import com.devsphere.user.entity.TaskStatus;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.repository.GoalRepository;
import com.devsphere.user.repository.TaskRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private GoalRepository goalRepository;

    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository, goalRepository, new SimpleMeterRegistry());
    }

    @Test
    void createTask_savesTaskAndReturnsResponse() {
        Long userId = 100L;
        Instant dueDate = Instant.now().plus(2, ChronoUnit.DAYS);
        CreateTaskRequest request = new CreateTaskRequest("Complete Kafka Task", "Implement consumer retry", TaskPriority.HIGH, dueDate, null);

        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task t = invocation.getArgument(0);
            t.setId(1L);
            return t;
        });

        TaskResponse response = taskService.createTask(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("Complete Kafka Task");
        assertThat(response.getPriority()).isEqualTo(TaskPriority.HIGH);
        assertThat(response.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(response.isOverdue()).isFalse();
    }

    @Test
    void createTask_withGoalId_verifiesGoalOwnership() {
        Long userId = 100L;
        Long goalId = 50L;
        CreateTaskRequest request = new CreateTaskRequest("Solve Tree Problems", "Binary Search Tree", TaskPriority.MEDIUM, null, goalId);

        when(goalRepository.findByIdAndUserId(goalId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.createTask(userId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Goal not found with id: 50");
    }

    @Test
    void getTask_returnsTask_whenOwned() {
        Long userId = 100L;
        Long taskId = 1L;
        Task task = new Task(userId, "Read Spring Security Doc", TaskPriority.LOW);
        task.setId(taskId);

        when(taskRepository.findByIdAndUserId(taskId, userId)).thenReturn(Optional.of(task));

        TaskResponse response = taskService.getTask(userId, taskId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(taskId);
        assertThat(response.getTitle()).isEqualTo("Read Spring Security Doc");
    }

    @Test
    void getTask_throwsResourceNotFound_whenNotOwned() {
        Long userId = 100L;
        Long taskId = 999L;

        when(taskRepository.findByIdAndUserId(taskId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTask(userId, taskId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Task not found with id: 999");
    }

    @Test
    void startTask_transitionsStatusToInProgress() {
        Long userId = 100L;
        Long taskId = 1L;
        Task task = new Task(userId, "Build feature", TaskPriority.HIGH);
        task.setId(taskId);
        task.setStatus(TaskStatus.TODO);

        when(taskRepository.findByIdAndUserId(taskId, userId)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

        TaskResponse response = taskService.startTask(userId, taskId);

        assertThat(response.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void completeTask_transitionsStatusToCompletedAndSetsCompletedAt() {
        Long userId = 100L;
        Long taskId = 1L;
        Task task = new Task(userId, "Build feature", TaskPriority.HIGH);
        task.setId(taskId);
        task.setStatus(TaskStatus.IN_PROGRESS);

        when(taskRepository.findByIdAndUserId(taskId, userId)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

        TaskResponse response = taskService.completeTask(userId, taskId);

        assertThat(response.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(response.getCompletedAt()).isNotNull();
    }

    @Test
    void reopenTask_transitionsStatusToTodoAndClearsCompletedAt() {
        Long userId = 100L;
        Long taskId = 1L;
        Task task = new Task(userId, "Build feature", TaskPriority.HIGH);
        task.setId(taskId);
        task.setStatus(TaskStatus.COMPLETED);
        task.setCompletedAt(Instant.now());

        when(taskRepository.findByIdAndUserId(taskId, userId)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

        TaskResponse response = taskService.reopenTask(userId, taskId);

        assertThat(response.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(response.getCompletedAt()).isNull();
    }

    @Test
    void cancelTask_throwsException_whenAlreadyCompleted() {
        Long userId = 100L;
        Long taskId = 1L;
        Task task = new Task(userId, "Build feature", TaskPriority.HIGH);
        task.setId(taskId);
        task.setStatus(TaskStatus.COMPLETED);

        when(taskRepository.findByIdAndUserId(taskId, userId)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.cancelTask(userId, taskId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot cancel a completed task");
    }

    @Test
    void archiveTask_setsStatusToArchived() {
        Long userId = 100L;
        Long taskId = 1L;
        Task task = new Task(userId, "Build feature", TaskPriority.HIGH);
        task.setId(taskId);
        task.setStatus(TaskStatus.TODO);

        when(taskRepository.findByIdAndUserId(taskId, userId)).thenReturn(Optional.of(task));

        taskService.archiveTask(userId, taskId);

        verify(taskRepository).save(task);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.ARCHIVED);
    }
}
