package com.devsphere.user;

import com.devsphere.user.dto.CreateGoalRequest;
import com.devsphere.user.dto.GoalResponse;
import com.devsphere.user.dto.PageResponse;
import com.devsphere.user.dto.UpdateGoalRequest;
import com.devsphere.user.entity.Goal;
import com.devsphere.user.entity.GoalStatus;
import com.devsphere.user.entity.GoalType;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.repository.GoalRepository;
import com.devsphere.user.service.GoalService;
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
class GoalIntegrationTest {

    @Autowired
    private GoalService goalService;

    @Autowired
    private GoalRepository goalRepository;

    @BeforeEach
    void cleanDatabase() {
        goalRepository.deleteAll();
    }

    @Test
    void goalCrudLifecycle_createsUpdatesAndArchivesGoal() {
        Long userId = 500L;
        CreateGoalRequest createReq = new CreateGoalRequest(
                "Complete 300 DSA Problems", "Backend interview preparation", GoalType.LONG_TERM, 300, 45, LocalDate.of(2027, 1, 1)
        );

        GoalResponse created = goalService.createGoal(userId, createReq);

        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getUserId()).isEqualTo(userId);
        assertThat(created.getTitle()).isEqualTo("Complete 300 DSA Problems");
        assertThat(created.getGoalType()).isEqualTo(GoalType.LONG_TERM);
        assertThat(created.getStatus()).isEqualTo(GoalStatus.ACTIVE);
        assertThat(created.getProgressPercentage()).isEqualTo(15.0);
        assertThat(created.getCompletedAt()).isNull();

        UpdateGoalRequest updateReq = new UpdateGoalRequest(
                "Complete 300 DSA Problems", "Finished all problems!", GoalType.LONG_TERM, GoalStatus.COMPLETED, 300, 300, LocalDate.of(2027, 1, 1)
        );

        GoalResponse updated = goalService.updateGoal(userId, created.getId(), updateReq);

        assertThat(updated.getStatus()).isEqualTo(GoalStatus.COMPLETED);
        assertThat(updated.getCompletedAt()).isNotNull();
        assertThat(updated.getProgressPercentage()).isEqualTo(100.0);

        goalService.archiveGoal(userId, created.getId());

        Goal archived = goalRepository.findById(created.getId()).orElseThrow();
        assertThat(archived.getStatus()).isEqualTo(GoalStatus.ARCHIVED);
    }

    @Test
    void idorProtection_preventsCrossUserAccessToGoals() {
        Long userA = 501L;
        Long userB = 502L;

        CreateGoalRequest createReq = new CreateGoalRequest("User A Private Goal", "Confidential", GoalType.DAILY, 1, 0, null);
        GoalResponse goalA = goalService.createGoal(userA, createReq);

        assertThatThrownBy(() -> goalService.getGoalById(userB, goalA.getId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Goal not found");

        UpdateGoalRequest updateReq = new UpdateGoalRequest("Hacked Title", "Hack", GoalType.DAILY, GoalStatus.COMPLETED, 1, 1, null);
        assertThatThrownBy(() -> goalService.updateGoal(userB, goalA.getId(), updateReq))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Goal not found");

        assertThatThrownBy(() -> goalService.archiveGoal(userB, goalA.getId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Goal not found");

        PageResponse<GoalResponse> userBGoals = goalService.getGoals(userB, null, null, 0, 20);
        assertThat(userBGoals.getContent()).isEmpty();

        PageResponse<GoalResponse> userAGoals = goalService.getGoals(userA, null, null, 0, 20);
        assertThat(userAGoals.getContent()).hasSize(1);
        assertThat(userAGoals.getContent().get(0).getId()).isEqualTo(goalA.getId());
    }

    @Test
    void goalFilteringAndPagination_returnsFilteredSubsets() {
        Long userId = 503L;

        goalService.createGoal(userId, new CreateGoalRequest("Daily 1", "D1", GoalType.DAILY, 5, 1, null));
        goalService.createGoal(userId, new CreateGoalRequest("Daily 2", "D2", GoalType.DAILY, 5, 5, null));
        goalService.createGoal(userId, new CreateGoalRequest("Weekly 1", "W1", GoalType.WEEKLY, 10, 2, null));

        PageResponse<GoalResponse> dailyGoals = goalService.getGoals(userId, null, GoalType.DAILY, 0, 20);
        assertThat(dailyGoals.getContent()).hasSize(2);

        PageResponse<GoalResponse> weeklyGoals = goalService.getGoals(userId, null, GoalType.WEEKLY, 0, 20);
        assertThat(weeklyGoals.getContent()).hasSize(1);

        PageResponse<GoalResponse> allGoals = goalService.getGoals(userId, null, null, 0, 2);
        assertThat(allGoals.getContent()).hasSize(2);
        assertThat(allGoals.getTotalElements()).isEqualTo(3);
    }
}
