package com.devsphere.user;

import com.devsphere.user.dto.CreatePlannerEntryRequest;
import com.devsphere.user.dto.CreateTaskRequest;
import com.devsphere.user.dto.DailyPlannerResponse;
import com.devsphere.user.dto.PlannerEntryResponse;
import com.devsphere.user.dto.ReorderPlannerEntryItem;
import com.devsphere.user.dto.ReschedulePlannerEntryRequest;
import com.devsphere.user.dto.TaskResponse;
import com.devsphere.user.dto.UpdatePlannerEntryRequest;
import com.devsphere.user.entity.TaskPriority;
import com.devsphere.user.entity.TaskStatus;
import com.devsphere.user.exception.DuplicatePlannerEntryException;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.repository.PlannerEntryRepository;
import com.devsphere.user.repository.TaskRepository;
import com.devsphere.user.service.PlannerService;
import com.devsphere.user.service.TaskService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class PlannerIntegrationTest {

    @Autowired
    private PlannerService plannerService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private PlannerEntryRepository plannerEntryRepository;

    @Autowired
    private TaskRepository taskRepository;

    @BeforeEach
    void cleanDatabase() {
        plannerEntryRepository.deleteAll();
        taskRepository.deleteAll();
    }

    @Test
    void plannerCrudLifecycle_createsReschedulesUpdatesAndUnschedulesEntry() {
        Long userId = 600L;
        LocalDate date1 = LocalDate.of(2026, 8, 26);
        LocalDate date2 = LocalDate.of(2026, 8, 27);

        TaskResponse task = taskService.createTask(userId, new CreateTaskRequest("Implement Planner Domain", "Service logic", TaskPriority.HIGH, null, null));

        CreatePlannerEntryRequest createReq = new CreatePlannerEntryRequest(
                task.getId(), date1, LocalTime.of(9, 0), LocalTime.of(10, 0), 60, 1
        );
        PlannerEntryResponse created = plannerService.createPlannerEntry(userId, createReq);

        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getPlannedDate()).isEqualTo(date1);
        assertThat(created.getSortOrder()).isEqualTo(1);
        assertThat(created.getPlannedMinutes()).isEqualTo(60);

        ReschedulePlannerEntryRequest rescheduleReq = new ReschedulePlannerEntryRequest(date2, LocalTime.of(14, 0), LocalTime.of(15, 0));
        PlannerEntryResponse rescheduled = plannerService.reschedulePlannerEntry(userId, created.getId(), rescheduleReq);
        assertThat(rescheduled.getPlannedDate()).isEqualTo(date2);

        DailyPlannerResponse day1 = plannerService.getDailyPlanner(userId, date1);
        assertThat(day1.getTotalEntries()).isEqualTo(0);

        DailyPlannerResponse day2 = plannerService.getDailyPlanner(userId, date2);
        assertThat(day2.getTotalEntries()).isEqualTo(1);

        plannerService.unschedulePlannerEntry(userId, created.getId());

        DailyPlannerResponse day2AfterUnschedule = plannerService.getDailyPlanner(userId, date2);
        assertThat(day2AfterUnschedule.getTotalEntries()).isEqualTo(0);

        TaskResponse taskAfterUnschedule = taskService.getTask(userId, task.getId());
        assertThat(taskAfterUnschedule).isNotNull();
        assertThat(taskAfterUnschedule.getStatus()).isEqualTo(TaskStatus.TODO);
    }

    @Test
    void idorProtection_preventsCrossUserAccessToPlannerEntriesAndTasks() {
        Long userA = 700L;
        Long userB = 800L;
        LocalDate date = LocalDate.of(2026, 8, 26);

        TaskResponse taskA = taskService.createTask(userA, new CreateTaskRequest("User A Task", "Private", TaskPriority.MEDIUM, null, null));
        PlannerEntryResponse entryA = plannerService.createPlannerEntry(userA, new CreatePlannerEntryRequest(taskA.getId(), date, null, null, null, 1));

        assertThatThrownBy(() -> plannerService.createPlannerEntry(userB, new CreatePlannerEntryRequest(taskA.getId(), date, null, null, null, 1)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Task not found with id: " + taskA.getId());

        assertThatThrownBy(() -> plannerService.getPlannerEntry(userB, entryA.getId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Planner entry not found with id: " + entryA.getId());

        assertThatThrownBy(() -> plannerService.updatePlannerEntry(userB, entryA.getId(), new UpdatePlannerEntryRequest(date, null, null, null, 2)))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThatThrownBy(() -> plannerService.reschedulePlannerEntry(userB, entryA.getId(), new ReschedulePlannerEntryRequest(date.plusDays(1), null, null)))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThatThrownBy(() -> plannerService.unschedulePlannerEntry(userB, entryA.getId()))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThatThrownBy(() -> plannerService.reorderDay(userB, date, List.of(new ReorderPlannerEntryItem(entryA.getId(), 1))))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void duplicateScheduling_preventsSchedulingSameTaskTwiceOnSameDate() {
        Long userId = 900L;
        LocalDate date = LocalDate.of(2026, 8, 26);

        TaskResponse task = taskService.createTask(userId, new CreateTaskRequest("Task for Duplicate Test", "Details", TaskPriority.LOW, null, null));

        plannerService.createPlannerEntry(userId, new CreatePlannerEntryRequest(task.getId(), date, null, null, null, 1));

        assertThatThrownBy(() -> plannerService.createPlannerEntry(userId, new CreatePlannerEntryRequest(task.getId(), date, null, null, null, 2)))
                .isInstanceOf(DuplicatePlannerEntryException.class)
                .hasMessageContaining("is already scheduled for date");

        PlannerEntryResponse day2Entry = plannerService.createPlannerEntry(userId, new CreatePlannerEntryRequest(task.getId(), date.plusDays(1), null, null, null, 1));
        assertThat(day2Entry).isNotNull();
    }

    @Test
    void archivedAndCancelledTaskValidation_preventsSchedulingArchivedOrCancelledTask() {
        Long userId = 1000L;
        LocalDate date = LocalDate.of(2026, 8, 26);

        TaskResponse t1 = taskService.createTask(userId, new CreateTaskRequest("Archived Task", "Desc", TaskPriority.MEDIUM, null, null));
        taskService.archiveTask(userId, t1.getId());

        assertThatThrownBy(() -> plannerService.createPlannerEntry(userId, new CreatePlannerEntryRequest(t1.getId(), date, null, null, null, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot schedule an archived or cancelled task");

        TaskResponse t2 = taskService.createTask(userId, new CreateTaskRequest("Cancelled Task", "Desc", TaskPriority.MEDIUM, null, null));
        taskService.cancelTask(userId, t2.getId());

        assertThatThrownBy(() -> plannerService.createPlannerEntry(userId, new CreatePlannerEntryRequest(t2.getId(), date, null, null, null, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot schedule an archived or cancelled task");
    }

    @Test
    void taskCompletionAndReopenRegression_preservesPlannerEntryAndReflectsStatus() {
        Long userId = 1100L;
        LocalDate date = LocalDate.of(2026, 8, 26);

        TaskResponse task = taskService.createTask(userId, new CreateTaskRequest("Task to Complete", "Desc", TaskPriority.URGENT, null, null));
        plannerService.createPlannerEntry(userId, new CreatePlannerEntryRequest(task.getId(), date, null, null, 30, 1));

        taskService.completeTask(userId, task.getId());

        DailyPlannerResponse dayCompleted = plannerService.getDailyPlanner(userId, date);
        assertThat(dayCompleted.getTotalEntries()).isEqualTo(1);
        assertThat(dayCompleted.getCompletedEntries()).isEqualTo(1);
        assertThat(dayCompleted.getPendingEntries()).isEqualTo(0);
        assertThat(dayCompleted.getCompletionPercentage()).isEqualTo(100.0);
        assertThat(dayCompleted.getEntries().get(0).getTaskStatus()).isEqualTo(TaskStatus.COMPLETED);

        taskService.reopenTask(userId, task.getId());

        DailyPlannerResponse dayReopened = plannerService.getDailyPlanner(userId, date);
        assertThat(dayReopened.getCompletedEntries()).isEqualTo(0);
        assertThat(dayReopened.getPendingEntries()).isEqualTo(1);
        assertThat(dayReopened.getCompletionPercentage()).isEqualTo(0.0);
        assertThat(dayReopened.getEntries().get(0).getTaskStatus()).isEqualTo(TaskStatus.TODO);
    }

    @Test
    void dailySummaryAndCompletionPercentage_calculatesDynamically() {
        Long userId = 1200L;
        LocalDate date = LocalDate.of(2026, 8, 26);

        TaskResponse t1 = taskService.createTask(userId, new CreateTaskRequest("Task 1", "D1", TaskPriority.HIGH, null, null));
        TaskResponse t2 = taskService.createTask(userId, new CreateTaskRequest("Task 2", "D2", TaskPriority.MEDIUM, null, null));
        TaskResponse t3 = taskService.createTask(userId, new CreateTaskRequest("Task 3", "D3", TaskPriority.LOW, null, null));

        plannerService.createPlannerEntry(userId, new CreatePlannerEntryRequest(t1.getId(), date, null, null, 60, 1));
        plannerService.createPlannerEntry(userId, new CreatePlannerEntryRequest(t2.getId(), date, null, null, 30, 2));
        plannerService.createPlannerEntry(userId, new CreatePlannerEntryRequest(t3.getId(), date, null, null, 40, 3));

        taskService.completeTask(userId, t1.getId());
        taskService.completeTask(userId, t2.getId());

        DailyPlannerResponse summary = plannerService.getDailyPlanner(userId, date);
        assertThat(summary.getTotalEntries()).isEqualTo(3);
        assertThat(summary.getCompletedEntries()).isEqualTo(2);
        assertThat(summary.getPendingEntries()).isEqualTo(1);
        assertThat(summary.getTotalPlannedMinutes()).isEqualTo(130);
        assertThat(summary.getCompletionPercentage()).isEqualTo(66.67);
    }

    @Test
    void reorderDay_updatesSortOrderDeterministically() {
        Long userId = 1300L;
        LocalDate date = LocalDate.of(2026, 8, 26);

        TaskResponse t1 = taskService.createTask(userId, new CreateTaskRequest("Task A", "A", TaskPriority.HIGH, null, null));
        TaskResponse t2 = taskService.createTask(userId, new CreateTaskRequest("Task B", "B", TaskPriority.MEDIUM, null, null));
        TaskResponse t3 = taskService.createTask(userId, new CreateTaskRequest("Task C", "C", TaskPriority.LOW, null, null));

        PlannerEntryResponse e1 = plannerService.createPlannerEntry(userId, new CreatePlannerEntryRequest(t1.getId(), date, null, null, null, 1));
        PlannerEntryResponse e2 = plannerService.createPlannerEntry(userId, new CreatePlannerEntryRequest(t2.getId(), date, null, null, null, 2));
        PlannerEntryResponse e3 = plannerService.createPlannerEntry(userId, new CreatePlannerEntryRequest(t3.getId(), date, null, null, null, 3));

        DailyPlannerResponse reordered = plannerService.reorderDay(userId, date, List.of(
                new ReorderPlannerEntryItem(e3.getId(), 1),
                new ReorderPlannerEntryItem(e1.getId(), 2),
                new ReorderPlannerEntryItem(e2.getId(), 3)
        ));

        assertThat(reordered.getEntries().get(0).getPlannerEntryId()).isEqualTo(e3.getId());
        assertThat(reordered.getEntries().get(1).getPlannerEntryId()).isEqualTo(e1.getId());
        assertThat(reordered.getEntries().get(2).getPlannerEntryId()).isEqualTo(e2.getId());
    }

    @Test
    void timeSlotValidation_rejectsEndTimeBeforeStartTimeOrSingleProvidedTime() {
        Long userId = 1400L;
        LocalDate date = LocalDate.of(2026, 8, 26);

        TaskResponse t1 = taskService.createTask(userId, new CreateTaskRequest("Task 1", "Details", TaskPriority.HIGH, null, null));

        assertThatThrownBy(() -> plannerService.createPlannerEntry(userId, new CreatePlannerEntryRequest(
                t1.getId(), date, LocalTime.of(11, 0), LocalTime.of(10, 0), null, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("endTime must be strictly after startTime");

        assertThatThrownBy(() -> plannerService.createPlannerEntry(userId, new CreatePlannerEntryRequest(
                t1.getId(), date, LocalTime.of(11, 0), null, null, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Both startTime and endTime must be provided together or both omitted");
    }
}
