package com.devsphere.user.repository;

import com.devsphere.user.entity.Task;
import com.devsphere.user.entity.TaskPriority;
import com.devsphere.user.entity.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    Optional<Task> findByIdAndUserId(Long id, Long userId);

    Page<Task> findAllByUserId(Long userId, Pageable pageable);

    Page<Task> findAllByUserIdAndStatus(Long userId, TaskStatus status, Pageable pageable);

    Page<Task> findAllByUserIdAndPriority(Long userId, TaskPriority priority, Pageable pageable);

    Page<Task> findAllByUserIdAndGoalId(Long userId, Long goalId, Pageable pageable);
}
