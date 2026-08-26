package com.devsphere.user.service;

import com.devsphere.user.dto.CreateProjectRequest;
import com.devsphere.user.dto.PageResponse;
import com.devsphere.user.dto.ProjectResponse;
import com.devsphere.user.dto.UpdateProjectRequest;
import com.devsphere.user.entity.DeveloperProject;
import com.devsphere.user.entity.ProjectStatus;
import com.devsphere.user.entity.ProjectType;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.repository.DeveloperProjectRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private DeveloperProjectRepository projectRepository;

    private SimpleMeterRegistry meterRegistry;
    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        projectService = new ProjectService(projectRepository, meterRegistry);
    }

    @Test
    void createProject_savesProjectAndIncrementsMetrics() {
        Long userId = 100L;
        CreateProjectRequest request = new CreateProjectRequest(
                " DevSphere ", "Microservices platform", ProjectType.PERSONAL,
                "https://github.com/user/devsphere", "https://devsphere.io", "https://docs.devsphere.io",
                List.of("Java", "Spring Boot", "PostgreSQL"), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)
        );

        when(projectRepository.save(any(DeveloperProject.class))).thenAnswer(invocation -> {
            DeveloperProject p = invocation.getArgument(0);
            p.setId(1L);
            return p;
        });

        ProjectResponse response = projectService.createProject(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getName()).isEqualTo("DevSphere");
        assertThat(response.getStatus()).isEqualTo(ProjectStatus.PLANNED);
        assertThat(response.getProjectType()).isEqualTo(ProjectType.PERSONAL);
        assertThat(response.getTechStack()).containsExactly("Java", "Spring Boot", "PostgreSQL");
        assertThat(response.getCompletedAt()).isNull();

        assertThat(meterRegistry.find("devsphere_projects_created_total").counter().count()).isEqualTo(1.0);
    }

    @Test
    void createProject_withTargetDateBeforeStartDate_throwsIllegalArgumentException() {
        Long userId = 100L;
        CreateProjectRequest request = new CreateProjectRequest(
                "Invalid Dates Project", "Desc", ProjectType.PERSONAL,
                null, null, null, null,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 5, 1)
        );

        assertThatThrownBy(() -> projectService.createProject(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("targetEndDate must not be before startDate");
    }

    @Test
    void createProject_withBlankName_throwsIllegalArgumentException() {
        Long userId = 100L;
        CreateProjectRequest request = new CreateProjectRequest(
                "   ", "Desc", ProjectType.PERSONAL,
                null, null, null, null, null, null
        );

        assertThatThrownBy(() -> projectService.createProject(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Project name is required");
    }

    @Test
    void getProject_returnsProject_whenOwned() {
        Long userId = 100L;
        Long projectId = 1L;
        DeveloperProject project = new DeveloperProject(userId, "DevSphere", ProjectType.PERSONAL);
        project.setId(projectId);

        when(projectRepository.findByIdAndUserId(projectId, userId)).thenReturn(Optional.of(project));

        ProjectResponse response = projectService.getProject(userId, projectId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(projectId);
        assertThat(response.getName()).isEqualTo("DevSphere");
    }

    @Test
    void getProject_throwsResourceNotFoundException_whenNotOwned() {
        Long userId = 100L;
        Long otherUserId = 200L;
        Long projectId = 1L;

        when(projectRepository.findByIdAndUserId(projectId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getProject(userId, projectId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Developer project not found with id: 1");
    }

    @Test
    void startProject_transitionsPlannedToInProgress() {
        Long userId = 100L;
        Long projectId = 1L;
        DeveloperProject project = new DeveloperProject(userId, "DevSphere", ProjectType.PERSONAL);
        project.setId(projectId);
        project.setStatus(ProjectStatus.PLANNED);

        when(projectRepository.findByIdAndUserId(projectId, userId)).thenReturn(Optional.of(project));
        when(projectRepository.save(any(DeveloperProject.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectResponse response = projectService.startProject(userId, projectId);

        assertThat(response.getStatus()).isEqualTo(ProjectStatus.IN_PROGRESS);
    }

    @Test
    void completeProject_transitionsInProgressToCompleted_andSetsCompletedAt() {
        Long userId = 100L;
        Long projectId = 1L;
        DeveloperProject project = new DeveloperProject(userId, "DevSphere", ProjectType.PERSONAL);
        project.setId(projectId);
        project.setStatus(ProjectStatus.IN_PROGRESS);

        when(projectRepository.findByIdAndUserId(projectId, userId)).thenReturn(Optional.of(project));
        when(projectRepository.save(any(DeveloperProject.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectResponse response = projectService.completeProject(userId, projectId);

        assertThat(response.getStatus()).isEqualTo(ProjectStatus.COMPLETED);
        assertThat(response.getCompletedAt()).isNotNull();
        assertThat(meterRegistry.find("devsphere_projects_completed_total").counter().count()).isEqualTo(1.0);
    }

    @Test
    void resumeProject_transitionsOnHoldToInProgress() {
        Long userId = 100L;
        Long projectId = 1L;
        DeveloperProject project = new DeveloperProject(userId, "DevSphere", ProjectType.PERSONAL);
        project.setId(projectId);
        project.setStatus(ProjectStatus.ON_HOLD);

        when(projectRepository.findByIdAndUserId(projectId, userId)).thenReturn(Optional.of(project));
        when(projectRepository.save(any(DeveloperProject.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectResponse response = projectService.resumeProject(userId, projectId);

        assertThat(response.getStatus()).isEqualTo(ProjectStatus.IN_PROGRESS);
    }

    @Test
    void resumeProject_fromPlannedStatus_throwsIllegalArgumentException() {
        Long userId = 100L;
        Long projectId = 1L;
        DeveloperProject project = new DeveloperProject(userId, "DevSphere", ProjectType.PERSONAL);
        project.setId(projectId);
        project.setStatus(ProjectStatus.PLANNED);

        when(projectRepository.findByIdAndUserId(projectId, userId)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectService.resumeProject(userId, projectId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Project can only be resumed from ON_HOLD status");
    }

    @Test
    void archiveProject_setsStatusToArchived() {
        Long userId = 100L;
        Long projectId = 1L;
        DeveloperProject project = new DeveloperProject(userId, "DevSphere", ProjectType.PERSONAL);
        project.setId(projectId);
        project.setStatus(ProjectStatus.IN_PROGRESS);

        when(projectRepository.findByIdAndUserId(projectId, userId)).thenReturn(Optional.of(project));

        projectService.archiveProject(userId, projectId);

        verify(projectRepository).save(project);
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.ARCHIVED);
        assertThat(meterRegistry.find("devsphere_projects_archived_total").counter().count()).isEqualTo(1.0);
    }

    @Test
    void invalidTransition_fromArchivedToInProgress_throwsIllegalArgumentException() {
        Long userId = 100L;
        Long projectId = 1L;
        DeveloperProject project = new DeveloperProject(userId, "DevSphere", ProjectType.PERSONAL);
        project.setId(projectId);
        project.setStatus(ProjectStatus.ARCHIVED);

        when(projectRepository.findByIdAndUserId(projectId, userId)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectService.startProject(userId, projectId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot transition project from ARCHIVED status");
    }
}
