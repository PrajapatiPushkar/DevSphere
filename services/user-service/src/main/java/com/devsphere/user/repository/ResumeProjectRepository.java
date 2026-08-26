package com.devsphere.user.repository;

import com.devsphere.user.entity.ResumeProject;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResumeProjectRepository extends JpaRepository<ResumeProject, Long> {

    List<ResumeProject> findAllByResumeProfileIdOrderByDisplayOrderAsc(Long resumeProfileId);

    Optional<ResumeProject> findByResumeProfileIdAndProjectId(Long resumeProfileId, Long projectId);

    boolean existsByResumeProfileIdAndProjectId(Long resumeProfileId, Long projectId);

    void deleteByResumeProfileIdAndProjectId(Long resumeProfileId, Long projectId);
}
