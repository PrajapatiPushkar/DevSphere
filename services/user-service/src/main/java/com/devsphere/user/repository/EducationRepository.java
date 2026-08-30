package com.devsphere.user.repository;

import com.devsphere.user.entity.Education;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EducationRepository extends JpaRepository<Education, Long> {

    Optional<Education> findByIdAndUserId(Long id, Long userId);

    List<Education> findAllByUserIdOrderByDisplayOrderAscStartDateDesc(Long userId);

    List<Education> findAllByIdInAndUserId(List<Long> ids, Long userId);
}
