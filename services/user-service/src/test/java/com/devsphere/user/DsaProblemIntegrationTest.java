package com.devsphere.user;

import com.devsphere.user.dto.CreateDsaProblemRequest;
import com.devsphere.user.dto.CreateGoalRequest;
import com.devsphere.user.dto.CreateTaskRequest;
import com.devsphere.user.dto.DailyDsaProgressResponse;
import com.devsphere.user.dto.DsaProblemResponse;
import com.devsphere.user.dto.DsaStatisticsResponse;
import com.devsphere.user.dto.GoalResponse;
import com.devsphere.user.dto.PageResponse;
import com.devsphere.user.dto.TaskResponse;
import com.devsphere.user.dto.UpdateDsaProblemRequest;
import com.devsphere.user.entity.DsaDifficulty;
import com.devsphere.user.entity.DsaPlatform;
import com.devsphere.user.entity.DsaProblemStatus;
import com.devsphere.user.entity.DsaTopic;
import com.devsphere.user.entity.GoalType;
import com.devsphere.user.entity.TaskPriority;
import com.devsphere.user.exception.DuplicateDsaProblemException;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.repository.DsaProblemRepository;
import com.devsphere.user.repository.GoalRepository;
import com.devsphere.user.repository.TaskRepository;
import com.devsphere.user.service.DsaProblemService;
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
class DsaProblemIntegrationTest {

    @Autowired
    private DsaProblemService dsaProblemService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private GoalService goalService;

    @Autowired
    private DsaProblemRepository dsaProblemRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private GoalRepository goalRepository;

    @BeforeEach
    void cleanDatabase() {
        dsaProblemRepository.deleteAll();
        taskRepository.deleteAll();
        goalRepository.deleteAll();
    }

    @Test
    void dsaProblemCrudLifecycle_createsStartsSolvesRevisitsAndArchivesProblem() {
        Long userId = 600L;
        CreateDsaProblemRequest createReq = new CreateDsaProblemRequest(
                "Reverse Linked List", "Reverse a singly linked list", DsaPlatform.LEETCODE,
                "https://leetcode.com/problems/reverse-linked-list", DsaDifficulty.EASY, DsaTopic.LINKED_LIST, 15, "Iterative approach", null, null
        );

        DsaProblemResponse created = dsaProblemService.createProblem(userId, createReq);
        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getStatus()).isEqualTo(DsaProblemStatus.TODO);
        assertThat(created.getAttemptCount()).isEqualTo(0);
        assertThat(created.getSolvedAt()).isNull();

        DsaProblemResponse started = dsaProblemService.startProblem(userId, created.getId());
        assertThat(started.getStatus()).isEqualTo(DsaProblemStatus.IN_PROGRESS);

        DsaProblemResponse solved = dsaProblemService.solveProblem(userId, created.getId());
        assertThat(solved.getStatus()).isEqualTo(DsaProblemStatus.SOLVED);
        assertThat(solved.getSolvedAt()).isNotNull();

        Instant firstSolvedAt = solved.getSolvedAt();

        DsaProblemResponse revisited = dsaProblemService.revisitProblem(userId, created.getId());
        assertThat(revisited.getStatus()).isEqualTo(DsaProblemStatus.REVISIT);
        assertThat(revisited.getSolvedAt()).isNotNull();
        assertThat(revisited.getSolvedAt().toEpochMilli()).isEqualTo(firstSolvedAt.toEpochMilli());

        DsaProblemResponse restarted = dsaProblemService.startProblem(userId, created.getId());
        assertThat(restarted.getStatus()).isEqualTo(DsaProblemStatus.IN_PROGRESS);

        DsaProblemResponse solvedAgain = dsaProblemService.solveProblem(userId, created.getId());
        assertThat(solvedAgain.getStatus()).isEqualTo(DsaProblemStatus.SOLVED);

        dsaProblemService.archiveProblem(userId, created.getId());

