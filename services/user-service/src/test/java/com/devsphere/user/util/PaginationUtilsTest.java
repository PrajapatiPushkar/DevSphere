package com.devsphere.user.util;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaginationUtilsTest {

    private static final Set<String> ALLOWED_FIELDS = Set.of("createdAt", "title", "status", "id");

    @Test
    void createPageable_withValidDefaults_returnsPageableWithSecondarySort() {
        Pageable pageable = PaginationUtils.createPageable(0, 20, null, ALLOWED_FIELDS, "createdAt", Sort.Direction.DESC);

        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(20);

        Sort.Order primary = pageable.getSort().getOrderFor("createdAt");
        Sort.Order secondary = pageable.getSort().getOrderFor("id");

        assertThat(primary).isNotNull();
        assertThat(primary.getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(secondary).isNotNull();
        assertThat(secondary.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void createPageable_withCustomSortAsc_parsesDirectionAndField() {
        Pageable pageable = PaginationUtils.createPageable(1, 50, "title,asc", ALLOWED_FIELDS, "createdAt", Sort.Direction.DESC);

        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(50);

        Sort.Order primary = pageable.getSort().getOrderFor("title");
        assertThat(primary).isNotNull();
        assertThat(primary.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void createPageable_withNegativePage_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> PaginationUtils.createPageable(-1, 20, null, ALLOWED_FIELDS, "createdAt", Sort.Direction.DESC))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Page index must not be negative");
    }

    @Test
    void createPageable_withZeroOrNegativeSize_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> PaginationUtils.createPageable(0, 0, null, ALLOWED_FIELDS, "createdAt", Sort.Direction.DESC))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Page size must be greater than zero");

        assertThatThrownBy(() -> PaginationUtils.createPageable(0, -5, null, ALLOWED_FIELDS, "createdAt", Sort.Direction.DESC))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Page size must be greater than zero");
    }

    @Test
    void createPageable_withOversizedSize_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> PaginationUtils.createPageable(0, 101, null, ALLOWED_FIELDS, "createdAt", Sort.Direction.DESC))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Page size must not exceed 100");
    }

    @Test
    void createPageable_withInvalidSortField_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> PaginationUtils.createPageable(0, 20, "unsupportedField,desc", ALLOWED_FIELDS, "createdAt", Sort.Direction.DESC))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid sort field: 'unsupportedField'");
    }

    @Test
    void createPageable_withInvalidSortDirection_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> PaginationUtils.createPageable(0, 20, "createdAt,invalidDir", ALLOWED_FIELDS, "createdAt", Sort.Direction.DESC))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid sort direction: 'invalidDir'");
    }
}
