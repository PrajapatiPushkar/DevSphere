package com.devsphere.user.repository;

import com.devsphere.user.entity.PublicResumeViewLog;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PublicResumeViewLogRepository extends JpaRepository<PublicResumeViewLog, Long> {

    long countByResumeProfileId(Long resumeProfileId);

    @Query("SELECT COUNT(DISTINCT p.ipHash) FROM PublicResumeViewLog p WHERE p.resumeProfileId = :resumeProfileId")
    long countDistinctIpHashByResumeProfileId(@Param("resumeProfileId") Long resumeProfileId);

    @Query("SELECT MAX(p.accessedAt) FROM PublicResumeViewLog p WHERE p.resumeProfileId = :resumeProfileId")
    Optional<Instant> findLatestAccessedAtByResumeProfileId(@Param("resumeProfileId") Long resumeProfileId);

    @Query("SELECT COALESCE(p.referrer, 'direct'), COUNT(p) FROM PublicResumeViewLog p WHERE p.resumeProfileId = :resumeProfileId GROUP BY COALESCE(p.referrer, 'direct') ORDER BY COUNT(p) DESC")
    List<Object[]> findTopReferrersByResumeProfileId(@Param("resumeProfileId") Long resumeProfileId, Pageable pageable);

    @Query("SELECT p.accessedAt FROM PublicResumeViewLog p WHERE p.resumeProfileId = :resumeProfileId AND p.accessedAt >= :startDate")
    List<Instant> findAccessTimestampsSince(@Param("resumeProfileId") Long resumeProfileId, @Param("startDate") Instant startDate);
}
