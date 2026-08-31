package com.devsphere.user.controller;

import com.devsphere.user.dto.CreateProjectRequest;
import com.devsphere.user.dto.PageResponse;
import com.devsphere.user.dto.ProjectResponse;
import com.devsphere.user.dto.UpdateProjectRequest;
import com.devsphere.user.entity.DeveloperProject;
import com.devsphere.user.entity.ProjectStatus;
import com.devsphere.user.entity.ProjectType;
import com.devsphere.user.exception.GlobalExceptionHandler;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.service.ProjectService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = ProjectController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@Import(GlobalExceptionHandler.class)
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProjectService projectService;

    @MockBean(name = "userSecurity")
    private com.devsphere.user.security.UserSecurity userSecurity;

    @MockBean
    private com.devsphere.user.security.JwtValidator jwtValidator;

    @Test
    void createProject_withValidRequest_returns201Created() throws Exception {
        Long userId = 100L;
        CreateProjectRequest request = new CreateProjectRequest(
                "DevSphere Platform", "Developer life management platform", ProjectType.PERSONAL,
                "https://github.com/user/devsphere", "https://devsphere.io", "https://docs.devsphere.io",
                List.of("Java", "Spring Boot"), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)
        );

        DeveloperProject project = new DeveloperProject(userId, "DevSphere Platform", ProjectType.PERSONAL);
        project.setId(1L);
        project.setStatus(ProjectStatus.PLANNED);
        project.setRepositoryUrl("https://github.com/user/devsphere");
        project.setTechStack(List.of("Java", "Spring Boot"));

        when(projectService.createProject(eq(userId), any(CreateProjectRequest.class)))
                .thenReturn(new ProjectResponse(project));

        mockMvc.perform(post("/api/v1/projects")
                        .header("X-Authenticated-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/projects/1"))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("DevSphere Platform"))
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andExpect(jsonPath("$.projectType").value("PERSONAL"))
                .andExpect(jsonPath("$.techStack[0]").value("Java"));
    }

    @Test
    void createProject_withBlankName_returns400BadRequest() throws Exception {
        Long userId = 100L;
        CreateProjectRequest request = new CreateProjectRequest(
                "", "Description", ProjectType.PERSONAL,
                null, null, null, null, null, null
        );

        mockMvc.perform(post("/api/v1/projects")
                        .header("X-Authenticated-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void createProject_withInvalidUrl_returns400BadRequest() throws Exception {
        Long userId = 100L;
        CreateProjectRequest request = new CreateProjectRequest(
                "Invalid URL Project", "Description", ProjectType.PERSONAL,
                "not-a-valid-url", null, null, null, null, null
        );

        mockMvc.perform(post("/api/v1/projects")
                        .header("X-Authenticated-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void getProjectById_whenOwned_returns200OK() throws Exception {
        Long userId = 100L;
        Long projectId = 1L;
        DeveloperProject project = new DeveloperProject(userId, "DevSphere Platform", ProjectType.PERSONAL);
        project.setId(projectId);

        when(projectService.getProject(userId, projectId)).thenReturn(new ProjectResponse(project));

        mockMvc.perform(get("/api/v1/projects/{id}", projectId)
                        .header("X-Authenticated-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(projectId))
                .andExpect(jsonPath("$.name").value("DevSphere Platform"));
    }

    @Test
    void getProjectById_whenNotOwned_returns404NotFound() throws Exception {
        Long userId = 100L;
        Long projectId = 999L;

        when(projectService.getProject(userId, projectId))
                .thenThrow(new ResourceNotFoundException("PROJECT_NOT_FOUND", "Developer project not found with id: 999"));

        mockMvc.perform(get("/api/v1/projects/{id}", projectId)
                        .header("X-Authenticated-User-Id", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
    }

    @Test
    void listProjects_returns200OKWithPage() throws Exception {
        Long userId = 100L;
        DeveloperProject p1 = new DeveloperProject(userId, "Project 1", ProjectType.PERSONAL);
        p1.setId(1L);
        DeveloperProject p2 = new DeveloperProject(userId, "Project 2", ProjectType.COLLEGE);
        p2.setId(2L);

        PageResponse<ProjectResponse> pageResponse = PageResponse.fromPage(
                new PageImpl<>(List.of(new ProjectResponse(p1), new ProjectResponse(p2)), PageRequest.of(0, 20), 2)
        );

        when(projectService.listProjects(eq(userId), any(), any(), anyInt(), anyInt(), anyString()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/projects")
                        .header("X-Authenticated-User-Id", userId)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void updateProject_returns200OK() throws Exception {
        Long userId = 100L;
        Long projectId = 1L;
        UpdateProjectRequest request = new UpdateProjectRequest(
                "Updated Name", "Updated Desc", ProjectType.OPEN_SOURCE,
                "https://github.com/user/devsphere", null, null, List.of("Java"), null, null
        );

        DeveloperProject project = new DeveloperProject(userId, "Updated Name", ProjectType.OPEN_SOURCE);
        project.setId(projectId);

        when(projectService.updateProject(eq(userId), eq(projectId), any(UpdateProjectRequest.class)))
                .thenReturn(new ProjectResponse(project));

        mockMvc.perform(put("/api/v1/projects/{id}", projectId)
                        .header("X-Authenticated-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.projectType").value("OPEN_SOURCE"));
    }

    @Test
    void startProject_returns200OK() throws Exception {
        Long userId = 100L;
        Long projectId = 1L;
        DeveloperProject project = new DeveloperProject(userId, "Project 1", ProjectType.PERSONAL);
        project.setId(projectId);
        project.setStatus(ProjectStatus.IN_PROGRESS);

        when(projectService.startProject(userId, projectId)).thenReturn(new ProjectResponse(project));

        mockMvc.perform(patch("/api/v1/projects/{id}/start", projectId)
                        .header("X-Authenticated-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void completeProject_returns200OK() throws Exception {
        Long userId = 100L;
        Long projectId = 1L;
        DeveloperProject project = new DeveloperProject(userId, "Project 1", ProjectType.PERSONAL);
        project.setId(projectId);
        project.setStatus(ProjectStatus.COMPLETED);

        when(projectService.completeProject(userId, projectId)).thenReturn(new ProjectResponse(project));

        mockMvc.perform(patch("/api/v1/projects/{id}/complete", projectId)
                        .header("X-Authenticated-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void archiveProject_returns204NoContent() throws Exception {
        Long userId = 100L;
        Long projectId = 1L;

        doNothing().when(projectService).archiveProject(userId, projectId);

        mockMvc.perform(delete("/api/v1/projects/{id}", projectId)
                        .header("X-Authenticated-User-Id", userId))
                .andExpect(status().isNoContent());

        verify(projectService).archiveProject(userId, projectId);
    }
}
