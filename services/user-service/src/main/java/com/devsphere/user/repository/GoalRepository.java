package com.devsphere.user.repository;

import com.devsphere.user.entity.Goal;
import com.devsphere.user.entity.GoalStatus;
import com.devsphere.user.entity.GoalType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface GoalRepository extends JpaRepository<Goal, Long>, JpaSpecificationExecutor<Goal> {

    Optional<Goal> findByIdAndUserId(Long id, Long userId);

    Page<Goal> findAllByUserId(Long userId, Pageable pageable);

    Page<Goal> findAllByUserIdAndStatus(Long userId, GoalStatus status, Pageable pageable);

    Page<Goal> findAllByUserIdAndGoalType(Long userId, GoalType goalType, Pageable pageable);

    Page<Goal> findAllByUserIdAndStatusAndGoalType(Long userId, GoalStatus status, GoalType goalType, Pageable pageable);
}
