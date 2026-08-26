package com.devsphere.user.service;

import com.devsphere.user.dto.SkillRequest;
import com.devsphere.user.dto.SkillResponse;
import com.devsphere.user.entity.Proficiency;
import com.devsphere.user.entity.Skill;
import com.devsphere.user.entity.SkillCategory;
import com.devsphere.user.exception.DuplicateSkillException;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.repository.SkillRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkillServiceTest {

    @Mock
    private SkillRepository skillRepository;

    private SimpleMeterRegistry meterRegistry;
    private SkillService skillService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        skillService = new SkillService(skillRepository, meterRegistry);
    }

    @Test
    void createSkill_validRequest_createsAndIncrementsCounter() {
        Long userId = 100L;
        SkillRequest request = new SkillRequest("Java", SkillCategory.PROGRAMMING_LANGUAGE, Proficiency.EXPERT, 5, 1);

        when(skillRepository.existsByUserIdAndNameIgnoreCase(userId, "Java")).thenReturn(false);
        when(skillRepository.save(any(Skill.class))).thenAnswer(inv -> {
            Skill s = inv.getArgument(0);
            s.setId(30L);
            return s;
        });

        SkillResponse response = skillService.createSkill(userId, request);

        assertThat(response.getId()).isEqualTo(30L);
        assertThat(response.getName()).isEqualTo("Java");
        assertThat(meterRegistry.find("devsphere_skill_created_total").counter().count()).isEqualTo(1.0);
    }

    @Test
    void createSkill_duplicateCaseInsensitive_throwsDuplicateSkillException() {
        Long userId = 100L;
        SkillRequest request = new SkillRequest("java", SkillCategory.PROGRAMMING_LANGUAGE, Proficiency.ADVANCED, 3, 0);

        when(skillRepository.existsByUserIdAndNameIgnoreCase(userId, "java")).thenReturn(true);

        assertThatThrownBy(() -> skillService.createSkill(userId, request))
                .isInstanceOf(DuplicateSkillException.class)
                .hasMessageContaining("A skill with name 'java' already exists");
    }

    @Test
    void createSkill_yearsOfExperienceNegative_throwsIllegalArgumentException() {
        Long userId = 100L;
        SkillRequest request = new SkillRequest("Python", SkillCategory.PROGRAMMING_LANGUAGE, Proficiency.INTERMEDIATE, -1, 0);

        when(skillRepository.existsByUserIdAndNameIgnoreCase(userId, "Python")).thenReturn(false);

        assertThatThrownBy(() -> skillService.createSkill(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("yearsOfExperience must be zero or positive");
    }

    @Test
    void deleteSkill_found_deletesRecord() {
        Long id = 30L;
        Long userId = 100L;
        Skill skill = new Skill(userId, "Java", SkillCategory.PROGRAMMING_LANGUAGE, Proficiency.EXPERT);
        skill.setId(id);

        when(skillRepository.findByIdAndUserId(id, userId)).thenReturn(Optional.of(skill));

        skillService.deleteSkill(id, userId);

        verify(skillRepository).delete(skill);
        assertThat(meterRegistry.find("devsphere_skill_deleted_total").counter().count()).isEqualTo(1.0);
    }
}
