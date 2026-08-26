package com.devsphere.user.service;

import com.devsphere.user.dto.CreatePlannerEntryRequest;
import com.devsphere.user.dto.DailyPlannerItemResponse;
import com.devsphere.user.dto.DailyPlannerResponse;
import com.devsphere.user.dto.PageResponse;
import com.devsphere.user.dto.PlannerEntryResponse;
import com.devsphere.user.dto.ReorderPlannerEntryItem;
import com.devsphere.user.dto.ReschedulePlannerEntryRequest;
import com.devsphere.user.dto.UpdatePlannerEntryRequest;
import com.devsphere.user.entity.PlannerEntry;
import com.devsphere.user.entity.Task;
import com.devsphere.user.entity.TaskStatus;
import com.devsphere.user.exception.DuplicatePlannerEntryException;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.repository.PlannerEntryRepository;
import com.devsphere.user.repository.TaskRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlannerService {

    private static final Logger log = LoggerFactory.getLogger(PlannerService.class);
    private static final int MAX_PAGE_SIZE = 100;

    private final PlannerEntryRepository plannerEntryRepository;
    private final TaskRepository taskRepository;
    private final MeterRegistry meterRegistry;

    public PlannerService(PlannerEntryRepository plannerEntryRepository, TaskRepository taskRepository) {
        this(plannerEntryRepository, taskRepository, new SimpleMeterRegistry());
    }

    @Autowired
    public PlannerService(PlannerEntryRepository plannerEntryRepository,
                          TaskRepository taskRepository,
                          MeterRegistry meterRegistry) {
        this.plannerEntryRepository = plannerEntryRepository;
        this.taskRepository = taskRepository;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public PlannerEntryResponse createPlannerEntry(Long userId, CreatePlannerEntryRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }

        Task task = taskRepository.findByIdAndUserId(request.getTaskId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("RESOURCE_NOT_FOUND",
                        "Task not found with id: " + request.getTaskId()));

        if (task.getStatus() == TaskStatus.ARCHIVED || task.getStatus() == TaskStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot schedule an archived or cancelled task");
        }

        validateTimeSlot(request.getStartTime(), request.getEndTime());

        if (plannerEntryRepository.existsByUserIdAndTaskIdAndPlannedDate(userId, request.getTaskId(), request.getPlannedDate())) {
            throw new DuplicatePlannerEntryException("DUPLICATE_PLANNER_ENTRY",
                    "Task " + request.getTaskId() + " is already scheduled for date: " + request.getPlannedDate());
        }

        log.info("Creating planner entry for userId: {}, taskId: {}, date: {}", userId, request.getTaskId(), request.getPlannedDate());

        PlannerEntry entry = new PlannerEntry(userId, request.getTaskId(), request.getPlannedDate(), request.getSortOrder());
        entry.setStartTime(request.getStartTime());
        entry.setEndTime(request.getEndTime());
        entry.setPlannedMinutes(request.getPlannedMinutes());

        PlannerEntry saved = plannerEntryRepository.save(entry);
        meterRegistry.counter("devsphere_planner_entries_created_total").increment();

        return new PlannerEntryResponse(saved);
    }

    @Transactional(readOnly = true)
    public PlannerEntryResponse getPlannerEntry(Long userId, Long entryId) {
        PlannerEntry entry = plannerEntryRepository.findByIdAndUserId(entryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("RESOURCE_NOT_FOUND",
                        "Planner entry not found with id: " + entryId));
        return new PlannerEntryResponse(entry);
    }

    @Transactional(readOnly = true)
    public PageResponse<PlannerEntryResponse> listPlannerEntries(Long userId, LocalDate date, int page, int size) {
        int validatedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int validatedPage = Math.max(page, 0);

        Pageable pageable = PageRequest.of(validatedPage, validatedSize,
                Sort.by("sortOrder").ascending()
                        .and(Sort.by("startTime").ascending())
                        .and(Sort.by("createdAt").ascending()));

        Page<PlannerEntry> entryPage;
        if (date != null) {
            entryPage = plannerEntryRepository.findAllByUserIdAndPlannedDate(userId, date, pageable);
        } else {
            entryPage = plannerEntryRepository.findAllByUserId(userId, pageable);
        }

        return PageResponse.fromPage(entryPage.map(PlannerEntryResponse::new));
    }

    @Transactional
    public PlannerEntryResponse updatePlannerEntry(Long userId, Long entryId, UpdatePlannerEntryRequest request) {
        PlannerEntry entry = plannerEntryRepository.findByIdAndUserId(entryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("RESOURCE_NOT_FOUND",
                        "Planner entry not found with id: " + entryId));

        validateTimeSlot(request.getStartTime(), request.getEndTime());

        if (!entry.getPlannedDate().equals(request.getPlannedDate()) &&
                plannerEntryRepository.existsByUserIdAndTaskIdAndPlannedDate(userId, entry.getTaskId(), request.getPlannedDate())) {
            throw new DuplicatePlannerEntryException("DUPLICATE_PLANNER_ENTRY",
                    "Task " + entry.getTaskId() + " is already scheduled for date: " + request.getPlannedDate());
        }

        entry.setPlannedDate(request.getPlannedDate());
        entry.setStartTime(request.getStartTime());
        entry.setEndTime(request.getEndTime());
        entry.setPlannedMinutes(request.getPlannedMinutes());
        entry.setSortOrder(request.getSortOrder());

        PlannerEntry updated = plannerEntryRepository.save(entry);
        return new PlannerEntryResponse(updated);
    }

    @Transactional
    public PlannerEntryResponse reschedulePlannerEntry(Long userId, Long entryId, ReschedulePlannerEntryRequest request) {
        PlannerEntry entry = plannerEntryRepository.findByIdAndUserId(entryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("RESOURCE_NOT_FOUND",
                        "Planner entry not found with id: " + entryId));

        validateTimeSlot(request.getStartTime(), request.getEndTime());

        if (!entry.getPlannedDate().equals(request.getPlannedDate()) &&
                plannerEntryRepository.existsByUserIdAndTaskIdAndPlannedDate(userId, entry.getTaskId(), request.getPlannedDate())) {
            throw new DuplicatePlannerEntryException("DUPLICATE_PLANNER_ENTRY",
                    "Task " + entry.getTaskId() + " is already scheduled for date: " + request.getPlannedDate());
        }

        entry.setPlannedDate(request.getPlannedDate());
        entry.setStartTime(request.getStartTime());
        entry.setEndTime(request.getEndTime());

        PlannerEntry updated = plannerEntryRepository.save(entry);
        meterRegistry.counter("devsphere_planner_entries_rescheduled_total").increment();

        return new PlannerEntryResponse(updated);
    }

    @Transactional
    public void unschedulePlannerEntry(Long userId, Long entryId) {
        PlannerEntry entry = plannerEntryRepository.findByIdAndUserId(entryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("RESOURCE_NOT_FOUND",
                        "Planner entry not found with id: " + entryId));

        plannerEntryRepository.delete(entry);
        meterRegistry.counter("devsphere_planner_entries_deleted_total").increment();
    }

    @Transactional(readOnly = true)
    public DailyPlannerResponse getDailyPlanner(Long userId, LocalDate date) {
        List<PlannerEntry> entries = plannerEntryRepository.findAllByUserIdAndPlannedDateOrderBySortOrderAscStartTimeAscCreatedAtAsc(userId, date);

        Set<Long> taskIds = entries.stream()
                .map(PlannerEntry::getTaskId)
                .collect(Collectors.toSet());

        Map<Long, Task> taskMap = taskRepository.findAllById(taskIds).stream()
                .filter(t -> Objects.equals(t.getUserId(), userId))
                .collect(Collectors.toMap(Task::getId, Function.identity()));

        List<DailyPlannerItemResponse> itemResponses = entries.stream()
                .map(entry -> new DailyPlannerItemResponse(entry, taskMap.get(entry.getTaskId())))
                .collect(Collectors.toList());

        return new DailyPlannerResponse(date, itemResponses);
    }

    @Transactional(readOnly = true)
    public DailyPlannerResponse getTodayPlanner(Long userId) {
        return getDailyPlanner(userId, LocalDate.now());
    }

    @Transactional
    public DailyPlannerResponse reorderDay(Long userId, LocalDate date, List<ReorderPlannerEntryItem> reorderItems) {
        if (reorderItems == null || reorderItems.isEmpty()) {
            throw new IllegalArgumentException("Reorder items list cannot be empty");
        }

        List<PlannerEntry> dayEntries = plannerEntryRepository.findAllByUserIdAndPlannedDateOrderBySortOrderAscStartTimeAscCreatedAtAsc(userId, date);

        Map<Long, PlannerEntry> entryMap = dayEntries.stream()
                .collect(Collectors.toMap(PlannerEntry::getId, Function.identity()));

        for (ReorderPlannerEntryItem item : reorderItems) {
            PlannerEntry entry = entryMap.get(item.getEntryId());
            if (entry == null) {
                throw new ResourceNotFoundException("RESOURCE_NOT_FOUND",
                        "Planner entry not found for date " + date + " with id: " + item.getEntryId());
            }
            entry.setSortOrder(item.getSortOrder());
        }

        plannerEntryRepository.saveAll(dayEntries);
        meterRegistry.counter("devsphere_planner_entries_reordered_total").increment();

        return getDailyPlanner(userId, date);
    }

    private void validateTimeSlot(LocalTime startTime, LocalTime endTime) {
        if (startTime != null && endTime != null) {
            if (!endTime.isAfter(startTime)) {
                throw new IllegalArgumentException("endTime must be strictly after startTime");
            }
        } else if (startTime != null || endTime != null) {
            throw new IllegalArgumentException("Both startTime and endTime must be provided together or both omitted");
        }
    }
}
