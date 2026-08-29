package com.devsphere.user;

import com.devsphere.user.dto.CreateGoalRequest;
import com.devsphere.user.dto.CreateTaskRequest;
import com.devsphere.user.dto.GoalResponse;
import com.devsphere.user.dto.PageResponse;
import com.devsphere.user.dto.TaskResponse;
import com.devsphere.user.dto.UpdateTaskRequest;
import com.devsphere.user.entity.GoalType;
import com.devsphere.user.entity.TaskPriority;
import com.devsphere.user.entity.TaskStatus;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.repository.GoalRepository;
import com.devsphere.user.repository.TaskRepository;
import com.devsphere.user.service.GoalService;
import com.devsphere.user.service.TaskService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class TaskIntegrationTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private GoalService goalService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private GoalRepository goalRepository;

    @BeforeEach
    void cleanDatabase() {
        taskRepository.deleteAll();
        goalRepository.deleteAll();
    }

    @Test
    void taskCrudLifecycle_createsStartsCompletesReopensAndArchivesTask() {
        Long userId = 600L;
        CreateTaskRequest createReq = new CreateTaskRequest(
                "Complete Arrays 1-10", "10 problems", TaskPriority.HIGH, Instant.now().plus(1, ChronoUnit.DAYS), null
        );

        TaskResponse created = taskService.createTask(userId, createReq);
        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(created.isOverdue()).isFalse();

        TaskResponse started = taskService.startTask(userId, created.getId());
        assertThat(started.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);

        TaskResponse completed = taskService.completeTask(userId, created.getId());
        assertThat(completed.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(completed.getCompletedAt()).isNotNull();

        TaskResponse reopened = taskService.reopenTask(userId, created.getId());
        assertThat(reopened.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(reopened.getCompletedAt()).isNull();

        taskService.archiveTask(userId, created.getId());
        TaskResponse archived = taskService.getTask(userId, created.getId());
        assertThat(archived.getStatus()).isEqualTo(TaskStatus.ARCHIVED);
    }

    @Test
    void idorProtection_preventsCrossUserAccessToTasks() {
        Long userA = 700L;
        Long userB = 800L;

        CreateTaskRequest request = new CreateTaskRequest("User A Private Task", "Confidential", TaskPriority.URGENT, null, null);
        TaskResponse taskA = taskService.createTask(userA, request);

        assertThatThrownBy(() -> taskService.getTask(userB, taskA.getId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Task not found with id: " + taskA.getId());

        assertThatThrownBy(() -> taskService.updateTask(userB, taskA.getId(), new UpdateTaskRequest("Hacked Title", "Desc", TaskPriority.LOW, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThatThrownBy(() -> taskService.startTask(userB, taskA.getId()))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThatThrownBy(() -> taskService.completeTask(userB, taskA.getId()))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThatThrownBy(() -> taskService.reopenTask(userB, taskA.getId()))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThatThrownBy(() -> taskService.cancelTask(userB, taskA.getId()))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThatThrownBy(() -> taskService.archiveTask(userB, taskA.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void goalOwnership_preventsLinkingTaskToOtherUsersGoal() {
        Long userA = 900L;
        Long userB = 901L;

        GoalResponse goalA = goalService.createGoal(userA, new CreateGoalRequest("User A Goal", "Desc", GoalType.LONG_TERM, 100, 0, null));

        CreateTaskRequest invalidTask = new CreateTaskRequest("User B Task with User A Goal", "Invalid link", TaskPriority.MEDIUM, null, goalA.getId());

        assertThatThrownBy(() -> taskService.createTask(userB, invalidTask))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Goal not found with id: " + goalA.getId());
    }

    @Test
    void taskFilteringSortingAndOverdue_returnsExpectedResults() {
        Long userId = 1000L;

        Instant pastDate = Instant.now().minus(2, ChronoUnit.DAYS);
        Instant futureDate = Instant.now().plus(5, ChronoUnit.DAYS);

        TaskResponse t1 = taskService.createTask(userId, new CreateTaskRequest("Overdue Task", "Past due", TaskPriority.HIGH, pastDate, null));
        TaskResponse t2 = taskService.createTask(userId, new CreateTaskRequest("Future Task", "Future due", TaskPriority.LOW, futureDate, null));
        TaskResponse t3 = taskService.createTask(userId, new CreateTaskRequest("No Due Date Task", "No due date", TaskPriority.HIGH, null, null));

        assertThat(t1.isOverdue()).isTrue();
        assertThat(t2.isOverdue()).isFalse();
        assertThat(t3.isOverdue()).isFalse();

        PageResponse<TaskResponse> highPriority = taskService.listTasks(userId, null, TaskPriority.HIGH, null, 0, 20);
        assertThat(highPriority.getContent()).hasSize(2);

        PageResponse<TaskResponse> todoTasks = taskService.listTasks(userId, TaskStatus.TODO, null, null, 0, 20, "createdAt,asc");
        assertThat(todoTasks.getContent()).hasSize(3);

        assertThat(todoTasks.getContent().get(0).getId()).isEqualTo(t1.getId());
        assertThat(todoTasks.getContent().get(1).getId()).isEqualTo(t2.getId());
        assertThat(todoTasks.getContent().get(2).getId()).isEqualTo(t3.getId());
    }
}
