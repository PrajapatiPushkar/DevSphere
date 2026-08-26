package com.devsphere.user.controller;

import com.devsphere.user.dto.ExperienceRequest;
import com.devsphere.user.dto.ExperienceResponse;
import com.devsphere.user.entity.EmploymentType;
import com.devsphere.user.entity.Experience;
import com.devsphere.user.exception.GlobalExceptionHandler;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.service.ExperienceService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = ExperienceController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@Import(GlobalExceptionHandler.class)
class ExperienceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ExperienceService experienceService;

    @MockBean(name = "userSecurity")
    private com.devsphere.user.security.UserSecurity userSecurity;

    @MockBean
    private com.devsphere.user.security.JwtValidator jwtValidator;

    @Test
    void createExperience_returns201Created() throws Exception {
        Long userId = 100L;
        ExperienceRequest request = new ExperienceRequest(
                "Google", "Software Engineer", EmploymentType.FULL_TIME, "Mountain View",
                LocalDate.of(2021, 6, 1), null, true, "L4 Engineer", 1
        );

        Experience exp = new Experience(userId, "Google", "Software Engineer", EmploymentType.FULL_TIME, LocalDate.of(2021, 6, 1));
        exp.setId(10L);
        exp.setCurrentlyWorking(true);

        when(experienceService.createExperience(eq(userId), any(ExperienceRequest.class)))
                .thenReturn(new ExperienceResponse(exp));

        mockMvc.perform(post("/api/v1/experience")
                        .header("X-Authenticated-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.companyName").value("Google"));
    }

    @Test
    void getExperience_returns200OK() throws Exception {
        Long userId = 100L;
        Long id = 10L;

        Experience exp = new Experience(userId, "Google", "SWE", EmploymentType.FULL_TIME, LocalDate.of(2021, 6, 1));
        exp.setId(id);

        when(experienceService.getExperience(id, userId)).thenReturn(new ExperienceResponse(exp));

        mockMvc.perform(get("/api/v1/experience/{id}", id)
                        .header("X-Authenticated-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.companyName").value("Google"));
    }

    @Test
    void getExperience_notFound_returns404() throws Exception {
        Long userId = 100L;
        Long id = 99L;

        when(experienceService.getExperience(id, userId))
                .thenThrow(new ResourceNotFoundException("EXPERIENCE_NOT_FOUND", "Experience record not found"));

        mockMvc.perform(get("/api/v1/experience/{id}", id)
                        .header("X-Authenticated-User-Id", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EXPERIENCE_NOT_FOUND"));
    }

    @Test
    void deleteExperience_returns204NoContent() throws Exception {
        Long userId = 100L;
        Long id = 10L;

        doNothing().when(experienceService).deleteExperience(id, userId);

        mockMvc.perform(delete("/api/v1/experience/{id}", id)
                        .header("X-Authenticated-User-Id", userId))
                .andExpect(status().isNoContent());

        verify(experienceService).deleteExperience(id, userId);
    }
}
