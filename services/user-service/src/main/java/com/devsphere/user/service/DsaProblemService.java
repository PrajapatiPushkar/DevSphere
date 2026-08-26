package com.devsphere.user.service;

import com.devsphere.user.dto.CreateDsaProblemRequest;
import com.devsphere.user.dto.DailyDsaProgressResponse;
import com.devsphere.user.dto.DsaProblemResponse;
import com.devsphere.user.dto.DsaStatisticsResponse;
import com.devsphere.user.dto.PageResponse;
import com.devsphere.user.dto.UpdateDsaProblemRequest;
import com.devsphere.user.entity.DsaDifficulty;
import com.devsphere.user.entity.DsaPlatform;
import com.devsphere.user.entity.DsaProblem;
import com.devsphere.user.entity.DsaProblemStatus;
import com.devsphere.user.entity.DsaTopic;
import com.devsphere.user.exception.DuplicateDsaProblemException;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.repository.DsaProblemRepository;
import com.devsphere.user.repository.GoalRepository;
import com.devsphere.user.repository.TaskRepository;
import com.devsphere.user.specification.DsaProblemSpecification;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DsaProblemService {

    private static final Logger log = LoggerFactory.getLogger(DsaProblemService.class);
    private static final int MAX_PAGE_SIZE = 100;

    private final DsaProblemRepository dsaProblemRepository;
    private final TaskRepository taskRepository;
    private final GoalRepository goalRepository;
    private final MeterRegistry meterRegistry;

    public DsaProblemService(DsaProblemRepository dsaProblemRepository,
                             TaskRepository taskRepository,
                             GoalRepository goalRepository) {
        this(dsaProblemRepository, taskRepository, goalRepository, new SimpleMeterRegistry());
    }

    @Autowired
    public DsaProblemService(DsaProblemRepository dsaProblemRepository,
                             TaskRepository taskRepository,
                             GoalRepository goalRepository,
                             MeterRegistry meterRegistry) {
        this.dsaProblemRepository = dsaProblemRepository;
        this.taskRepository = taskRepository;
        this.goalRepository = goalRepository;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public DsaProblemResponse createProblem(Long userId, CreateDsaProblemRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }

        validateTaskOwnership(userId, request.getTaskId());
        validateGoalOwnership(userId, request.getGoalId());

        if (request.getProblemUrl() != null && !request.getProblemUrl().isBlank()) {
            String cleanUrl = request.getProblemUrl().trim();
            if (dsaProblemRepository.existsByUserIdAndPlatformAndProblemUrlAndStatusNot(userId, request.getPlatform(), cleanUrl, DsaProblemStatus.ARCHIVED)) {
                throw new DuplicateDsaProblemException("DUPLICATE_DSA_PROBLEM",
                        "Problem URL is already tracked for platform: " + request.getPlatform());
            }
        }

        log.info("Creating DSA problem for userId: {}, title: {}", userId, request.getTitle());

        DsaProblem problem = new DsaProblem(userId, request.getTitle().trim(), request.getPlatform(), request.getDifficulty(), request.getTopic());
        problem.setDescription(request.getDescription());
        problem.setProblemUrl(request.getProblemUrl() != null ? request.getProblemUrl().trim() : null);
        problem.setTimeSpentMinutes(request.getTimeSpentMinutes() != null ? request.getTimeSpentMinutes() : 0);
        problem.setNotes(request.getNotes());
        problem.setTaskId(request.getTaskId());
        problem.setGoalId(request.getGoalId());

        DsaProblem saved = dsaProblemRepository.save(problem);
        meterRegistry.counter("devsphere_dsa_problems_created_total", "difficulty", request.getDifficulty().name(), "platform", request.getPlatform().name()).increment();

        return new DsaProblemResponse(saved);
    }

    @Transactional(readOnly = true)
    public DsaProblemResponse getProblem(Long userId, Long problemId) {
        DsaProblem problem = findActiveProblem(userId, problemId);
        return new DsaProblemResponse(problem);
    }

    @Transactional(readOnly = true)
    public PageResponse<DsaProblemResponse> listProblems(Long userId, DsaDifficulty difficulty, DsaTopic topic, DsaPlatform platform, DsaProblemStatus status, int page, int size) {
        int validatedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int validatedPage = Math.max(page, 0);

        Pageable pageable = PageRequest.of(validatedPage, validatedSize);

        Specification<DsaProblem> spec = DsaProblemSpecification.filterProblems(userId, difficulty, topic, platform, status);
        Page<DsaProblem> problemPage = dsaProblemRepository.findAll(spec, pageable);

        return PageResponse.fromPage(problemPage.map(DsaProblemResponse::new));
    }

    @Transactional
    public DsaProblemResponse updateProblem(Long userId, Long problemId, UpdateDsaProblemRequest request) {
        DsaProblem problem = findActiveProblem(userId, problemId);

        validateTaskOwnership(userId, request.getTaskId());
        validateGoalOwnership(userId, request.getGoalId());

        if (request.getProblemUrl() != null && !request.getProblemUrl().isBlank()) {
            String cleanUrl = request.getProblemUrl().trim();
            if (!cleanUrl.equalsIgnoreCase(problem.getProblemUrl()) &&
                    dsaProblemRepository.existsByUserIdAndPlatformAndProblemUrlAndStatusNot(userId, request.getPlatform(), cleanUrl, DsaProblemStatus.ARCHIVED)) {
                throw new DuplicateDsaProblemException("DUPLICATE_DSA_PROBLEM",
                        "Problem URL is already tracked for platform: " + request.getPlatform());
            }
        }

        problem.setTitle(request.getTitle().trim());
        problem.setDescription(request.getDescription());
        problem.setPlatform(request.getPlatform());
        problem.setProblemUrl(request.getProblemUrl() != null ? request.getProblemUrl().trim() : null);
        problem.setDifficulty(request.getDifficulty());
        problem.setTopic(request.getTopic());
        problem.setTimeSpentMinutes(request.getTimeSpentMinutes() != null ? request.getTimeSpentMinutes() : 0);
        problem.setNotes(request.getNotes());
        problem.setTaskId(request.getTaskId());
        problem.setGoalId(request.getGoalId());

        DsaProblem updated = dsaProblemRepository.save(problem);
        return new DsaProblemResponse(updated);
    }

    @Transactional
    public void archiveProblem(Long userId, Long problemId) {
        DsaProblem problem = findActiveProblem(userId, problemId);
        problem.setStatus(DsaProblemStatus.ARCHIVED);
        dsaProblemRepository.save(problem);
    }

    @Transactional
    public DsaProblemResponse incrementAttempt(Long userId, Long problemId) {
        DsaProblem problem = findActiveProblem(userId, problemId);
        problem.setAttemptCount(problem.getAttemptCount() + 1);

        DsaProblem updated = dsaProblemRepository.save(problem);
        meterRegistry.counter("devsphere_dsa_attempts_total", "difficulty", problem.getDifficulty().name(), "platform", problem.getPlatform().name()).increment();

        return new DsaProblemResponse(updated);
    }

    @Transactional
    public DsaProblemResponse startProblem(Long userId, Long problemId) {
        DsaProblem problem = findActiveProblem(userId, problemId);

        if (problem.getStatus() == DsaProblemStatus.SOLVED) {
            throw new IllegalArgumentException("Cannot transition SOLVED problem directly to IN_PROGRESS. Mark as REVISIT first.");
        }

        problem.setStatus(DsaProblemStatus.IN_PROGRESS);
        DsaProblem updated = dsaProblemRepository.save(problem);

        return new DsaProblemResponse(updated);
    }

    @Transactional
    public DsaProblemResponse solveProblem(Long userId, Long problemId) {
        DsaProblem problem = findActiveProblem(userId, problemId);

        problem.setStatus(DsaProblemStatus.SOLVED);
        problem.setSolvedAt(Instant.now());

        DsaProblem updated = dsaProblemRepository.save(problem);
        meterRegistry.counter("devsphere_dsa_problems_solved_total", "difficulty", problem.getDifficulty().name(), "platform", problem.getPlatform().name()).increment();

        return new DsaProblemResponse(updated);
    }

    @Transactional
    public DsaProblemResponse revisitProblem(Long userId, Long problemId) {
        DsaProblem problem = findActiveProblem(userId, problemId);

        problem.setStatus(DsaProblemStatus.REVISIT);
        // Preserves existing solvedAt timestamp

        DsaProblem updated = dsaProblemRepository.save(problem);
        meterRegistry.counter("devsphere_dsa_problems_revisited_total", "difficulty", problem.getDifficulty().name(), "platform", problem.getPlatform().name()).increment();

        return new DsaProblemResponse(updated);
    }

    @Transactional(readOnly = true)
    public DailyDsaProgressResponse getDailyProgress(Long userId, LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        Instant startOfDay = targetDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endOfDay = targetDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusMillis(1);

        List<DsaProblem> solvedOnDate = dsaProblemRepository.findAllByUserIdAndSolvedAtBetween(userId, startOfDay, endOfDay);

        int problemsSolved = solvedOnDate.size();
        int totalAttempts = solvedOnDate.stream().mapToInt(DsaProblem::getAttemptCount).sum();
        int timeSpentMinutes = solvedOnDate.stream().filter(p -> p.getTimeSpentMinutes() != null).mapToInt(DsaProblem::getTimeSpentMinutes).sum();

        return new DailyDsaProgressResponse(targetDate, problemsSolved, totalAttempts, timeSpentMinutes);
    }

    @Transactional(readOnly = true)
    public DsaStatisticsResponse getStatistics(Long userId) {
        List<DsaProblem> activeProblems = dsaProblemRepository.findAllByUserIdAndStatusNot(userId, DsaProblemStatus.ARCHIVED);

        int totalProblems = activeProblems.size();
        int solvedProblems = (int) activeProblems.stream().filter(p -> p.getStatus() == DsaProblemStatus.SOLVED).count();
        int inProgressProblems = (int) activeProblems.stream().filter(p -> p.getStatus() == DsaProblemStatus.IN_PROGRESS).count();
        int revisitProblems = (int) activeProblems.stream().filter(p -> p.getStatus() == DsaProblemStatus.REVISIT).count();

        int easySolved = (int) activeProblems.stream().filter(p -> p.getStatus() == DsaProblemStatus.SOLVED && p.getDifficulty() == DsaDifficulty.EASY).count();
        int mediumSolved = (int) activeProblems.stream().filter(p -> p.getStatus() == DsaProblemStatus.SOLVED && p.getDifficulty() == DsaDifficulty.MEDIUM).count();
        int hardSolved = (int) activeProblems.stream().filter(p -> p.getStatus() == DsaProblemStatus.SOLVED && p.getDifficulty() == DsaDifficulty.HARD).count();

        int totalTimeSpentMinutes = activeProblems.stream().filter(p -> p.getTimeSpentMinutes() != null).mapToInt(DsaProblem::getTimeSpentMinutes).sum();
        int totalAttempts = activeProblems.stream().filter(p -> p.getAttemptCount() != null).mapToInt(DsaProblem::getAttemptCount).sum();

        return new DsaStatisticsResponse(totalProblems, solvedProblems, inProgressProblems, revisitProblems, easySolved, mediumSolved, hardSolved, totalTimeSpentMinutes, totalAttempts);
    }

    private DsaProblem findActiveProblem(Long userId, Long problemId) {
        DsaProblem problem = dsaProblemRepository.findByIdAndUserId(problemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("RESOURCE_NOT_FOUND",
                        "DSA problem not found with id: " + problemId));

        if (problem.getStatus() == DsaProblemStatus.ARCHIVED) {
            throw new ResourceNotFoundException("RESOURCE_NOT_FOUND",
                    "DSA problem not found with id: " + problemId);
        }

        return problem;
    }

    private void validateTaskOwnership(Long userId, Long taskId) {
        if (taskId != null) {
            taskRepository.findByIdAndUserId(taskId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("RESOURCE_NOT_FOUND",
                            "Task not found with id: " + taskId));
        }
    }

    private void validateGoalOwnership(Long userId, Long goalId) {
        if (goalId != null) {
            goalRepository.findByIdAndUserId(goalId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("RESOURCE_NOT_FOUND",
                            "Goal not found with id: " + goalId));
        }
    }
}
