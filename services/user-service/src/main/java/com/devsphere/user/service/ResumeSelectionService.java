package com.devsphere.user.service;

import com.devsphere.user.dto.ResumeCertificationRequest;
import com.devsphere.user.dto.ResumeCertificationResponse;
import com.devsphere.user.dto.ResumeEducationRequest;
import com.devsphere.user.dto.ResumeEducationResponse;
import com.devsphere.user.dto.ResumeExperienceRequest;
import com.devsphere.user.dto.ResumeExperienceResponse;
import com.devsphere.user.dto.ResumeProjectRequest;
import com.devsphere.user.dto.ResumeProjectResponse;
import com.devsphere.user.dto.ResumeSkillRequest;
import com.devsphere.user.dto.ResumeSkillResponse;
import com.devsphere.user.entity.ResumeCertification;
import com.devsphere.user.entity.ResumeEducation;
import com.devsphere.user.entity.ResumeExperience;
import com.devsphere.user.entity.ResumeProject;
import com.devsphere.user.entity.ResumeSkill;
import com.devsphere.user.exception.DuplicateResumeSelectionException;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.repository.CertificationRepository;
import com.devsphere.user.repository.DeveloperProjectRepository;
import com.devsphere.user.repository.EducationRepository;
import com.devsphere.user.repository.ExperienceRepository;
import com.devsphere.user.repository.ResumeCertificationRepository;
import com.devsphere.user.repository.ResumeEducationRepository;
import com.devsphere.user.repository.ResumeExperienceRepository;
import com.devsphere.user.repository.ResumeProfileRepository;
import com.devsphere.user.repository.ResumeProjectRepository;
import com.devsphere.user.repository.ResumeSkillRepository;
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
public class ResumeSelectionService {

    private static final Logger log = LoggerFactory.getLogger(ResumeSelectionService.class);

    private final ResumeProfileRepository resumeProfileRepository;
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

