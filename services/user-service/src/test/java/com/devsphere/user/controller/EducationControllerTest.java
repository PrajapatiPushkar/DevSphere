package com.devsphere.user.controller;

import com.devsphere.user.dto.EducationRequest;
import com.devsphere.user.dto.EducationResponse;
import com.devsphere.user.entity.Education;
import com.devsphere.user.exception.GlobalExceptionHandler;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.service.EducationService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = EducationController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@Import(GlobalExceptionHandler.class)
class EducationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EducationService educationService;

    @MockBean(name = "userSecurity")
    private com.devsphere.user.security.UserSecurity userSecurity;

    @MockBean
    private com.devsphere.user.security.JwtValidator jwtValidator;

    @Test
    void createEducation_returns201Created() throws Exception {
        Long userId = 100L;
        EducationRequest request = new EducationRequest(
                "Harvard", "Master of Science", "Computer Science", "Cambridge",
                LocalDate.of(2022, 9, 1), LocalDate.of(2024, 5, 1), false, "MS CS", 1
        );

        Education edu = new Education(userId, "Harvard", "Master of Science", LocalDate.of(2022, 9, 1));
        edu.setId(20L);

        when(educationService.createEducation(eq(userId), any(EducationRequest.class)))
                .thenReturn(new EducationResponse(edu));

        mockMvc.perform(post("/api/v1/education")
                        .header("X-Authenticated-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(20L))
                .andExpect(jsonPath("$.institutionName").value("Harvard"));
    }

    @Test
    void getEducation_notFound_returns404() throws Exception {
        Long userId = 100L;
        Long id = 99L;

        when(educationService.getEducation(id, userId))
                .thenThrow(new ResourceNotFoundException("EDUCATION_NOT_FOUND", "Education record not found"));

        mockMvc.perform(get("/api/v1/education/{id}", id)
                        .header("X-Authenticated-User-Id", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EDUCATION_NOT_FOUND"));
    }

    @Test
    void deleteEducation_returns204NoContent() throws Exception {
        Long userId = 100L;
        Long id = 20L;

        doNothing().when(educationService).deleteEducation(id, userId);

        mockMvc.perform(delete("/api/v1/education/{id}", id)
                        .header("X-Authenticated-User-Id", userId))
                .andExpect(status().isNoContent());

        verify(educationService).deleteEducation(id, userId);
    }
}
