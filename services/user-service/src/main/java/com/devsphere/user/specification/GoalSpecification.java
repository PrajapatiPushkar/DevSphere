package com.devsphere.user.specification;

import com.devsphere.user.entity.Goal;
import com.devsphere.user.entity.GoalStatus;
import com.devsphere.user.entity.GoalType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public class GoalSpecification {

    public static Specification<Goal> filterGoals(Long userId, GoalStatus status, GoalType goalType) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.equal(root.get("userId"), userId));

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (goalType != null) {
                predicates.add(criteriaBuilder.equal(root.get("goalType"), goalType));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
