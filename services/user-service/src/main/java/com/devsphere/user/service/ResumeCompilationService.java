package com.devsphere.user.service;

import com.devsphere.user.dto.compilation.CompiledCertificationResponse;
import com.devsphere.user.dto.compilation.CompiledEducationResponse;
import com.devsphere.user.dto.compilation.CompiledExperienceResponse;
import com.devsphere.user.dto.compilation.CompiledProjectResponse;
import com.devsphere.user.dto.compilation.CompiledResumeResponse;
import com.devsphere.user.dto.compilation.CompiledResumeSectionResponse;
import com.devsphere.user.dto.compilation.CompiledSkillItemResponse;
import com.devsphere.user.dto.compilation.CompiledSkillsResponse;
import com.devsphere.user.dto.compilation.CompiledSummaryResponse;
import com.devsphere.user.entity.Certification;
import com.devsphere.user.entity.DeveloperProject;
import com.devsphere.user.entity.Education;
import com.devsphere.user.entity.Experience;
import com.devsphere.user.entity.ResumeCertification;
import com.devsphere.user.entity.ResumeEducation;
import com.devsphere.user.entity.ResumeExperience;
import com.devsphere.user.entity.ResumeProfile;
import com.devsphere.user.entity.ResumeProject;
import com.devsphere.user.entity.ResumeSection;
import com.devsphere.user.entity.ResumeSkill;
import com.devsphere.user.entity.Skill;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.repository.CareerProfileRepository;
import com.devsphere.user.repository.CertificationRepository;
import com.devsphere.user.repository.DeveloperProjectRepository;
import com.devsphere.user.repository.EducationRepository;
import com.devsphere.user.repository.ExperienceRepository;
import com.devsphere.user.repository.ResumeCertificationRepository;
import com.devsphere.user.repository.ResumeEducationRepository;
import com.devsphere.user.repository.ResumeExperienceRepository;
import com.devsphere.user.repository.ResumeProfileRepository;
import com.devsphere.user.repository.ResumeProjectRepository;
import com.devsphere.user.repository.ResumeSectionRepository;
import com.devsphere.user.repository.ResumeSkillRepository;
import com.devsphere.user.repository.SkillRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResumeCompilationService {

    private static final Logger log = LoggerFactory.getLogger(ResumeCompilationService.class);

    private final ResumeProfileRepository resumeProfileRepository;
    private final ResumeSectionRepository resumeSectionRepository;
    private final CareerProfileRepository careerProfileRepository;

    private final ExperienceRepository experienceRepository;
    private final EducationRepository educationRepository;
    private final SkillRepository skillRepository;
    private final CertificationRepository certificationRepository;
    private final DeveloperProjectRepository projectRepository;

    private final ResumeExperienceRepository resumeExperienceRepository;
    private final ResumeEducationRepository resumeEducationRepository;
    private final ResumeSkillRepository resumeSkillRepository;
    private final ResumeCertificationRepository resumeCertificationRepository;
    private final ResumeProjectRepository resumeProjectRepository;

    private final MeterRegistry meterRegistry;

    public ResumeCompilationService(
            ResumeProfileRepository resumeProfileRepository,
            ResumeSectionRepository resumeSectionRepository,
            CareerProfileRepository careerProfileRepository,
            ExperienceRepository experienceRepository,
            EducationRepository educationRepository,
            SkillRepository skillRepository,
            CertificationRepository certificationRepository,
            DeveloperProjectRepository projectRepository,
            ResumeExperienceRepository resumeExperienceRepository,
            ResumeEducationRepository resumeEducationRepository,
            ResumeSkillRepository resumeSkillRepository,
            ResumeCertificationRepository resumeCertificationRepository,
            ResumeProjectRepository resumeProjectRepository) {
        this(resumeProfileRepository, resumeSectionRepository, careerProfileRepository,
                experienceRepository, educationRepository, skillRepository, certificationRepository, projectRepository,
                resumeExperienceRepository, resumeEducationRepository, resumeSkillRepository,
                resumeCertificationRepository, resumeProjectRepository, new SimpleMeterRegistry());
    }

    @Autowired
    public ResumeCompilationService(
            ResumeProfileRepository resumeProfileRepository,
            ResumeSectionRepository resumeSectionRepository,
            CareerProfileRepository careerProfileRepository,
            ExperienceRepository experienceRepository,
            EducationRepository educationRepository,
            SkillRepository skillRepository,
            CertificationRepository certificationRepository,
            DeveloperProjectRepository projectRepository,
            ResumeExperienceRepository resumeExperienceRepository,
            ResumeEducationRepository resumeEducationRepository,
            ResumeSkillRepository resumeSkillRepository,
            ResumeCertificationRepository resumeCertificationRepository,
            ResumeProjectRepository resumeProjectRepository,
            MeterRegistry meterRegistry) {
        this.resumeProfileRepository = resumeProfileRepository;
        this.resumeSectionRepository = resumeSectionRepository;
        this.careerProfileRepository = careerProfileRepository;
        this.experienceRepository = experienceRepository;
        this.educationRepository = educationRepository;
        this.skillRepository = skillRepository;
        this.certificationRepository = certificationRepository;
        this.projectRepository = projectRepository;
        this.resumeExperienceRepository = resumeExperienceRepository;
        this.resumeEducationRepository = resumeEducationRepository;
        this.resumeSkillRepository = resumeSkillRepository;
        this.resumeCertificationRepository = resumeCertificationRepository;
        this.resumeProjectRepository = resumeProjectRepository;
        this.meterRegistry = meterRegistry;
    }

    @Transactional(readOnly = true)
    public CompiledResumeResponse compileResume(Long resumeId, Long userId) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            ResumeProfile profile = resumeProfileRepository.findByIdAndUserId(resumeId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("RESUME_NOT_FOUND", "Resume profile not found"));

            List<ResumeSection> sections = resumeSectionRepository.findAllByResumeProfileIdOrderByDisplayOrderAscIdAsc(resumeId);

            List<CompiledResumeSectionResponse> compiledSections = new ArrayList<>();

            for (ResumeSection section : sections) {
                if (Boolean.TRUE.equals(section.getVisible())) {
                    Object content = compileSectionContent(section, profile, userId);
                    compiledSections.add(new CompiledResumeSectionResponse(
                            section.getSectionType(),
                            section.getDisplayOrder(),
                            section.getVisible(),
                            content
                    ));
                }
            }

            meterRegistry.counter("devsphere_resume_compilation_total",
                    "status", "success",
                    "template", profile.getTemplate().name().toLowerCase()
            ).increment();

            log.info("Successfully compiled resume ID: {} for userId: {}", resumeId, userId);

            return new CompiledResumeResponse(
                    profile.getId(),
                    profile.getId(),
                    profile.getName(),
                    profile.getTargetRole(),
                    profile.getTemplate(),
                    compiledSections
            );
        } catch (Exception e) {
            meterRegistry.counter("devsphere_resume_compilation_total", "status", "failure").increment();
            throw e;
        } finally {
            sample.stop(meterRegistry.timer("devsphere_resume_compilation_duration"));
        }
    }

    private Object compileSectionContent(ResumeSection section, ResumeProfile profile, Long userId) {
        return switch (section.getSectionType()) {
            case SUMMARY -> compileSummary(profile, userId);
            case EXPERIENCE -> compileExperiences(profile.getId(), userId);
            case EDUCATION -> compileEducations(profile.getId(), userId);
            case SKILLS -> compileSkills(profile.getId(), userId);
            case CERTIFICATIONS -> compileCertifications(profile.getId(), userId);
            case PROJECTS -> compileProjects(profile.getId(), userId);
        };
    }

    private CompiledSummaryResponse compileSummary(ResumeProfile profile, Long userId) {
        if (profile.getSummaryOverride() != null && !profile.getSummaryOverride().isBlank()) {
            return new CompiledSummaryResponse(profile.getSummaryOverride().trim());
        }

        return careerProfileRepository.findByUserId(userId)
                .map(cp -> new CompiledSummaryResponse(cp.getProfessionalSummary()))
                .orElseGet(() -> new CompiledSummaryResponse(null));
    }

    private Map<String, Object> compileExperiences(Long resumeId, Long userId) {
        List<ResumeExperience> refs = resumeExperienceRepository.findAllByResumeProfileIdOrderByDisplayOrderAsc(resumeId);
        if (refs.isEmpty()) {
            return Map.of("items", List.of());
        }

        List<Long> ids = refs.stream().map(ResumeExperience::getExperienceId).toList();
        Map<Long, Experience> expMap = experienceRepository.findAllByIdInAndUserId(ids, userId)
                .stream()
                .collect(Collectors.toMap(Experience::getId, Function.identity()));

        List<CompiledExperienceResponse> items = new ArrayList<>();
        for (ResumeExperience ref : refs) {
            Experience exp = expMap.get(ref.getExperienceId());
            if (exp != null) {
                items.add(new CompiledExperienceResponse(exp, ref.getDisplayOrder()));
            }
        }
        return Map.of("items", items);
    }

    private Map<String, Object> compileEducations(Long resumeId, Long userId) {
        List<ResumeEducation> refs = resumeEducationRepository.findAllByResumeProfileIdOrderByDisplayOrderAsc(resumeId);
        if (refs.isEmpty()) {
            return Map.of("items", List.of());
        }

        List<Long> ids = refs.stream().map(ResumeEducation::getEducationId).toList();
        Map<Long, Education> eduMap = educationRepository.findAllByIdInAndUserId(ids, userId)
                .stream()
                .collect(Collectors.toMap(Education::getId, Function.identity()));

        List<CompiledEducationResponse> items = new ArrayList<>();
        for (ResumeEducation ref : refs) {
            Education edu = eduMap.get(ref.getEducationId());
            if (edu != null) {
                items.add(new CompiledEducationResponse(edu, ref.getDisplayOrder()));
            }
        }
        return Map.of("items", items);
    }

    private CompiledSkillsResponse compileSkills(Long resumeId, Long userId) {
        List<ResumeSkill> refs = resumeSkillRepository.findAllByResumeProfileIdOrderByDisplayOrderAsc(resumeId);
        if (refs.isEmpty()) {
            return new CompiledSkillsResponse(List.of());
        }

        List<Long> ids = refs.stream().map(ResumeSkill::getSkillId).toList();
        Map<Long, Skill> skillMap = skillRepository.findAllByIdInAndUserId(ids, userId)
                .stream()
                .collect(Collectors.toMap(Skill::getId, Function.identity()));

        List<CompiledSkillItemResponse> items = new ArrayList<>();
        for (ResumeSkill ref : refs) {
            Skill skill = skillMap.get(ref.getSkillId());
            if (skill != null) {
                items.add(new CompiledSkillItemResponse(skill, ref.getDisplayOrder()));
            }
        }
        return new CompiledSkillsResponse(items);
    }

    private Map<String, Object> compileCertifications(Long resumeId, Long userId) {
        List<ResumeCertification> refs = resumeCertificationRepository.findAllByResumeProfileIdOrderByDisplayOrderAsc(resumeId);
        if (refs.isEmpty()) {
            return Map.of("items", List.of());
        }

        List<Long> ids = refs.stream().map(ResumeCertification::getCertificationId).toList();
        Map<Long, Certification> certMap = certificationRepository.findAllByIdInAndUserId(ids, userId)
                .stream()
                .collect(Collectors.toMap(Certification::getId, Function.identity()));

        List<CompiledCertificationResponse> items = new ArrayList<>();
        for (ResumeCertification ref : refs) {
            Certification cert = certMap.get(ref.getCertificationId());
            if (cert != null) {
                items.add(new CompiledCertificationResponse(cert, ref.getDisplayOrder()));
            }
        }
        return Map.of("items", items);
    }

    private Map<String, Object> compileProjects(Long resumeId, Long userId) {
        List<ResumeProject> refs = resumeProjectRepository.findAllByResumeProfileIdOrderByDisplayOrderAsc(resumeId);
        if (refs.isEmpty()) {
            return Map.of("items", List.of());
        }

        List<Long> ids = refs.stream().map(ResumeProject::getProjectId).toList();
        Map<Long, DeveloperProject> projMap = projectRepository.findAllByIdInAndUserId(ids, userId)
                .stream()
                .collect(Collectors.toMap(DeveloperProject::getId, Function.identity()));

        List<CompiledProjectResponse> items = new ArrayList<>();
        for (ResumeProject ref : refs) {
            DeveloperProject proj = projMap.get(ref.getProjectId());
            if (proj != null) {
                items.add(new CompiledProjectResponse(proj, ref.getDisplayOrder()));
            }
        }
        return Map.of("items", items);
    }
}
