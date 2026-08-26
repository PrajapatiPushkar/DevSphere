package com.devsphere.user.repository;

import com.devsphere.user.entity.PlannerEntry;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlannerEntryRepository extends JpaRepository<PlannerEntry, Long> {

    Optional<PlannerEntry> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndTaskIdAndPlannedDate(Long userId, Long taskId, LocalDate plannedDate);

    List<PlannerEntry> findAllByUserIdAndPlannedDateOrderBySortOrderAscStartTimeAscCreatedAtAsc(Long userId, LocalDate plannedDate);

    Page<PlannerEntry> findAllByUserId(Long userId, Pageable pageable);

    Page<PlannerEntry> findAllByUserIdAndPlannedDate(Long userId, LocalDate plannedDate, Pageable pageable);

    List<PlannerEntry> findAllByUserIdAndPlannedDateAndIdIn(Long userId, LocalDate plannedDate, Collection<Long> ids);

    Optional<PlannerEntry> findByUserIdAndTaskIdAndPlannedDate(Long userId, Long taskId, LocalDate plannedDate);
}
