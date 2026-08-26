package com.devsphere.user;

import com.devsphere.user.dto.CreateProjectRequest;
import com.devsphere.user.dto.PageResponse;
import com.devsphere.user.dto.ProjectResponse;
import com.devsphere.user.dto.UpdateProjectRequest;
import com.devsphere.user.entity.ProjectStatus;
import com.devsphere.user.entity.ProjectType;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.repository.DeveloperProjectRepository;
import com.devsphere.user.service.ProjectService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class DeveloperProjectIntegrationTest {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private DeveloperProjectRepository projectRepository;

    @BeforeEach
    void cleanDatabase() {
        projectRepository.deleteAll();
    }

    @Test
    void projectCrudLifecycle_createsStartsHoldsResumesCompletesAndArchivesProject() {
        Long userId = 700L;
        CreateProjectRequest createReq = new CreateProjectRequest(
                "DevSphere Platform", "Microservices developer ecosystem", ProjectType.PERSONAL,
                "https://github.com/user/devsphere", "https://devsphere.io", "https://docs.devsphere.io",
                List.of("Java 21", "Spring Boot 3", "PostgreSQL", "Kafka"), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)
        );

        ProjectResponse created = projectService.createProject(userId, createReq);
        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("DevSphere Platform");
        assertThat(created.getStatus()).isEqualTo(ProjectStatus.PLANNED);
        assertThat(created.getProjectType()).isEqualTo(ProjectType.PERSONAL);
        assertThat(created.getTechStack()).containsExactly("Java 21", "Spring Boot 3", "PostgreSQL", "Kafka");
        assertThat(created.getCompletedAt()).isNull();

        ProjectResponse started = projectService.startProject(userId, created.getId());
        assertThat(started.getStatus()).isEqualTo(ProjectStatus.IN_PROGRESS);

        ProjectResponse onHold = projectService.holdProject(userId, created.getId());
        assertThat(onHold.getStatus()).isEqualTo(ProjectStatus.ON_HOLD);
        assertThat(onHold.getCompletedAt()).isNull();

        ProjectResponse resumed = projectService.resumeProject(userId, created.getId());
        assertThat(resumed.getStatus()).isEqualTo(ProjectStatus.IN_PROGRESS);

        ProjectResponse completed = projectService.completeProject(userId, created.getId());
        assertThat(completed.getStatus()).isEqualTo(ProjectStatus.COMPLETED);
        assertThat(completed.getCompletedAt()).isNotNull();
        Instant completedAtTimestamp = completed.getCompletedAt();

        projectService.archiveProject(userId, created.getId());

        ProjectResponse archived = projectService.getProject(userId, created.getId());
        assertThat(archived.getStatus()).isEqualTo(ProjectStatus.ARCHIVED);
        assertThat(archived.getCompletedAt()).isNotNull();
        assertThat(archived.getCompletedAt().toEpochMilli()).isEqualTo(completedAtTimestamp.toEpochMilli());
    }

    @Test
    void invalidStatusTransitions_throwIllegalArgumentException() {
        Long userId = 700L;
        CreateProjectRequest createReq = new CreateProjectRequest(
                "Archived Test Project", "Desc", ProjectType.LEARNING,
                null, null, null, null, null, null
        );

        ProjectResponse created = projectService.createProject(userId, createReq);
        projectService.archiveProject(userId, created.getId());

        assertThatThrownBy(() -> projectService.startProject(userId, created.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot transition project from ARCHIVED status");

        assertThatThrownBy(() -> projectService.completeProject(userId, created.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot transition project from ARCHIVED status");

        assertThatThrownBy(() -> projectService.holdProject(userId, created.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot transition project from ARCHIVED status");

        assertThatThrownBy(() -> projectService.resumeProject(userId, created.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Project can only be resumed from ON_HOLD status");
    }

    @Test
    void idorProtection_returnsNotFoundForOtherUsersProjects() {
        Long userA = 701L;
        Long userB = 702L;

        CreateProjectRequest createReq = new CreateProjectRequest(
                "User A Project", "Private project", ProjectType.PERSONAL,
                null, null, null, null, null, null
        );

        ProjectResponse projectA = projectService.createProject(userA, createReq);
        Long projectId = projectA.getId();

        assertThatThrownBy(() -> projectService.getProject(userB, projectId))
                .isInstanceOf(ResourceNotFoundException.class);

        UpdateProjectRequest updateReq = new UpdateProjectRequest(
                "Hacked Name", "Hacked Desc", ProjectType.PERSONAL,
                null, null, null, null, null, null
        );
        assertThatThrownBy(() -> projectService.updateProject(userB, projectId, updateReq))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThatThrownBy(() -> projectService.startProject(userB, projectId))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThatThrownBy(() -> projectService.completeProject(userB, projectId))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThatThrownBy(() -> projectService.holdProject(userB, projectId))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThatThrownBy(() -> projectService.resumeProject(userB, projectId))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThatThrownBy(() -> projectService.archiveProject(userB, projectId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listProjects_withFiltersAndPagination() {
        Long userId = 703L;

        ProjectResponse p1 = projectService.createProject(userId, new CreateProjectRequest("P1", "D1", ProjectType.PERSONAL, null, null, null, null, null, null));
        ProjectResponse p2 = projectService.createProject(userId, new CreateProjectRequest("P2", "D2", ProjectType.OPEN_SOURCE, null, null, null, null, null, null));
        ProjectResponse p3 = projectService.createProject(userId, new CreateProjectRequest("P3", "D3", ProjectType.OPEN_SOURCE, null, null, null, null, null, null));
        ProjectResponse p4 = projectService.createProject(userId, new CreateProjectRequest("P4", "D4", ProjectType.FREELANCE, null, null, null, null, null, null));

        projectService.startProject(userId, p2.getId());
        projectService.startProject(userId, p3.getId());
        projectService.completeProject(userId, p3.getId());
        projectService.archiveProject(userId, p4.getId());

        PageResponse<ProjectResponse> activeProjects = projectService.listProjects(userId, null, null, 0, 20);
        assertThat(activeProjects.getTotalElements()).isEqualTo(3);

        PageResponse<ProjectResponse> inProgressProjects = projectService.listProjects(userId, ProjectStatus.IN_PROGRESS, null, 0, 20);
        assertThat(inProgressProjects.getTotalElements()).isEqualTo(1);
        assertThat(inProgressProjects.getContent().get(0).getId()).isEqualTo(p2.getId());

        PageResponse<ProjectResponse> openSourceProjects = projectService.listProjects(userId, null, ProjectType.OPEN_SOURCE, 0, 20);
        assertThat(openSourceProjects.getTotalElements()).isEqualTo(2);

        PageResponse<ProjectResponse> combinedFilter = projectService.listProjects(userId, ProjectStatus.COMPLETED, ProjectType.OPEN_SOURCE, 0, 20);
        assertThat(combinedFilter.getTotalElements()).isEqualTo(1);
        assertThat(combinedFilter.getContent().get(0).getId()).isEqualTo(p3.getId());

        PageResponse<ProjectResponse> archivedProjects = projectService.listProjects(userId, ProjectStatus.ARCHIVED, null, 0, 20);
        assertThat(archivedProjects.getTotalElements()).isEqualTo(1);
        assertThat(archivedProjects.getContent().get(0).getId()).isEqualTo(p4.getId());
    }

    @Test
    void validation_targetEndDateBeforeStartDate_throwsIllegalArgumentException() {
        Long userId = 704L;
        CreateProjectRequest request = new CreateProjectRequest(
                "Invalid Dates", "Desc", ProjectType.PERSONAL,
                null, null, null, null,
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 1, 1)
        );

        assertThatThrownBy(() -> projectService.createProject(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("targetEndDate must not be before startDate");
    }
}
