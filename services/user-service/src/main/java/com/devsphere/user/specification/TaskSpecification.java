package com.devsphere.user.specification;

import com.devsphere.user.entity.Task;
import com.devsphere.user.entity.TaskPriority;
import com.devsphere.user.entity.TaskStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class TaskSpecification {

    public static Specification<Task> filterTasks(Long userId, TaskStatus status, TaskPriority priority, Long goalId) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.equal(root.get("userId"), userId));

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (priority != null) {
                predicates.add(criteriaBuilder.equal(root.get("priority"), priority));
            }

            if (goalId != null) {
                predicates.add(criteriaBuilder.equal(root.get("goalId"), goalId));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
