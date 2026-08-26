package com.devsphere.user.repository;

import com.devsphere.user.entity.ResumeCertification;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResumeCertificationRepository extends JpaRepository<ResumeCertification, Long> {

    List<ResumeCertification> findAllByResumeProfileIdOrderByDisplayOrderAsc(Long resumeProfileId);

    Optional<ResumeCertification> findByResumeProfileIdAndCertificationId(Long resumeProfileId, Long certificationId);

    boolean existsByResumeProfileIdAndCertificationId(Long resumeProfileId, Long certificationId);

    void deleteByResumeProfileIdAndCertificationId(Long resumeProfileId, Long certificationId);
}
