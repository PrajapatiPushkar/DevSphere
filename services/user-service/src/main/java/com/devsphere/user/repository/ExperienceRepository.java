package com.devsphere.user.repository;

import com.devsphere.user.entity.Experience;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExperienceRepository extends JpaRepository<Experience, Long> {

    Optional<Experience> findByIdAndUserId(Long id, Long userId);

    List<Experience> findAllByUserIdOrderByDisplayOrderAscStartDateDesc(Long userId);

    List<Experience> findAllByIdInAndUserId(List<Long> ids, Long userId);
}
