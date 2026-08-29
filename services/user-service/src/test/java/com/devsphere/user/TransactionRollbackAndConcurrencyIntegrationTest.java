package com.devsphere.user;

import com.devsphere.user.dto.CreateGoalRequest;
import com.devsphere.user.dto.CreateTaskRequest;
import com.devsphere.user.dto.GoalResponse;
import com.devsphere.user.dto.ResumeProfileRequest;
import com.devsphere.user.dto.ResumeProfileResponse;
import com.devsphere.user.dto.ResumeVersionResponse;
import com.devsphere.user.entity.Goal;
import com.devsphere.user.entity.GoalStatus;
import com.devsphere.user.entity.GoalType;
import com.devsphere.user.entity.ResumeProfile;
import com.devsphere.user.entity.ResumeStatus;
import com.devsphere.user.entity.ResumeTemplate;
import com.devsphere.user.entity.ResumeVersion;
import com.devsphere.user.entity.ResumeVersionStatus;
import com.devsphere.user.entity.Task;
import com.devsphere.user.entity.TaskPriority;
import com.devsphere.user.entity.UserProfile;
import com.devsphere.user.exception.GlobalExceptionHandler;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.repository.GoalRepository;
import com.devsphere.user.repository.ResumeProfileRepository;
import com.devsphere.user.repository.ResumeSectionRepository;
import com.devsphere.user.repository.ResumeVersionRepository;
import com.devsphere.user.repository.TaskRepository;
import com.devsphere.user.repository.UserProfileRepository;
import com.devsphere.user.service.GoalService;
import com.devsphere.user.service.ResumeProfileService;
import com.devsphere.user.service.ResumeVersionService;
import com.devsphere.user.service.TaskService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class TransactionRollbackAndConcurrencyIntegrationTest {

    @Autowired
    private ResumeProfileService resumeProfileService;

    @Autowired
    private ResumeProfileRepository resumeProfileRepository;

    @Autowired
    private ResumeSectionRepository resumeSectionRepository;

    @Autowired
    private ResumeVersionService resumeVersionService;

    @Autowired
    private ResumeVersionRepository resumeVersionRepository;

    @Autowired
    private GoalService goalService;

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void cleanDatabase() {
        resumeVersionRepository.deleteAll();
        resumeSectionRepository.deleteAll();
        resumeProfileRepository.deleteAll();
        taskRepository.deleteAll();
        goalRepository.deleteAll();
        userProfileRepository.deleteAll();
    }

    @Test
    void createResumeProfile_atomicMultiStepTransactionSuccess() {
        Long userId = 701L;
        ResumeProfileRequest request = new ResumeProfileRequest();
        request.setName("Fullstack Engineer");
        request.setTargetRole("Senior Developer");
        request.setTemplate(ResumeTemplate.MODERN);

        ResumeProfileResponse response = resumeProfileService.createResumeProfile(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();

        // Verify parent profile and all 6 default sections created
        assertThat(resumeProfileRepository.findById(response.getId())).isPresent();
        assertThat(resumeSectionRepository.findAllByResumeProfileIdOrderByDisplayOrderAscIdAsc(response.getId())).hasSize(6);
    }

    @Test
    void optimisticLockingConflict_onStaleVersionUpdate_throwsOptimisticLockingFailureException() {
        Long userId = 702L;
        Goal goal = new Goal(userId, "Original Goal", GoalType.DAILY);
        Goal saved = goalRepository.saveAndFlush(goal);

        // Fetch two separate entity instances representing concurrent requests
        Goal instanceA = goalRepository.findById(saved.getId()).orElseThrow();
        Goal instanceB = goalRepository.findById(saved.getId()).orElseThrow();

        // Update instance A
        instanceA.setTitle("Updated by Request A");
        goalRepository.saveAndFlush(instanceA);

        // Attempting to update instance B with stale version should fail with ObjectOptimisticLockingFailureException
        instanceB.setTitle("Updated by Request B");

        assertThatThrownBy(() -> goalRepository.saveAndFlush(instanceB))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    void globalExceptionHandler_convertsOptimisticLockingFailureTo409Conflict() {
        ObjectOptimisticLockingFailureException ex = new ObjectOptimisticLockingFailureException(Goal.class, 100L);
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/v1/goals/100");

        ResponseEntity<?> response = globalExceptionHandler.handleOptimisticLockingFailure(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void globalExceptionHandler_convertsDataIntegrityViolationTo409Conflict() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Constraint violation: UNIQUE");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/user-profile");

        ResponseEntity<?> response = globalExceptionHandler.handleDataIntegrityViolation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void databaseConstraintViolation_onDuplicateProfileUserId_throwsDataIntegrityViolation() {
        Long userId = 703L;
        UserProfile p1 = new UserProfile(userId);
        userProfileRepository.saveAndFlush(p1);

        UserProfile p2 = new UserProfile(userId);
        assertThatThrownBy(() -> userProfileRepository.saveAndFlush(p2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void publishVersion_pessimisticLockingGuaranteesSinglePublishedVersion() throws Exception {
        Long userId = 704L;
        ResumeProfileRequest req = new ResumeProfileRequest();
        req.setName("Backend Resume");
        req.setTargetRole("Java Engineer");
        req.setTemplate(ResumeTemplate.PROFESSIONAL);

        ResumeProfileResponse profile = resumeProfileService.createResumeProfile(userId, req);
        Long resumeId = profile.getId();

        ResumeVersionResponse v1 = resumeVersionService.createVersion(resumeId, userId, null);
        ResumeVersionResponse v2 = resumeVersionService.createVersion(resumeId, userId, null);

        // Concurrently attempt to publish v1 and v2
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        CompletableFuture<ResumeVersionResponse> f1 = CompletableFuture.supplyAsync(() -> {
            try {
                readyLatch.countDown();
                startLatch.await();
                return resumeVersionService.publishVersion(resumeId, v1.getId(), userId);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);

        CompletableFuture<ResumeVersionResponse> f2 = CompletableFuture.supplyAsync(() -> {
            try {
                readyLatch.countDown();
                startLatch.await();
                return resumeVersionService.publishVersion(resumeId, v2.getId(), userId);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);

        readyLatch.await();
        startLatch.countDown();

        try {
            CompletableFuture.allOf(f1, f2).join();
        } catch (Exception ignored) {
            // One or both futures complete
        }

        executor.shutdown();

        // Verify database state: exactly ONE version has status PUBLISHED
        List<ResumeVersion> publishedVersions = resumeVersionRepository
                .findAllByResumeProfileIdAndUserIdOrderByVersionNumberDesc(resumeId, userId)
                .stream()
                .filter(v -> v.getStatus() == ResumeVersionStatus.PUBLISHED)
                .toList();

        assertThat(publishedVersions).hasSize(1);
    }

    @Test
    void transaction_preservesIdorUserOwnershipIsolation() {
        Long userA = 801L;
        Long userB = 802L;

        GoalResponse goalA = goalService.createGoal(userA, new CreateGoalRequest(
                "User A Goal", "Desc", GoalType.DAILY, 5, 0, LocalDate.now()
        ));

        // User B attempting to access or update User A's goal throws ResourceNotFoundException
        assertThatThrownBy(() -> goalService.getGoalById(userB, goalA.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
