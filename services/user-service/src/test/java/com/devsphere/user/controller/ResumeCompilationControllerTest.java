package com.devsphere.user.controller;

import com.devsphere.user.dto.compilation.CompiledResumeResponse;
import com.devsphere.user.dto.compilation.CompiledResumeSectionResponse;
import com.devsphere.user.dto.compilation.CompiledSummaryResponse;
import com.devsphere.user.entity.ResumeSectionType;
import com.devsphere.user.entity.ResumeTemplate;
import com.devsphere.user.exception.GlobalExceptionHandler;
import com.devsphere.user.service.ResumeCompilationService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = ResumeCompilationController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@Import(GlobalExceptionHandler.class)
class ResumeCompilationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ResumeCompilationService resumeCompilationService;

    @MockBean(name = "userSecurity")
    private com.devsphere.user.security.UserSecurity userSecurity;

    @MockBean
    private com.devsphere.user.security.JwtValidator jwtValidator;

    @Test
    void compileResume_returns200OK() throws Exception {
        Long userId = 100L;
        Long resumeId = 50L;

        CompiledResumeSectionResponse summarySection = new CompiledResumeSectionResponse(
                ResumeSectionType.SUMMARY, 1, true, new CompiledSummaryResponse("Summary text")
        );

        CompiledResumeResponse response = new CompiledResumeResponse(
                resumeId, resumeId, "Java Backend Resume", "Backend Engineer", ResumeTemplate.PROFESSIONAL, List.of(summarySection)
        );

        when(resumeCompilationService.compileResume(resumeId, userId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/resumes/{resumeId}/compile", resumeId)
                        .header("X-Authenticated-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(resumeId))
                .andExpect(jsonPath("$.name").value("Java Backend Resume"))
                .andExpect(jsonPath("$.sections[0].sectionType").value("SUMMARY"))
                .andExpect(jsonPath("$.sections[0].content.text").value("Summary text"));
    }
}
