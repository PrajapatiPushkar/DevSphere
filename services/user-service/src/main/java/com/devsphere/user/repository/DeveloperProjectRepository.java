package com.devsphere.user.repository;

import com.devsphere.user.entity.DeveloperProject;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DeveloperProjectRepository extends JpaRepository<DeveloperProject, Long>, JpaSpecificationExecutor<DeveloperProject> {

    Optional<DeveloperProject> findByIdAndUserId(Long id, Long userId);

    Page<DeveloperProject> findAllByUserId(Long userId, Pageable pageable);

    java.util.List<DeveloperProject> findAllByIdInAndUserId(java.util.List<Long> ids, Long userId);
}
