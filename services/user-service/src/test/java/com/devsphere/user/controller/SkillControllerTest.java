package com.devsphere.user.controller;

import com.devsphere.user.dto.SkillRequest;
import com.devsphere.user.dto.SkillResponse;
import com.devsphere.user.entity.Proficiency;
import com.devsphere.user.entity.Skill;
import com.devsphere.user.entity.SkillCategory;
import com.devsphere.user.exception.DuplicateSkillException;
import com.devsphere.user.exception.GlobalExceptionHandler;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.service.SkillService;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = SkillController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@Import(GlobalExceptionHandler.class)
class SkillControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SkillService skillService;

    @MockBean(name = "userSecurity")
    private com.devsphere.user.security.UserSecurity userSecurity;

    @MockBean
    private com.devsphere.user.security.JwtValidator jwtValidator;

    @Test
    void createSkill_returns201Created() throws Exception {
        Long userId = 100L;
        SkillRequest request = new SkillRequest("Spring Boot", SkillCategory.FRAMEWORK, Proficiency.EXPERT, 4, 1);

        Skill skill = new Skill(userId, "Spring Boot", SkillCategory.FRAMEWORK, Proficiency.EXPERT);
        skill.setId(30L);

        when(skillService.createSkill(eq(userId), any(SkillRequest.class)))
                .thenReturn(new SkillResponse(skill));

        mockMvc.perform(post("/api/v1/skills")
                        .header("X-Authenticated-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(30L))
                .andExpect(jsonPath("$.name").value("Spring Boot"));
    }

    @Test
    void createSkill_duplicateSkill_returns409Conflict() throws Exception {
        Long userId = 100L;
        SkillRequest request = new SkillRequest("Java", SkillCategory.PROGRAMMING_LANGUAGE, Proficiency.EXPERT, 5, 1);

        when(skillService.createSkill(eq(userId), any(SkillRequest.class)))
                .thenThrow(new DuplicateSkillException("DUPLICATE_SKILL", "A skill with name 'Java' already exists for this user"));

        mockMvc.perform(post("/api/v1/skills")
                        .header("X-Authenticated-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_SKILL"));
    }

    @Test
    void deleteSkill_returns204NoContent() throws Exception {
        Long userId = 100L;
        Long id = 30L;

        doNothing().when(skillService).deleteSkill(id, userId);

        mockMvc.perform(delete("/api/v1/skills/{id}", id)
                        .header("X-Authenticated-User-Id", userId))
                .andExpect(status().isNoContent());

        verify(skillService).deleteSkill(id, userId);
    }
}
