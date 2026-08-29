package com.devsphere.user.repository;

import com.devsphere.user.entity.ResumeVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ResumeVersionRepository extends JpaRepository<ResumeVersion, Long> {

    Optional<ResumeVersion> findByIdAndUserId(Long id, Long userId);

    Optional<ResumeVersion> findByIdAndResumeProfileIdAndUserId(Long id, Long resumeProfileId, Long userId);

    List<ResumeVersion> findAllByResumeProfileIdAndUserIdOrderByVersionNumberDesc(Long resumeProfileId, Long userId);

    Optional<ResumeVersion> findByResumeProfileIdAndVersionNumberAndUserId(Long resumeProfileId, Integer versionNumber, Long userId);

    @Query("SELECT MAX(v.versionNumber) FROM ResumeVersion v WHERE v.resumeProfileId = :resumeProfileId")
    Optional<Integer> findMaxVersionNumberByResumeProfileId(@Param("resumeProfileId") Long resumeProfileId);
}
