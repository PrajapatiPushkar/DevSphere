package com.devsphere.user.controller;

import com.devsphere.user.dto.CreateTaskRequest;
import com.devsphere.user.dto.PageResponse;
import com.devsphere.user.dto.TaskResponse;
import com.devsphere.user.dto.UpdateTaskRequest;
import com.devsphere.user.entity.Task;
import com.devsphere.user.entity.TaskPriority;
import com.devsphere.user.entity.TaskStatus;
import com.devsphere.user.exception.GlobalExceptionHandler;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
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

@WebMvcTest(value = TaskController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@Import(GlobalExceptionHandler.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskService taskService;

    @MockBean(name = "userSecurity")
    private com.devsphere.user.security.UserSecurity userSecurity;

    @MockBean
    private com.devsphere.user.security.JwtValidator jwtValidator;

    @Test
    void createTask_withValidRequest_returns201Created() throws Exception {
        Long userId = 100L;
        CreateTaskRequest request = new CreateTaskRequest("Binary Tree Problems", "Complete 5 problems", TaskPriority.HIGH, null, null);

        Task task = new Task(userId, "Binary Tree Problems", TaskPriority.HIGH);
        task.setId(1L);
        task.setStatus(TaskStatus.TODO);
        TaskResponse response = new TaskResponse(task);

        when(taskService.createTask(eq(userId), any(CreateTaskRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/tasks")
                        .header("X-Authenticated-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Binary Tree Problems"))
                .andExpect(jsonPath("$.status").value("TODO"));
    }

    @Test
    void createTask_withBlankTitle_returns400BadRequest() throws Exception {
        Long userId = 100L;
        CreateTaskRequest request = new CreateTaskRequest("", "Invalid title", TaskPriority.MEDIUM, null, null);

        mockMvc.perform(post("/api/v1/tasks")
                        .header("X-Authenticated-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void listTasks_returns200OK() throws Exception {
        Long userId = 100L;
        Task task = new Task(userId, "Study Kafka", TaskPriority.URGENT);
        task.setId(10L);

        PageResponse<TaskResponse> pageResponse = new PageResponse<>(List.of(new TaskResponse(task)), 0, 20, 1, 1, true);

        when(taskService.listTasks(eq(userId), any(), any(), any(), anyInt(), anyInt())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/tasks")
                        .header("X-Authenticated-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10L))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getTaskById_whenNotFound_returns404NotFound() throws Exception {
        Long userId = 100L;
        Long taskId = 99L;

        when(taskService.getTask(userId, taskId))
                .thenThrow(new ResourceNotFoundException("RESOURCE_NOT_FOUND", "Task not found with id: 99"));

        mockMvc.perform(get("/api/v1/tasks/{id}", taskId)
                        .header("X-Authenticated-User-Id", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void startTask_returns200OK() throws Exception {
        Long userId = 100L;
        Long taskId = 1L;
        Task task = new Task(userId, "Start engine", TaskPriority.HIGH);
        task.setId(taskId);
        task.setStatus(TaskStatus.IN_PROGRESS);

        when(taskService.startTask(userId, taskId)).thenReturn(new TaskResponse(task));

        mockMvc.perform(patch("/api/v1/tasks/{id}/start", taskId)
                        .header("X-Authenticated-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void completeTask_returns200OK() throws Exception {
        Long userId = 100L;
        Long taskId = 1L;
        Task task = new Task(userId, "Finish engine", TaskPriority.HIGH);
        task.setId(taskId);
        task.setStatus(TaskStatus.COMPLETED);

        when(taskService.completeTask(userId, taskId)).thenReturn(new TaskResponse(task));

        mockMvc.perform(patch("/api/v1/tasks/{id}/complete", taskId)
                        .header("X-Authenticated-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void deleteTask_returns204NoContent() throws Exception {
        Long userId = 100L;
        Long taskId = 1L;

        mockMvc.perform(delete("/api/v1/tasks/{id}", taskId)
                        .header("X-Authenticated-User-Id", userId))
                .andExpect(status().isNoContent());

        verify(taskService).archiveTask(userId, taskId);
    }
}
