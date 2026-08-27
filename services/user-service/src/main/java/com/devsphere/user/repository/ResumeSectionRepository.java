package com.devsphere.user.repository;

import com.devsphere.user.entity.ResumeSection;
import com.devsphere.user.entity.ResumeSectionType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResumeSectionRepository extends JpaRepository<ResumeSection, Long> {

    List<ResumeSection> findAllByResumeProfileIdOrderByDisplayOrderAscIdAsc(Long resumeProfileId);

    Optional<ResumeSection> findByIdAndResumeProfileId(Long id, Long resumeProfileId);

    Optional<ResumeSection> findByResumeProfileIdAndSectionType(Long resumeProfileId, ResumeSectionType sectionType);

    boolean existsByResumeProfileIdAndSectionType(Long resumeProfileId, ResumeSectionType sectionType);
}
