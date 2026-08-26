package com.devsphere.user.controller;

import com.devsphere.user.dto.ResumeSectionResponse;
import com.devsphere.user.dto.UpdateResumeSectionRequest;
import com.devsphere.user.entity.ResumeSection;
import com.devsphere.user.entity.ResumeSectionType;
import com.devsphere.user.exception.GlobalExceptionHandler;
import com.devsphere.user.service.ResumeSectionService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = ResumeSectionController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@Import(GlobalExceptionHandler.class)
class ResumeSectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ResumeSectionService resumeSectionService;

    @MockBean(name = "userSecurity")
    private com.devsphere.user.security.UserSecurity userSecurity;

    @MockBean
    private com.devsphere.user.security.JwtValidator jwtValidator;

    @Test
    void listSections_returns200OK() throws Exception {
        Long userId = 100L;
        Long resumeId = 50L;

        ResumeSection s1 = new ResumeSection(resumeId, ResumeSectionType.SUMMARY, 1, true);
        s1.setId(10L);

        when(resumeSectionService.listSections(resumeId, userId)).thenReturn(List.of(new ResumeSectionResponse(s1)));

        mockMvc.perform(get("/api/v1/resumes/{resumeId}/sections", resumeId)
                        .header("X-Authenticated-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10L))
                .andExpect(jsonPath("$[0].sectionType").value("SUMMARY"));
    }

    @Test
    void updateSection_returns200OK() throws Exception {
        Long userId = 100L;
        Long resumeId = 50L;
        Long sectionId = 10L;

        UpdateResumeSectionRequest request = new UpdateResumeSectionRequest(2, false);

        ResumeSection updated = new ResumeSection(resumeId, ResumeSectionType.SUMMARY, 2, false);
        updated.setId(sectionId);

        when(resumeSectionService.updateSection(eq(resumeId), eq(sectionId), eq(userId), any(UpdateResumeSectionRequest.class)))
                .thenReturn(new ResumeSectionResponse(updated));

        mockMvc.perform(put("/api/v1/resumes/{resumeId}/sections/{sectionId}", resumeId, sectionId)
                        .header("X-Authenticated-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayOrder").value(2))
                .andExpect(jsonPath("$.visible").value(false));
    }
}