    @Autowired
    public ResumeSelectionService(ResumeProfileRepository resumeProfileRepository,
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

    public ResumeSelectionService(ResumeProfileRepository resumeProfileRepository,
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
        this(resumeProfileRepository, experienceRepository, educationRepository, skillRepository,
                certificationRepository, projectRepository, resumeExperienceRepository, resumeEducationRepository,
                resumeSkillRepository, resumeCertificationRepository, resumeProjectRepository, new SimpleMeterRegistry());
    }

    // --- EXPERIENCE SELECTIONS ---
    @Transactional
    public ResumeExperienceResponse addExperience(Long resumeId, Long userId, ResumeExperienceRequest request) {
        verifyResumeOwnership(resumeId, userId);
        verifyExperienceOwnership(request.getExperienceId(), userId);

        if (resumeExperienceRepository.existsByResumeProfileIdAndExperienceId(resumeId, request.getExperienceId())) {
            throw new DuplicateResumeSelectionException("DUPLICATE_SELECTION", "Experience is already selected in this resume");
        }

        ResumeExperience re = new ResumeExperience(resumeId, request.getExperienceId(), request.getDisplayOrder());
        ResumeExperience saved = resumeExperienceRepository.save(re);
        meterRegistry.counter("devsphere_resume_experience_selected_total").increment();
        log.info("Selected experienceId: {} for resumeId: {}", request.getExperienceId(), resumeId);

        return new ResumeExperienceResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ResumeExperienceResponse> listExperiences(Long resumeId, Long userId) {
        verifyResumeOwnership(resumeId, userId);
        return resumeExperienceRepository.findAllByResumeProfileIdOrderByDisplayOrderAsc(resumeId)
                .stream()
                .map(ResumeExperienceResponse::new)
                .toList();
    }

    @Transactional
    public void removeExperience(Long resumeId, Long experienceId, Long userId) {
        verifyResumeOwnership(resumeId, userId);
        ResumeExperience re = resumeExperienceRepository.findByResumeProfileIdAndExperienceId(resumeId, experienceId)
                .orElseThrow(() -> new ResourceNotFoundException("SELECTION_NOT_FOUND", "Experience selection not found in resume"));
        resumeExperienceRepository.delete(re);
        log.info("Removed experienceId: {} from resumeId: {}", experienceId, resumeId);
    }

    // --- EDUCATION SELECTIONS ---
    @Transactional
    public ResumeEducationResponse addEducation(Long resumeId, Long userId, ResumeEducationRequest request) {
        verifyResumeOwnership(resumeId, userId);
        verifyEducationOwnership(request.getEducationId(), userId);

        if (resumeEducationRepository.existsByResumeProfileIdAndEducationId(resumeId, request.getEducationId())) {
            throw new DuplicateResumeSelectionException("DUPLICATE_SELECTION", "Education is already selected in this resume");
        }

        ResumeEducation re = new ResumeEducation(resumeId, request.getEducationId(), request.getDisplayOrder());
        ResumeEducation saved = resumeEducationRepository.save(re);
        meterRegistry.counter("devsphere_resume_education_selected_total").increment();
        log.info("Selected educationId: {} for resumeId: {}", request.getEducationId(), resumeId);

        return new ResumeEducationResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ResumeEducationResponse> listEducations(Long resumeId, Long userId) {
        verifyResumeOwnership(resumeId, userId);
        return resumeEducationRepository.findAllByResumeProfileIdOrderByDisplayOrderAsc(resumeId)
                .stream()
                .map(ResumeEducationResponse::new)
                .toList();
    }

    @Transactional
    public void removeEducation(Long resumeId, Long educationId, Long userId) {
        verifyResumeOwnership(resumeId, userId);
        ResumeEducation re = resumeEducationRepository.findByResumeProfileIdAndEducationId(resumeId, educationId)
                .orElseThrow(() -> new ResourceNotFoundException("SELECTION_NOT_FOUND", "Education selection not found in resume"));
        resumeEducationRepository.delete(re);
        log.info("Removed educationId: {} from resumeId: {}", educationId, resumeId);
    }

    // --- SKILL SELECTIONS ---
    @Transactional
    public ResumeSkillResponse addSkill(Long resumeId, Long userId, ResumeSkillRequest request) {
        verifyResumeOwnership(resumeId, userId);
        verifySkillOwnership(request.getSkillId(), userId);

        if (resumeSkillRepository.existsByResumeProfileIdAndSkillId(resumeId, request.getSkillId())) {
            throw new DuplicateResumeSelectionException("DUPLICATE_SELECTION", "Skill is already selected in this resume");
        }

        ResumeSkill rs = new ResumeSkill(resumeId, request.getSkillId(), request.getDisplayOrder());
        ResumeSkill saved = resumeSkillRepository.save(rs);
        meterRegistry.counter("devsphere_resume_skill_selected_total").increment();
        log.info("Selected skillId: {} for resumeId: {}", request.getSkillId(), resumeId);

        return new ResumeSkillResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ResumeSkillResponse> listSkills(Long resumeId, Long userId) {
        verifyResumeOwnership(resumeId, userId);
        return resumeSkillRepository.findAllByResumeProfileIdOrderByDisplayOrderAsc(resumeId)
                .stream()
                .map(ResumeSkillResponse::new)
                .toList();
    }

    @Transactional
    public void removeSkill(Long resumeId, Long skillId, Long userId) {
        verifyResumeOwnership(resumeId, userId);
        ResumeSkill rs = resumeSkillRepository.findByResumeProfileIdAndSkillId(resumeId, skillId)
                .orElseThrow(() -> new ResourceNotFoundException("SELECTION_NOT_FOUND", "Skill selection not found in resume"));
        resumeSkillRepository.delete(rs);
        log.info("Removed skillId: {} from resumeId: {}", skillId, resumeId);
    }

    // --- CERTIFICATION SELECTIONS ---
    @Transactional
    public ResumeCertificationResponse addCertification(Long resumeId, Long userId, ResumeCertificationRequest request) {
        verifyResumeOwnership(resumeId, userId);
        verifyCertificationOwnership(request.getCertificationId(), userId);

        if (resumeCertificationRepository.existsByResumeProfileIdAndCertificationId(resumeId, request.getCertificationId())) {
            throw new DuplicateResumeSelectionException("DUPLICATE_SELECTION", "Certification is already selected in this resume");
        }

        ResumeCertification rc = new ResumeCertification(resumeId, request.getCertificationId(), request.getDisplayOrder());
        ResumeCertification saved = resumeCertificationRepository.save(rc);
        meterRegistry.counter("devsphere_resume_certification_selected_total").increment();
        log.info("Selected certificationId: {} for resumeId: {}", request.getCertificationId(), resumeId);

        return new ResumeCertificationResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ResumeCertificationResponse> listCertifications(Long resumeId, Long userId) {
        verifyResumeOwnership(resumeId, userId);
        return resumeCertificationRepository.findAllByResumeProfileIdOrderByDisplayOrderAsc(resumeId)
                .stream()
                .map(ResumeCertificationResponse::new)
                .toList();
    }

    @Transactional
    public void removeCertification(Long resumeId, Long certificationId, Long userId) {
        verifyResumeOwnership(resumeId, userId);
        ResumeCertification rc = resumeCertificationRepository.findByResumeProfileIdAndCertificationId(resumeId, certificationId)
                .orElseThrow(() -> new ResourceNotFoundException("SELECTION_NOT_FOUND", "Certification selection not found in resume"));
        resumeCertificationRepository.delete(rc);
        log.info("Removed certificationId: {} from resumeId: {}", certificationId, resumeId);
    }

    // --- PROJECT SELECTIONS ---
    @Transactional
    public ResumeProjectResponse addProject(Long resumeId, Long userId, ResumeProjectRequest request) {
        verifyResumeOwnership(resumeId, userId);
        verifyProjectOwnership(request.getProjectId(), userId);

        if (resumeProjectRepository.existsByResumeProfileIdAndProjectId(resumeId, request.getProjectId())) {
            throw new DuplicateResumeSelectionException("DUPLICATE_SELECTION", "Project is already selected in this resume");
        }

        ResumeProject rp = new ResumeProject(resumeId, request.getProjectId(), request.getDisplayOrder());
        ResumeProject saved = resumeProjectRepository.save(rp);
        meterRegistry.counter("devsphere_resume_project_selected_total").increment();
        log.info("Selected projectId: {} for resumeId: {}", request.getProjectId(), resumeId);

        return new ResumeProjectResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ResumeProjectResponse> listProjects(Long resumeId, Long userId) {
        verifyResumeOwnership(resumeId, userId);
        return resumeProjectRepository.findAllByResumeProfileIdOrderByDisplayOrderAsc(resumeId)
                .stream()
                .map(ResumeProjectResponse::new)
                .toList();
    }

    @Transactional
    public void removeProject(Long resumeId, Long projectId, Long userId) {
        verifyResumeOwnership(resumeId, userId);
        ResumeProject rp = resumeProjectRepository.findByResumeProfileIdAndProjectId(resumeId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("SELECTION_NOT_FOUND", "Project selection not found in resume"));
        resumeProjectRepository.delete(rp);
        log.info("Removed projectId: {} from resumeId: {}", projectId, resumeId);
    }

    // --- OWNERSHIP HELPER METHODS ---
    private void verifyResumeOwnership(Long resumeId, Long userId) {
        if (!resumeProfileRepository.findByIdAndUserId(resumeId, userId).isPresent()) {
            throw new ResourceNotFoundException("RESUME_NOT_FOUND", "Resume profile not found");
        }
    }

    private void verifyExperienceOwnership(Long experienceId, Long userId) {
        if (!experienceRepository.findByIdAndUserId(experienceId, userId).isPresent()) {
            throw new ResourceNotFoundException("EXPERIENCE_NOT_FOUND", "Experience record not found");
        }
    }

    private void verifyEducationOwnership(Long educationId, Long userId) {
        if (!educationRepository.findByIdAndUserId(educationId, userId).isPresent()) {
            throw new ResourceNotFoundException("EDUCATION_NOT_FOUND", "Education record not found");
        }
    }

    private void verifySkillOwnership(Long skillId, Long userId) {
        if (!skillRepository.findByIdAndUserId(skillId, userId).isPresent()) {
            throw new ResourceNotFoundException("SKILL_NOT_FOUND", "Skill record not found");
        }
    }

    private void verifyCertificationOwnership(Long certificationId, Long userId) {
        if (!certificationRepository.findByIdAndUserId(certificationId, userId).isPresent()) {
            throw new ResourceNotFoundException("CERTIFICATION_NOT_FOUND", "Certification record not found");
        }
    }

    private void verifyProjectOwnership(Long projectId, Long userId) {
        if (!projectRepository.findByIdAndUserId(projectId, userId).isPresent()) {
            throw new ResourceNotFoundException("PROJECT_NOT_FOUND", "Developer project record not found");
        }
    }
}
