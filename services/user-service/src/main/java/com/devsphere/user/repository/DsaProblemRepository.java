package com.devsphere.user.repository;

import com.devsphere.user.entity.DsaPlatform;
import com.devsphere.user.entity.DsaProblem;
import com.devsphere.user.entity.DsaProblemStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DsaProblemRepository extends JpaRepository<DsaProblem, Long>, JpaSpecificationExecutor<DsaProblem> {

    Optional<DsaProblem> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndPlatformAndProblemUrlAndStatusNot(Long userId, DsaPlatform platform, String problemUrl, DsaProblemStatus status);

    List<DsaProblem> findAllByUserId(Long userId);

    List<DsaProblem> findAllByUserIdAndStatusNot(Long userId, DsaProblemStatus status);

    List<DsaProblem> findAllByUserIdAndSolvedAtBetween(Long userId, Instant start, Instant end);
}
