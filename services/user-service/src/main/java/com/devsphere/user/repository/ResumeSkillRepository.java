package com.devsphere.user.repository;

import com.devsphere.user.entity.ResumeSkill;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResumeSkillRepository extends JpaRepository<ResumeSkill, Long> {

    List<ResumeSkill> findAllByResumeProfileIdOrderByDisplayOrderAsc(Long resumeProfileId);

    Optional<ResumeSkill> findByResumeProfileIdAndSkillId(Long resumeProfileId, Long skillId);

    boolean existsByResumeProfileIdAndSkillId(Long resumeProfileId, Long skillId);

    void deleteByResumeProfileIdAndSkillId(Long resumeProfileId, Long skillId);
}
