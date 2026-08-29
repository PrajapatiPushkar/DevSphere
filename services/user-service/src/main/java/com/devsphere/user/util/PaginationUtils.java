package com.devsphere.user.util;

import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PaginationUtils {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    private PaginationUtils() {
    }

    public static Pageable createPageable(int page, int size, String sortParam, Set<String> allowedSortFields, String defaultSortField, Sort.Direction defaultDirection) {
        if (page < 0) {
            throw new IllegalArgumentException("Page index must not be negative");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Page size must be greater than zero");
        }
        if (size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Page size must not exceed " + MAX_PAGE_SIZE);
        }

        String field = defaultSortField;
        Sort.Direction direction = defaultDirection;

        if (sortParam != null && !sortParam.isBlank()) {
            String[] parts = sortParam.split(",");
            field = parts[0].trim();

            if (!allowedSortFields.contains(field)) {
                throw new IllegalArgumentException("Invalid sort field: '" + field + "'. Allowed sort fields: " + allowedSortFields);
            }

            if (parts.length > 1) {
                String dirStr = parts[1].trim().toLowerCase();
                if ("asc".equals(dirStr)) {
                    direction = Sort.Direction.ASC;
                } else if ("desc".equals(dirStr)) {
                    direction = Sort.Direction.DESC;
                } else {
                    throw new IllegalArgumentException("Invalid sort direction: '" + parts[1] + "'. Allowed values: asc, desc");
                }
            }
        }

        Sort sort = Sort.by(direction, field);
        if (!"id".equalsIgnoreCase(field)) {
            sort = sort.and(Sort.by(Sort.Direction.DESC, "id"));
        }

        return PageRequest.of(page, size, sort);
    }
}
