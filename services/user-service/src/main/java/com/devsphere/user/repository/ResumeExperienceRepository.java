package com.devsphere.user.repository;

import com.devsphere.user.entity.ResumeExperience;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResumeExperienceRepository extends JpaRepository<ResumeExperience, Long> {

    List<ResumeExperience> findAllByResumeProfileIdOrderByDisplayOrderAsc(Long resumeProfileId);

    Optional<ResumeExperience> findByResumeProfileIdAndExperienceId(Long resumeProfileId, Long experienceId);

    boolean existsByResumeProfileIdAndExperienceId(Long resumeProfileId, Long experienceId);

    void deleteByResumeProfileIdAndExperienceId(Long resumeProfileId, Long experienceId);
}
