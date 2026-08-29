package com.devsphere.user;

import com.devsphere.user.dto.CreateGoalRequest;
import com.devsphere.user.dto.CreateTaskRequest;
import com.devsphere.user.dto.GoalResponse;
import com.devsphere.user.dto.PageResponse;
import com.devsphere.user.dto.TaskResponse;
import com.devsphere.user.entity.GoalStatus;
import com.devsphere.user.entity.GoalType;
import com.devsphere.user.entity.TaskPriority;
import com.devsphere.user.entity.TaskStatus;
import com.devsphere.user.repository.GoalRepository;
import com.devsphere.user.repository.TaskRepository;
import com.devsphere.user.service.GoalService;
import com.devsphere.user.service.TaskService;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class PaginationAndFilteringIntegrationTest {

    @Autowired
    private GoalService goalService;

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepository;

    @BeforeEach
    void cleanDatabase() {
        taskRepository.deleteAll();
        goalRepository.deleteAll();
    }

    @Test
    void getGoals_paginatesAndSortsCorrectly() {
        Long userId = 901L;

        for (int i = 1; i <= 5; i++) {
            goalService.createGoal(userId, new CreateGoalRequest(
                    "Goal " + i, "Description " + i, GoalType.WEEKLY, 10, 0, LocalDate.now().plusDays(i)
            ));
        }

        PageResponse<GoalResponse> page0 = goalService.getGoals(userId, null, null, 0, 2, "title,asc");
        assertThat(page0.getContent()).hasSize(2);
        assertThat(page0.getContent().get(0).getTitle()).isEqualTo("Goal 1");
        assertThat(page0.getContent().get(1).getTitle()).isEqualTo("Goal 2");
        assertThat(page0.getTotalElements()).isEqualTo(5);
        assertThat(page0.getTotalPages()).isEqualTo(3);
        assertThat(page0.isFirst()).isTrue();
        assertThat(page0.isLast()).isFalse();

        PageResponse<GoalResponse> page1 = goalService.getGoals(userId, null, null, 1, 2, "title,asc");
        assertThat(page1.getContent()).hasSize(2);
        assertThat(page1.getContent().get(0).getTitle()).isEqualTo("Goal 3");
        assertThat(page1.getContent().get(1).getTitle()).isEqualTo("Goal 4");
        assertThat(page1.isFirst()).isFalse();
        assertThat(page1.isLast()).isFalse();

        PageResponse<GoalResponse> page2 = goalService.getGoals(userId, null, null, 2, 2, "title,asc");
        assertThat(page2.getContent()).hasSize(1);
        assertThat(page2.getContent().get(0).getTitle()).isEqualTo("Goal 5");
        assertThat(page2.isFirst()).isFalse();
        assertThat(page2.isLast()).isTrue();
    }

    @Test
    void getGoals_enforcesUserScopingAndIdorProtection() {
        Long userA = 1001L;
        Long userB = 1002L;

        goalService.createGoal(userA, new CreateGoalRequest("User A Goal", "Desc", GoalType.DAILY, 1, 0, LocalDate.now()));
        goalService.createGoal(userB, new CreateGoalRequest("User B Goal", "Desc", GoalType.DAILY, 1, 0, LocalDate.now()));

        PageResponse<GoalResponse> userAGoals = goalService.getGoals(userA, null, null, 0, 20, "createdAt,desc");
        assertThat(userAGoals.getContent()).hasSize(1);
        assertThat(userAGoals.getContent().get(0).getTitle()).isEqualTo("User A Goal");

        PageResponse<GoalResponse> userBGoals = goalService.getGoals(userB, null, null, 0, 20, "createdAt,desc");
        assertThat(userBGoals.getContent()).hasSize(1);
        assertThat(userBGoals.getContent().get(0).getTitle()).isEqualTo("User B Goal");
    }

    @Test
    void getGoals_handlesEmptyResultCorrectly() {
        Long userId = 999L;
        PageResponse<GoalResponse> response = goalService.getGoals(userId, GoalStatus.COMPLETED, GoalType.LONG_TERM, 0, 20, "createdAt,desc");

        assertThat(response.getContent()).isEmpty();
        assertThat(response.getTotalElements()).isEqualTo(0);
        assertThat(response.getTotalPages()).isEqualTo(0);
        assertThat(response.isFirst()).isTrue();
        assertThat(response.isLast()).isTrue();
    }

    @Test
    void getGoals_rejectsInvalidParameters() {
        Long userId = 901L;

        assertThatThrownBy(() -> goalService.getGoals(userId, null, null, -1, 20, "createdAt,desc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Page index must not be negative");

        assertThatThrownBy(() -> goalService.getGoals(userId, null, null, 0, 0, "createdAt,desc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Page size must be greater than zero");

        assertThatThrownBy(() -> goalService.getGoals(userId, null, null, 0, 150, "createdAt,desc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Page size must not exceed 100");

        assertThatThrownBy(() -> goalService.getGoals(userId, null, null, 0, 20, "secretKey,desc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid sort field");

        assertThatThrownBy(() -> goalService.getGoals(userId, null, null, 0, 20, "createdAt,sideways"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid sort direction");
    }

    @Test
    void listTasks_combinesFilteringSortingAndPagination() {
        Long userId = 902L;

        taskService.createTask(userId, new CreateTaskRequest("Task High 1", "Desc", TaskPriority.HIGH, Instant.now(), null));
        taskService.createTask(userId, new CreateTaskRequest("Task High 2", "Desc", TaskPriority.HIGH, Instant.now(), null));
        taskService.createTask(userId, new CreateTaskRequest("Task Low 1", "Desc", TaskPriority.LOW, Instant.now(), null));

        PageResponse<TaskResponse> highPriority = taskService.listTasks(userId, TaskStatus.TODO, TaskPriority.HIGH, null, 0, 10, "title,asc");
        assertThat(highPriority.getContent()).hasSize(2);
        assertThat(highPriority.getContent().get(0).getTitle()).isEqualTo("Task High 1");
        assertThat(highPriority.getContent().get(1).getTitle()).isEqualTo("Task High 2");
    }
}
