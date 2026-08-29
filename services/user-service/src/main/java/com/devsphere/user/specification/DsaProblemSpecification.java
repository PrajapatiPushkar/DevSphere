package com.devsphere.user.specification;

import com.devsphere.user.entity.DsaDifficulty;
import com.devsphere.user.entity.DsaPlatform;
import com.devsphere.user.entity.DsaProblem;
import com.devsphere.user.entity.DsaProblemStatus;
import com.devsphere.user.entity.DsaTopic;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public class DsaProblemSpecification {

    public static Specification<DsaProblem> filterProblems(Long userId, DsaDifficulty difficulty, DsaTopic topic, DsaPlatform platform, DsaProblemStatus status) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.equal(root.get("userId"), userId));

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            } else {
                predicates.add(criteriaBuilder.notEqual(root.get("status"), DsaProblemStatus.ARCHIVED));
            }

            if (difficulty != null) {
                predicates.add(criteriaBuilder.equal(root.get("difficulty"), difficulty));
            }

            if (topic != null) {
                predicates.add(criteriaBuilder.equal(root.get("topic"), topic));
            }

            if (platform != null) {
                predicates.add(criteriaBuilder.equal(root.get("platform"), platform));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
