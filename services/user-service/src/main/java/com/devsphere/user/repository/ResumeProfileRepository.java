package com.devsphere.user.repository;

import com.devsphere.user.entity.ResumeProfile;
import com.devsphere.user.entity.ResumeStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResumeProfileRepository extends JpaRepository<ResumeProfile, Long> {

    Optional<ResumeProfile> findByIdAndUserId(Long id, Long userId);

    List<ResumeProfile> findAllByUserIdOrderByIdDesc(Long userId);

    Optional<ResumeProfile> findByUserIdAndStatus(Long userId, ResumeStatus status);

    List<ResumeProfile> findAllByUserIdAndStatus(Long userId, ResumeStatus status);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT r FROM ResumeProfile r WHERE r.id = :id AND r.userId = :userId")
    Optional<ResumeProfile> findByIdAndUserIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id, @org.springframework.data.repository.query.Param("userId") Long userId);
}
