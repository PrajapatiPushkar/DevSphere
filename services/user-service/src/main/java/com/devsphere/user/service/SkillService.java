package com.devsphere.user.service;

import com.devsphere.user.dto.SkillRequest;
import com.devsphere.user.dto.SkillResponse;
import com.devsphere.user.entity.Skill;
import com.devsphere.user.exception.DuplicateSkillException;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.repository.SkillRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SkillService {

    private static final Logger log = LoggerFactory.getLogger(SkillService.class);

    private final SkillRepository skillRepository;
    private final MeterRegistry meterRegistry;

    public SkillService(SkillRepository skillRepository) {
        this(skillRepository, new SimpleMeterRegistry());
    }

    @Autowired
    public SkillService(SkillRepository skillRepository, MeterRegistry meterRegistry) {
        this.skillRepository = skillRepository;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public SkillResponse createSkill(Long userId, SkillRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }

        String trimmedName = request.getName().trim();
        if (skillRepository.existsByUserIdAndNameIgnoreCase(userId, trimmedName)) {
            throw new DuplicateSkillException("DUPLICATE_SKILL", "A skill with name '" + trimmedName + "' already exists for this user");
        }

        validateYearsOfExperience(request.getYearsOfExperience());

        Skill skill = new Skill(
                userId,
                trimmedName,
                request.getCategory(),
                request.getProficiency()
        );
        skill.setYearsOfExperience(request.getYearsOfExperience());
        skill.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);

        Skill saved = skillRepository.save(skill);
        meterRegistry.counter("devsphere_skill_created_total").increment();
        log.info("Created skill ID: {} ('{}') for userId: {}", saved.getId(), saved.getName(), userId);

        return new SkillResponse(saved);
    }

    @Transactional(readOnly = true)
    public SkillResponse getSkill(Long id, Long userId) {
        Skill skill = skillRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("SKILL_NOT_FOUND", "Skill record not found"));
        return new SkillResponse(skill);
    }

    @Transactional(readOnly = true)
    public List<SkillResponse> listSkills(Long userId) {
        return skillRepository.findAllByUserIdOrderByDisplayOrderAscNameAsc(userId)
                .stream()
                .map(SkillResponse::new)
                .toList();
    }

    @Transactional
    public SkillResponse updateSkill(Long id, Long userId, SkillRequest request) {
        Skill skill = skillRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("SKILL_NOT_FOUND", "Skill record not found"));

        String trimmedName = request.getName().trim();
        if (skillRepository.existsByUserIdAndNameIgnoreCaseAndIdNot(userId, trimmedName, id)) {
            throw new DuplicateSkillException("DUPLICATE_SKILL", "A skill with name '" + trimmedName + "' already exists for this user");
        }

        validateYearsOfExperience(request.getYearsOfExperience());

        skill.setName(trimmedName);
        skill.setCategory(request.getCategory());
        skill.setProficiency(request.getProficiency());
        skill.setYearsOfExperience(request.getYearsOfExperience());
        if (request.getDisplayOrder() != null) {
            skill.setDisplayOrder(request.getDisplayOrder());
        }

        Skill updated = skillRepository.save(skill);
        meterRegistry.counter("devsphere_skill_updated_total").increment();
        log.info("Updated skill ID: {} ('{}') for userId: {}", updated.getId(), updated.getName(), userId);

        return new SkillResponse(updated);
    }

    @Transactional
    public void deleteSkill(Long id, Long userId) {
        Skill skill = skillRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("SKILL_NOT_FOUND", "Skill record not found"));

        skillRepository.delete(skill);
        meterRegistry.counter("devsphere_skill_deleted_total").increment();
        log.info("Deleted skill ID: {} for userId: {}", id, userId);
    }

    private void validateYearsOfExperience(Integer yearsOfExperience) {
        if (yearsOfExperience != null) {
            if (yearsOfExperience < 0) {
                throw new IllegalArgumentException("yearsOfExperience must be zero or positive");
            }
            if (yearsOfExperience > 70) {
                throw new IllegalArgumentException("yearsOfExperience cannot exceed 70 years");
            }
        }
    }
}
