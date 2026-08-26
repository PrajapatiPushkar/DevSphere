package com.devsphere.user.controller;

import com.devsphere.user.dto.CreateGoalRequest;
import com.devsphere.user.dto.GoalResponse;
import com.devsphere.user.dto.PageResponse;
import com.devsphere.user.dto.UpdateGoalRequest;
import com.devsphere.user.entity.GoalStatus;
import com.devsphere.user.entity.GoalType;
import com.devsphere.user.exception.GlobalExceptionHandler;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.service.GoalService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = GoalController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@Import(GlobalExceptionHandler.class)
class GoalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GoalService goalService;

    @MockBean(name = "userSecurity")
    private com.devsphere.user.security.UserSecurity userSecurity;

    @MockBean
    private com.devsphere.user.security.JwtValidator jwtValidator;

    @Test
    void createGoal_withValidRequest_returns21Created() throws Exception {
        Long userId = 100L;
        CreateGoalRequest request = new CreateGoalRequest("Solve 300 DSA Problems", "Interview Prep", GoalType.LONG_TERM, 300, 45, LocalDate.of(2027, 1, 1));
        GoalResponse response = new GoalResponse(1L, userId, "Solve 300 DSA Problems", "Interview Prep", GoalType.LONG_TERM, GoalStatus.ACTIVE, 300, 45, LocalDate.of(2027, 1, 1), null, Instant.now(), Instant.now());

        when(goalService.createGoal(eq(userId), any(CreateGoalRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/goals")
                        .header("X-Authenticated-User-Id", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/goals/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Solve 300 DSA Problems"))
                .andExpect(jsonPath("$.progressPercentage").value(15.0));
    }

    @Test
    void createGoal_withBlankTitle_returns400BadRequest() throws Exception {
        CreateGoalRequest request = new CreateGoalRequest("", "Desc", GoalType.DAILY, 10, 0, null);

        mockMvc.perform(post("/api/v1/goals")
                        .header("X-Authenticated-User-Id", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message", containsString("Title is required")));
    }

    @Test
    void getGoals_returns200OkWithPage() throws Exception {
        Long userId = 100L;
        GoalResponse response = new GoalResponse(1L, userId, "Goal 1", "Desc", GoalType.WEEKLY, GoalStatus.ACTIVE, 10, 2, null, null, Instant.now(), Instant.now());
        PageResponse<GoalResponse> pageResponse = new PageResponse<>(List.of(response), 0, 20, 1, 1, true);

        when(goalService.getGoals(eq(userId), any(), any(), anyInt(), anyInt())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/goals")
                        .header("X-Authenticated-User-Id", "100")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getGoalById_returns200OkWhenOwned() throws Exception {
        Long userId = 100L;
        Long goalId = 1L;
        GoalResponse response = new GoalResponse(goalId, userId, "Goal 1", "Desc", GoalType.DAILY, GoalStatus.ACTIVE, 1, 0, null, null, Instant.now(), Instant.now());

        when(goalService.getGoalById(userId, goalId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/goals/1")
                        .header("X-Authenticated-User-Id", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getGoalById_returns404NotFoundWhenNotOwned() throws Exception {
        Long userId = 100L;
        Long goalId = 999L;

        when(goalService.getGoalById(userId, goalId))
                .thenThrow(new ResourceNotFoundException("RESOURCE_NOT_FOUND", "Goal not found with id: 999"));

        mockMvc.perform(get("/api/v1/goals/999")
                        .header("X-Authenticated-User-Id", "100"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void updateGoal_returns200Ok() throws Exception {
        Long userId = 100L;
        Long goalId = 1L;
        UpdateGoalRequest request = new UpdateGoalRequest("Updated Goal", "Desc", GoalType.DAILY, GoalStatus.COMPLETED, 10, 10, null);
        GoalResponse response = new GoalResponse(goalId, userId, "Updated Goal", "Desc", GoalType.DAILY, GoalStatus.COMPLETED, 10, 10, null, Instant.now(), Instant.now(), Instant.now());

        when(goalService.updateGoal(eq(userId), eq(goalId), any(UpdateGoalRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/goals/1")
                        .header("X-Authenticated-User-Id", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void deleteGoal_returns204NoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/goals/1")
                        .header("X-Authenticated-User-Id", "100"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteGoal_returns404NotFoundWhenNotOwned() throws Exception {
        doThrow(new ResourceNotFoundException("RESOURCE_NOT_FOUND", "Goal not found with id: 999"))
                .when(goalService).archiveGoal(100L, 999L);

        mockMvc.perform(delete("/api/v1/goals/999")
                        .header("X-Authenticated-User-Id", "100"))
                .andExpect(status().isNotFound());
    }
}
