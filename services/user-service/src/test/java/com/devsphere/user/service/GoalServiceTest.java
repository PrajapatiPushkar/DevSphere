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
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class GoalServiceTest {

    @Mock
    private GoalRepository goalRepository;

    private GoalService goalService;

    @BeforeEach
    void setUp() {
        goalService = new GoalService(goalRepository, new SimpleMeterRegistry());
    }

    @Test
    void createGoal_savesGoalAndReturnsResponse() {
        Long userId = 100L;
        CreateGoalRequest request = new CreateGoalRequest("Learn Java 21", "Master modern Java features", GoalType.LONG_TERM, 100, 20, LocalDate.now().plusMonths(3));

        when(goalRepository.save(any(Goal.class))).thenAnswer(invocation -> {
            Goal g = invocation.getArgument(0);
            g.setId(1L);
            return g;
        });

        GoalResponse response = goalService.createGoal(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getTitle()).isEqualTo("Learn Java 21");
        assertThat(response.getGoalType()).isEqualTo(GoalType.LONG_TERM);
        assertThat(response.getStatus()).isEqualTo(GoalStatus.ACTIVE);
        assertThat(response.getProgressPercentage()).isEqualTo(20.0);
    }

    @Test
    void getGoals_returnsPaginatedUserGoals() {
        Long userId = 100L;
        Goal goal1 = new Goal(userId, "Goal 1", GoalType.DAILY);
        goal1.setId(1L);

        when(goalRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(goal1)));

        PageResponse<GoalResponse> pageResponse = goalService.getGoals(userId, null, null, 0, 10);

        assertThat(pageResponse).isNotNull();
        assertThat(pageResponse.getContent()).hasSize(1);
        assertThat(pageResponse.getContent().get(0).getTitle()).isEqualTo("Goal 1");
    }

    @Test
    void getGoals_throwsExceptionWhenSizeExceedsMax() {
        Long userId = 100L;

        assertThatThrownBy(() -> goalService.getGoals(userId, null, null, 0, 500))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not exceed 100");
    }

    @Test
    void getGoalById_returnsGoalWhenOwned() {
        Long userId = 100L;
        Long goalId = 1L;
        Goal goal = new Goal(userId, "Test Goal", GoalType.WEEKLY);
        goal.setId(goalId);

        when(goalRepository.findByIdAndUserId(goalId, userId)).thenReturn(Optional.of(goal));

        GoalResponse response = goalService.getGoalById(userId, goalId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(goalId);
        assertThat(response.getTitle()).isEqualTo("Test Goal");
    }

    @Test
    void getGoalById_throwsNotFoundWhenNotOwned() {
        Long userId = 100L;
        Long otherUserId = 200L;
        Long goalId = 1L;

        when(goalRepository.findByIdAndUserId(goalId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> goalService.getGoalById(userId, goalId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Goal not found");
    }

    @Test
    void updateGoal_setsCompletedAtWhenStatusChangedToCompleted() {
        Long userId = 100L;
        Long goalId = 1L;
        Goal goal = new Goal(userId, "Test Goal", GoalType.DAILY);
        goal.setId(goalId);
        goal.setStatus(GoalStatus.ACTIVE);

        UpdateGoalRequest updateRequest = new UpdateGoalRequest(
                "Updated Goal", "Updated Desc", GoalType.DAILY, GoalStatus.COMPLETED, 10, 10, LocalDate.now()
        );

        when(goalRepository.findByIdAndUserId(goalId, userId)).thenReturn(Optional.of(goal));
        when(goalRepository.save(any(Goal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GoalResponse response = goalService.updateGoal(userId, goalId, updateRequest);

        assertThat(response.getStatus()).isEqualTo(GoalStatus.COMPLETED);
        assertThat(response.getCompletedAt()).isNotNull();
        assertThat(response.getProgressPercentage()).isEqualTo(100.0);
    }

    @Test
    void archiveGoal_transitionsStatusToArchived() {
        Long userId = 100L;
        Long goalId = 1L;
        Goal goal = new Goal(userId, "Test Goal", GoalType.DAILY);
        goal.setId(goalId);
        goal.setStatus(GoalStatus.ACTIVE);

        when(goalRepository.findByIdAndUserId(goalId, userId)).thenReturn(Optional.of(goal));

        goalService.archiveGoal(userId, goalId);

        ArgumentCaptor<Goal> captor = ArgumentCaptor.forClass(Goal.class);
        verify(goalRepository).save(captor.capture());

        assertThat(captor.getValue().getStatus()).isEqualTo(GoalStatus.ARCHIVED);
    }
}
