package com.devsphere.user.controller;

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
import com.devsphere.user.exception.GlobalExceptionHandler;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.service.DsaProblemService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
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

@WebMvcTest(value = DsaProblemController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@Import(GlobalExceptionHandler.class)
class DsaProblemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DsaProblemService dsaProblemService;

    @MockBean(name = "userSecurity")
    private com.devsphere.user.security.UserSecurity userSecurity;

    @MockBean
    private com.devsphere.user.security.JwtValidator jwtValidator;

    @Test
    void createProblem_withValidRequest_returns201Created() throws Exception {
        Long userId = 100L;
        CreateDsaProblemRequest request = new CreateDsaProblemRequest(
                "Two Sum", "Find two numbers", DsaPlatform.LEETCODE, "https://leetcode.com/problems/two-sum",
                DsaDifficulty.EASY, DsaTopic.ARRAY, 20, "Use hash map", null, null
        );

        DsaProblem problem = new DsaProblem(userId, "Two Sum", DsaPlatform.LEETCODE, DsaDifficulty.EASY, DsaTopic.ARRAY);
        problem.setId(1L);
        problem.setStatus(DsaProblemStatus.TODO);

        when(dsaProblemService.createProblem(eq(userId), any(CreateDsaProblemRequest.class)))
                .thenReturn(new DsaProblemResponse(problem));

        mockMvc.perform(post("/api/v1/dsa/problems")
                        .header("X-Authenticated-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Two Sum"))
                .andExpect(jsonPath("$.platform").value("LEETCODE"))
                .andExpect(jsonPath("$.difficulty").value("EASY"));
    }

    @Test
    void createProblem_withBlankTitle_returns400BadRequest() throws Exception {
        Long userId = 100L;
        CreateDsaProblemRequest request = new CreateDsaProblemRequest(
                "", "Invalid title", DsaPlatform.LEETCODE, null, DsaDifficulty.EASY, DsaTopic.ARRAY, null, null, null, null
        );

        mockMvc.perform(post("/api/v1/dsa/problems")
                        .header("X-Authenticated-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void createProblem_withDuplicateUrl_returns409Conflict() throws Exception {
        Long userId = 100L;
        CreateDsaProblemRequest request = new CreateDsaProblemRequest(
                "Two Sum", "Desc", DsaPlatform.LEETCODE, "https://leetcode.com/problems/two-sum", DsaDifficulty.EASY, DsaTopic.ARRAY, null, null, null, null
        );

        when(dsaProblemService.createProblem(eq(userId), any(CreateDsaProblemRequest.class)))
                .thenThrow(new DuplicateDsaProblemException("DUPLICATE_DSA_PROBLEM", "Problem URL already tracked"));

        mockMvc.perform(post("/api/v1/dsa/problems")
                        .header("X-Authenticated-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_DSA_PROBLEM"));
    }

    @Test
    void getProblemById_whenFound_returns200OK() throws Exception {
        Long userId = 100L;
        Long problemId = 1L;

        DsaProblem problem = new DsaProblem(userId, "3Sum", DsaPlatform.LEETCODE, DsaDifficulty.MEDIUM, DsaTopic.TWO_POINTERS);
        problem.setId(problemId);

        when(dsaProblemService.getProblem(userId, problemId)).thenReturn(new DsaProblemResponse(problem));

        mockMvc.perform(get("/api/v1/dsa/problems/{id}", problemId)
                        .header("X-Authenticated-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(problemId))
                .andExpect(jsonPath("$.title").value("3Sum"));
    }

    @Test
    void getProblemById_whenNotFound_returns404NotFound() throws Exception {
        Long userId = 100L;
        Long problemId = 99L;

        when(dsaProblemService.getProblem(userId, problemId))
                .thenThrow(new ResourceNotFoundException("RESOURCE_NOT_FOUND", "DSA problem not found with id: 99"));

        mockMvc.perform(get("/api/v1/dsa/problems/{id}", problemId)
                        .header("X-Authenticated-User-Id", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void listProblems_returns200OK() throws Exception {
        Long userId = 100L;
        DsaProblem problem = new DsaProblem(userId, "LRU Cache", DsaPlatform.LEETCODE, DsaDifficulty.HARD, DsaTopic.LINKED_LIST);
        problem.setId(10L);

        PageResponse<DsaProblemResponse> pageResponse = new PageResponse<>(List.of(new DsaProblemResponse(problem)), 0, 20, 1, 1, true);

        when(dsaProblemService.listProblems(eq(userId), any(), any(), any(), any(), anyInt(), anyInt(), anyString())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/dsa/problems")
                        .header("X-Authenticated-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10L))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void updateProblem_returns200OK() throws Exception {
        Long userId = 100L;
        Long problemId = 1L;
        UpdateDsaProblemRequest request = new UpdateDsaProblemRequest(
                "Updated 3Sum", "Updated desc", DsaPlatform.LEETCODE, null, DsaDifficulty.MEDIUM, DsaTopic.TWO_POINTERS, 35, "Updated notes", null, null
        );

        DsaProblem problem = new DsaProblem(userId, "Updated 3Sum", DsaPlatform.LEETCODE, DsaDifficulty.MEDIUM, DsaTopic.TWO_POINTERS);
        problem.setId(problemId);

        when(dsaProblemService.updateProblem(eq(userId), eq(problemId), any(UpdateDsaProblemRequest.class)))
                .thenReturn(new DsaProblemResponse(problem));

        mockMvc.perform(put("/api/v1/dsa/problems/{id}", problemId)
                        .header("X-Authenticated-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated 3Sum"));
    }

    @Test
    void archiveProblem_returns204NoContent() throws Exception {
        Long userId = 100L;
        Long problemId = 1L;

        mockMvc.perform(delete("/api/v1/dsa/problems/{id}", problemId)
                        .header("X-Authenticated-User-Id", userId))
                .andExpect(status().isNoContent());

        verify(dsaProblemService).archiveProblem(userId, problemId);
    }

    @Test
    void incrementAttempt_returns200OK() throws Exception {
        Long userId = 100L;
        Long problemId = 1L;

        DsaProblem problem = new DsaProblem(userId, "Merge K Lists", DsaPlatform.LEETCODE, DsaDifficulty.HARD, DsaTopic.HEAP);
        problem.setId(problemId);
        problem.setAttemptCount(1);

        when(dsaProblemService.incrementAttempt(userId, problemId)).thenReturn(new DsaProblemResponse(problem));

        mockMvc.perform(post("/api/v1/dsa/problems/{id}/attempt", problemId)
                        .header("X-Authenticated-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attemptCount").value(1));
    }

    @Test
    void startProblem_returns200OK() throws Exception {
        Long userId = 100L;
        Long problemId = 1L;

        DsaProblem problem = new DsaProblem(userId, "Start Problem", DsaPlatform.LEETCODE, DsaDifficulty.EASY, DsaTopic.STRING);
        problem.setId(problemId);
        problem.setStatus(DsaProblemStatus.IN_PROGRESS);

        when(dsaProblemService.startProblem(userId, problemId)).thenReturn(new DsaProblemResponse(problem));

        mockMvc.perform(patch("/api/v1/dsa/problems/{id}/start", problemId)
                        .header("X-Authenticated-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void solveProblem_returns200OK() throws Exception {
        Long userId = 100L;
        Long problemId = 1L;

        DsaProblem problem = new DsaProblem(userId, "Solve Problem", DsaPlatform.LEETCODE, DsaDifficulty.EASY, DsaTopic.STRING);
        problem.setId(problemId);
        problem.setStatus(DsaProblemStatus.SOLVED);
        problem.setSolvedAt(Instant.now());

        when(dsaProblemService.solveProblem(userId, problemId)).thenReturn(new DsaProblemResponse(problem));

        mockMvc.perform(patch("/api/v1/dsa/problems/{id}/solve", problemId)
                        .header("X-Authenticated-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SOLVED"));
    }

    @Test
    void revisitProblem_returns200OK() throws Exception {
        Long userId = 100L;
        Long problemId = 1L;

        DsaProblem problem = new DsaProblem(userId, "Revisit Problem", DsaPlatform.LEETCODE, DsaDifficulty.MEDIUM, DsaTopic.GRAPH);
        problem.setId(problemId);
        problem.setStatus(DsaProblemStatus.REVISIT);

        when(dsaProblemService.revisitProblem(userId, problemId)).thenReturn(new DsaProblemResponse(problem));

        mockMvc.perform(patch("/api/v1/dsa/problems/{id}/revisit", problemId)
                        .header("X-Authenticated-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVISIT"));
    }

    @Test
    void getDailyProgress_returns200OK() throws Exception {
        Long userId = 100L;
        LocalDate date = LocalDate.of(2026, 8, 26);

        DailyDsaProgressResponse response = new DailyDsaProgressResponse(date, 3, 5, 90);

        when(dsaProblemService.getDailyProgress(userId, date)).thenReturn(response);

        mockMvc.perform(get("/api/v1/dsa/progress/daily")
                        .header("X-Authenticated-User-Id", userId)
                        .param("date", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-08-26"))
                .andExpect(jsonPath("$.problemsSolved").value(3))
                .andExpect(jsonPath("$.totalAttempts").value(5))
                .andExpect(jsonPath("$.timeSpentMinutes").value(90));
    }

    @Test
    void getStatistics_returns200OK() throws Exception {
        Long userId = 100L;
        DsaStatisticsResponse response = new DsaStatisticsResponse(10, 6, 2, 2, 3, 2, 1, 300, 15);

        when(dsaProblemService.getStatistics(userId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/dsa/statistics")
                        .header("X-Authenticated-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProblems").value(10))
                .andExpect(jsonPath("$.solvedProblems").value(6))
                .andExpect(jsonPath("$.easySolved").value(3))
                .andExpect(jsonPath("$.mediumSolved").value(2))
                .andExpect(jsonPath("$.hardSolved").value(1));
    }
}
