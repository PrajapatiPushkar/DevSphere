package com.devsphere.user.controller;

import com.devsphere.user.dto.publicresume.PublicResumeResponse;
import com.devsphere.user.entity.ResumeTemplate;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.security.JwtAuthenticationFilter;
import com.devsphere.user.service.PublicResumeService;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PublicResumeController.class)
@AutoConfigureMockMvc(addFilters = false)
class PublicResumeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PublicResumeService publicResumeService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void getPublicResume_WhenFound_Returns200AndResponse() throws Exception {
        String publicId = "pub-uuid-1234";
        PublicResumeResponse response = new PublicResumeResponse("Backend Dev", "Senior Java Engineer", ResumeTemplate.PROFESSIONAL, Collections.emptyList());

        when(publicResumeService.getPublicResume(publicId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/public/resumes/{publicResumeId}", publicId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Backend Dev"))
                .andExpect(jsonPath("$.targetRole").value("Senior Java Engineer"))
                .andExpect(jsonPath("$.template").value("PROFESSIONAL"))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.resumeProfileId").doesNotExist());
    }

    @Test
    void getPublicResume_WhenNotFound_Returns404() throws Exception {
        String publicId = "non-existent";
        when(publicResumeService.getPublicResume(publicId))
                .thenThrow(new ResourceNotFoundException("PUBLIC_RESUME_NOT_FOUND", "Public resume not found"));

        mockMvc.perform(get("/api/v1/public/resumes/{publicResumeId}", publicId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PUBLIC_RESUME_NOT_FOUND"));
    }
}
