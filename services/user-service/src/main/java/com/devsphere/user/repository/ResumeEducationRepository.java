package com.devsphere.user.repository;

import com.devsphere.user.entity.ResumeEducation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResumeEducationRepository extends JpaRepository<ResumeEducation, Long> {

    List<ResumeEducation> findAllByResumeProfileIdOrderByDisplayOrderAsc(Long resumeProfileId);

    Optional<ResumeEducation> findByResumeProfileIdAndEducationId(Long resumeProfileId, Long educationId);

    boolean existsByResumeProfileIdAndEducationId(Long resumeProfileId, Long educationId);

    void deleteByResumeProfileIdAndEducationId(Long resumeProfileId, Long educationId);
}
