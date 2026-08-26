package com.devsphere.user.controller;

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
import com.devsphere.user.entity.TaskPriority;
import com.devsphere.user.entity.TaskStatus;
import com.devsphere.user.exception.DuplicatePlannerEntryException;
import com.devsphere.user.exception.GlobalExceptionHandler;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.service.PlannerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = PlannerController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@Import(GlobalExceptionHandler.class)
class PlannerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PlannerService plannerService;

    @MockBean(name = "userSecurity")
    private com.devsphere.user.security.UserSecurity userSecurity;

    @MockBean
    private com.devsphere.user.security.JwtValidator jwtValidator;

    @Test
    void createPlannerEntry_withValidRequest_returns201Created() throws Exception {
        Long userId = 100L;
        LocalDate date = LocalDate.of(2026, 8, 26);
        CreatePlannerEntryRequest request = new CreatePlannerEntryRequest(
                5L, date, LocalTime.of(9, 0), LocalTime.of(10, 0), 60, 1
        );

        PlannerEntry entry = new PlannerEntry(userId, 5L, date, 1);
        entry.setId(10L);
        entry.setStartTime(LocalTime.of(9, 0));
        entry.setEndTime(LocalTime.of(10, 0));
        entry.setPlannedMinutes(60);

        when(plannerService.createPlannerEntry(eq(userId), any(CreatePlannerEntryRequest.class)))
                .thenReturn(new PlannerEntryResponse(entry));

        mockMvc.perform(post("/api/v1/planner/entries")
                        .header("X-Authenticated-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.taskId").value(5L))
                .andExpect(jsonPath("$.sortOrder").value(1))
                .andExpect(jsonPath("$.plannedMinutes").value(60));
    }

    @Test
    void createPlannerEntry_withMissingTaskId_returns400BadRequest() throws Exception {
        Long userId = 100L;
        CreatePlannerEntryRequest request = new CreatePlannerEntryRequest(
                null, LocalDate.of(2026, 8, 26), null, null, null, 1
        );

        mockMvc.perform(post("/api/v1/planner/entries")
                        .header("X-Authenticated-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void createPlannerEntry_withDuplicateTask_returns409Conflict() throws Exception {
        Long userId = 100L;
        LocalDate date = LocalDate.of(2026, 8, 26);
        CreatePlannerEntryRequest request = new CreatePlannerEntryRequest(
                5L, date, null, null, null, 1
        );

        when(plannerService.createPlannerEntry(eq(userId), any(CreatePlannerEntryRequest.class)))
                .thenThrow(new DuplicatePlannerEntryException("DUPLICATE_PLANNER_ENTRY", "Task already scheduled"));

        mockMvc.perform(post("/api/v1/planner/entries")
                        .header("X-Authenticated-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_PLANNER_ENTRY"));
    }

    @Test
    void getPlannerEntryById_whenFound_returns200OK() throws Exception {
        Long userId = 100L;
        Long entryId = 10L;
        PlannerEntry entry = new PlannerEntry(userId, 5L, LocalDate.of(2026, 8, 26), 1);
        entry.setId(entryId);

        when(plannerService.getPlannerEntry(userId, entryId)).thenReturn(new PlannerEntryResponse(entry));

        mockMvc.perform(get("/api/v1/planner/entries/{id}", entryId)
                        .header("X-Authenticated-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(entryId))
                .andExpect(jsonPath("$.taskId").value(5L));
    }

    @Test
    void getPlannerEntryById_whenNotFound_returns404NotFound() throws Exception {
        Long userId = 100L;
        Long entryId = 99L;

        when(plannerService.getPlannerEntry(userId, entryId))
                .thenThrow(new ResourceNotFoundException("RESOURCE_NOT_FOUND", "Planner entry not found with id: 99"));

        mockMvc.perform(get("/api/v1/planner/entries/{id}", entryId)
                        .header("X-Authenticated-User-Id", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void listPlannerEntries_returns200OK() throws Exception {
        Long userId = 100L;
        PlannerEntry entry = new PlannerEntry(userId, 5L, LocalDate.of(2026, 8, 26), 1);
        entry.setId(10L);
        PageResponse<PlannerEntryResponse> pageResponse = new PageResponse<>(List.of(new PlannerEntryResponse(entry)), 0, 20, 1, 1, true);

        when(plannerService.listPlannerEntries(eq(userId), any(), anyInt(), anyInt())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/planner/entries")
                        .header("X-Authenticated-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10L))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void updatePlannerEntry_returns200OK() throws Exception {
        Long userId = 100L;
        Long entryId = 10L;
        LocalDate date = LocalDate.of(2026, 8, 27);
        UpdatePlannerEntryRequest request = new UpdatePlannerEntryRequest(
                date, LocalTime.of(10, 0), LocalTime.of(11, 0), 60, 2
        );

        PlannerEntry entry = new PlannerEntry(userId, 5L, date, 2);
        entry.setId(entryId);
        entry.setStartTime(LocalTime.of(10, 0));
        entry.setEndTime(LocalTime.of(11, 0));
        entry.setPlannedMinutes(60);

        when(plannerService.updatePlannerEntry(eq(userId), eq(entryId), any(UpdatePlannerEntryRequest.class)))
                .thenReturn(new PlannerEntryResponse(entry));

        mockMvc.perform(put("/api/v1/planner/entries/{id}", entryId)
                        .header("X-Authenticated-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(entryId))
                .andExpect(jsonPath("$.sortOrder").value(2));
    }

    @Test
    void unschedulePlannerEntry_returns204NoContent() throws Exception {
        Long userId = 100L;
        Long entryId = 10L;

        mockMvc.perform(delete("/api/v1/planner/entries/{id}", entryId)
                        .header("X-Authenticated-User-Id", userId))
                .andExpect(status().isNoContent());

        verify(plannerService).unschedulePlannerEntry(userId, entryId);
    }

    @Test
    void reschedulePlannerEntry_returns200OK() throws Exception {
        Long userId = 100L;
        Long entryId = 10L;
        LocalDate date = LocalDate.of(2026, 8, 27);
        ReschedulePlannerEntryRequest request = new ReschedulePlannerEntryRequest(date, LocalTime.of(14, 0), LocalTime.of(15, 0));

        PlannerEntry entry = new PlannerEntry(userId, 5L, date, 1);
        entry.setId(entryId);

        when(plannerService.reschedulePlannerEntry(eq(userId), eq(entryId), any(ReschedulePlannerEntryRequest.class)))
                .thenReturn(new PlannerEntryResponse(entry));

        mockMvc.perform(patch("/api/v1/planner/entries/{id}/reschedule", entryId)
                        .header("X-Authenticated-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plannedDate").value("2026-08-27"));
    }

    @Test
    void getDailyPlanner_returns200OK() throws Exception {
        Long userId = 100L;
        LocalDate date = LocalDate.of(2026, 8, 26);

        PlannerEntry entry = new PlannerEntry(userId, 5L, date, 1);
        entry.setId(10L);

        Task task = new Task(userId, "Study Microservices", TaskPriority.HIGH);
        task.setId(5L);
        task.setStatus(TaskStatus.TODO);

        DailyPlannerItemResponse item = new DailyPlannerItemResponse(entry, task);
        DailyPlannerResponse response = new DailyPlannerResponse(date, List.of(item));

        when(plannerService.getDailyPlanner(userId, date)).thenReturn(response);

        mockMvc.perform(get("/api/v1/planner/days/{date}", date)
                        .header("X-Authenticated-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-08-26"))
                .andExpect(jsonPath("$.totalEntries").value(1))
                .andExpect(jsonPath("$.completedEntries").value(0))
                .andExpect(jsonPath("$.entries[0].taskTitle").value("Study Microservices"));
    }

    @Test
    void getTodayPlanner_returns200OK() throws Exception {
        Long userId = 100L;
        LocalDate today = LocalDate.now();

        DailyPlannerResponse response = new DailyPlannerResponse(today, List.of());

        when(plannerService.getTodayPlanner(userId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/planner/today")
                        .header("X-Authenticated-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value(today.toString()))
                .andExpect(jsonPath("$.totalEntries").value(0));
    }

    @Test
    void reorderDay_returns200OK() throws Exception {
        Long userId = 100L;
        LocalDate date = LocalDate.of(2026, 8, 26);
        List<ReorderPlannerEntryItem> request = List.of(
                new ReorderPlannerEntryItem(10L, 1),
                new ReorderPlannerEntryItem(11L, 2)
        );

        DailyPlannerResponse response = new DailyPlannerResponse(date, List.of());

        when(plannerService.reorderDay(eq(userId), eq(date), any())).thenReturn(response);

        mockMvc.perform(patch("/api/v1/planner/days/{date}/reorder", date)
                        .header("X-Authenticated-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-08-26"));
    }
}
