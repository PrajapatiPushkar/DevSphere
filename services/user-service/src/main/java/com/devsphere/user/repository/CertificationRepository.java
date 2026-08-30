package com.devsphere.user.repository;

import com.devsphere.user.entity.Certification;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CertificationRepository extends JpaRepository<Certification, Long> {

    Optional<Certification> findByIdAndUserId(Long id, Long userId);

    List<Certification> findAllByUserIdOrderByDisplayOrderAscIssueDateDesc(Long userId);

    List<Certification> findAllByIdInAndUserId(List<Long> ids, Long userId);
}