        assertThatThrownBy(() -> dsaProblemService.getProblem(userId, created.getId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("DSA problem not found with id: " + created.getId());
    }

    @Test
    void idorProtection_preventsCrossUserAccessToDsaProblemsTasksAndGoals() {
        Long userA = 700L;
        Long userB = 800L;

        TaskResponse taskA = taskService.createTask(userA, new CreateTaskRequest("User A Task", "Desc", TaskPriority.HIGH, null, null));
        GoalResponse goalA = goalService.createGoal(userA, new CreateGoalRequest("User A Goal", "Desc", GoalType.DAILY, 10, 0, null));

        CreateDsaProblemRequest reqA = new CreateDsaProblemRequest("User A Problem", "Desc", DsaPlatform.LEETCODE, null, DsaDifficulty.MEDIUM, DsaTopic.ARRAY, 30, null, null, null);
        DsaProblemResponse problemA = dsaProblemService.createProblem(userA, reqA);

        assertThatThrownBy(() -> dsaProblemService.getProblem(userB, problemA.getId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("DSA problem not found with id: " + problemA.getId());

        assertThatThrownBy(() -> dsaProblemService.updateProblem(userB, problemA.getId(), new UpdateDsaProblemRequest("Hacked Title", "Desc", DsaPlatform.LEETCODE, null, DsaDifficulty.EASY, DsaTopic.ARRAY, 10, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThatThrownBy(() -> dsaProblemService.archiveProblem(userB, problemA.getId()))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThatThrownBy(() -> dsaProblemService.incrementAttempt(userB, problemA.getId()))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThatThrownBy(() -> dsaProblemService.startProblem(userB, problemA.getId()))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThatThrownBy(() -> dsaProblemService.solveProblem(userB, problemA.getId()))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThatThrownBy(() -> dsaProblemService.revisitProblem(userB, problemA.getId()))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThatThrownBy(() -> dsaProblemService.createProblem(userB, new CreateDsaProblemRequest("User B with User A Task", "Desc", DsaPlatform.LEETCODE, null, DsaDifficulty.EASY, DsaTopic.ARRAY, null, null, taskA.getId(), null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Task not found with id: " + taskA.getId());

        assertThatThrownBy(() -> dsaProblemService.createProblem(userB, new CreateDsaProblemRequest("User B with User A Goal", "Desc", DsaPlatform.LEETCODE, null, DsaDifficulty.EASY, DsaTopic.ARRAY, null, null, null, goalA.getId())))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Goal not found with id: " + goalA.getId());
    }

    @Test
    void attemptTracking_incrementsAttemptCount() {
        Long userId = 900L;
        DsaProblemResponse problem = dsaProblemService.createProblem(userId, new CreateDsaProblemRequest(
                "Valid Sudoku", "Desc", DsaPlatform.LEETCODE, null, DsaDifficulty.MEDIUM, DsaTopic.HASHING, 25, null, null, null
        ));

        assertThat(problem.getAttemptCount()).isEqualTo(0);

        DsaProblemResponse attempt1 = dsaProblemService.incrementAttempt(userId, problem.getId());
        assertThat(attempt1.getAttemptCount()).isEqualTo(1);

        DsaProblemResponse attempt2 = dsaProblemService.incrementAttempt(userId, problem.getId());
        assertThat(attempt2.getAttemptCount()).isEqualTo(2);
    }

    @Test
    void duplicateProblemUrl_preventsDuplicateUrlForSamePlatform() {
        Long userId = 1000L;
        String url = "https://leetcode.com/problems/climbing-stairs";

        dsaProblemService.createProblem(userId, new CreateDsaProblemRequest(
                "Climbing Stairs 1", "Desc", DsaPlatform.LEETCODE, url, DsaDifficulty.EASY, DsaTopic.DYNAMIC_PROGRAMMING, 10, null, null, null
        ));

        assertThatThrownBy(() -> dsaProblemService.createProblem(userId, new CreateDsaProblemRequest(
                "Climbing Stairs Duplicate", "Desc", DsaPlatform.LEETCODE, url, DsaDifficulty.EASY, DsaTopic.DYNAMIC_PROGRAMMING, 10, null, null, null
        ))).isInstanceOf(DuplicateDsaProblemException.class)
                .hasMessageContaining("Problem URL is already tracked");

        DsaProblemResponse otherPlatform = dsaProblemService.createProblem(userId, new CreateDsaProblemRequest(
                "Climbing Stairs Other Platform", "Desc", DsaPlatform.GEEKSFORGEEKS, url, DsaDifficulty.EASY, DsaTopic.DYNAMIC_PROGRAMMING, 10, null, null, null
        ));
        assertThat(otherPlatform).isNotNull();
    }

    @Test
    void filteringAndPagination_returnsExpectedProblems() {
        Long userId = 1100L;

        dsaProblemService.createProblem(userId, new CreateDsaProblemRequest("Easy Array", "Desc", DsaPlatform.LEETCODE, null, DsaDifficulty.EASY, DsaTopic.ARRAY, 15, null, null, null));
        dsaProblemService.createProblem(userId, new CreateDsaProblemRequest("Medium String", "Desc", DsaPlatform.LEETCODE, null, DsaDifficulty.MEDIUM, DsaTopic.STRING, 25, null, null, null));
        dsaProblemService.createProblem(userId, new CreateDsaProblemRequest("Hard Graph", "Desc", DsaPlatform.CODEFORCES, null, DsaDifficulty.HARD, DsaTopic.GRAPH, 45, null, null, null));

        PageResponse<DsaProblemResponse> mediumProblems = dsaProblemService.listProblems(userId, DsaDifficulty.MEDIUM, null, null, null, 0, 20);
        assertThat(mediumProblems.getContent()).hasSize(1);
        assertThat(mediumProblems.getContent().get(0).getTitle()).isEqualTo("Medium String");

        PageResponse<DsaProblemResponse> arrayProblems = dsaProblemService.listProblems(userId, null, DsaTopic.ARRAY, null, null, 0, 20);
        assertThat(arrayProblems.getContent()).hasSize(1);
        assertThat(arrayProblems.getContent().get(0).getTitle()).isEqualTo("Easy Array");

        PageResponse<DsaProblemResponse> combinedFilter = dsaProblemService.listProblems(userId, DsaDifficulty.MEDIUM, DsaTopic.STRING, DsaPlatform.LEETCODE, null, 0, 20);
        assertThat(combinedFilter.getContent()).hasSize(1);
    }

    @Test
    void dailyProgressAndStatistics_calculatesDynamically() {
        Long userId = 1200L;

        DsaProblemResponse p1 = dsaProblemService.createProblem(userId, new CreateDsaProblemRequest("E1", "D1", DsaPlatform.LEETCODE, null, DsaDifficulty.EASY, DsaTopic.ARRAY, 10, null, null, null));
        DsaProblemResponse p2 = dsaProblemService.createProblem(userId, new CreateDsaProblemRequest("E2", "D2", DsaPlatform.LEETCODE, null, DsaDifficulty.EASY, DsaTopic.STRING, 15, null, null, null));
        DsaProblemResponse p3 = dsaProblemService.createProblem(userId, new CreateDsaProblemRequest("M1", "D3", DsaPlatform.LEETCODE, null, DsaDifficulty.MEDIUM, DsaTopic.TREE, 30, null, null, null));
        DsaProblemResponse p4 = dsaProblemService.createProblem(userId, new CreateDsaProblemRequest("M2", "D4", DsaPlatform.LEETCODE, null, DsaDifficulty.MEDIUM, DsaTopic.GRAPH, 25, null, null, null));
        DsaProblemResponse p5 = dsaProblemService.createProblem(userId, new CreateDsaProblemRequest("H1", "D5", DsaPlatform.CODEFORCES, null, DsaDifficulty.HARD, DsaTopic.DYNAMIC_PROGRAMMING, 50, null, null, null));
        DsaProblemResponse p6 = dsaProblemService.createProblem(userId, new CreateDsaProblemRequest("T1", "D6", DsaPlatform.LEETCODE, null, DsaDifficulty.EASY, DsaTopic.MATH, 5, null, null, null));

        dsaProblemService.incrementAttempt(userId, p1.getId());
        dsaProblemService.incrementAttempt(userId, p2.getId());

        dsaProblemService.solveProblem(userId, p1.getId());
        dsaProblemService.solveProblem(userId, p2.getId());
        dsaProblemService.solveProblem(userId, p3.getId());
        dsaProblemService.solveProblem(userId, p5.getId());
        dsaProblemService.revisitProblem(userId, p4.getId());

        DsaStatisticsResponse stats = dsaProblemService.getStatistics(userId);
        assertThat(stats.getTotalProblems()).isEqualTo(6);
        assertThat(stats.getSolvedProblems()).isEqualTo(4);
        assertThat(stats.getRevisitProblems()).isEqualTo(1);
        assertThat(stats.getEasySolved()).isEqualTo(2);
        assertThat(stats.getMediumSolved()).isEqualTo(1);
        assertThat(stats.getHardSolved()).isEqualTo(1);
        assertThat(stats.getTotalTimeSpentMinutes()).isEqualTo(135);
        assertThat(stats.getTotalAttempts()).isEqualTo(2);

        DailyDsaProgressResponse daily = dsaProblemService.getDailyProgress(userId, java.time.LocalDate.now(java.time.ZoneOffset.UTC));
        assertThat(daily.getProblemsSolved()).isEqualTo(4);
    }
}
