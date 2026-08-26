package com.devsphere.user.specification;

import com.devsphere.user.entity.DeveloperProject;
import com.devsphere.user.entity.ProjectStatus;
import com.devsphere.user.entity.ProjectType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public class ProjectSpecification {

    public static Specification<DeveloperProject> filterProjects(Long userId, ProjectStatus status, ProjectType projectType) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.equal(root.get("userId"), userId));

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            } else {
                predicates.add(criteriaBuilder.notEqual(root.get("status"), ProjectStatus.ARCHIVED));
            }

            if (projectType != null) {
                predicates.add(criteriaBuilder.equal(root.get("projectType"), projectType));
            }

            if (query != null) {
                query.orderBy(criteriaBuilder.desc(root.get("createdAt")));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
