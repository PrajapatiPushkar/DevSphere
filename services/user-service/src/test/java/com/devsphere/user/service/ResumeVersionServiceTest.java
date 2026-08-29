package com.devsphere.user.service;

import com.devsphere.user.dto.CreateResumeVersionRequest;
import com.devsphere.user.dto.ResumeVersionResponse;
import com.devsphere.user.dto.compilation.CompiledResumeResponse;
import com.devsphere.user.entity.ResumeProfile;
import com.devsphere.user.entity.ResumeTemplate;
import com.devsphere.user.entity.ResumeVersion;
import com.devsphere.user.entity.ResumeVersionStatus;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.repository.ResumeProfileRepository;
import com.devsphere.user.repository.ResumeVersionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResumeVersionServiceTest {

    private ResumeVersionRepository resumeVersionRepository;
    private ResumeProfileRepository resumeProfileRepository;
    private ResumeCompilationService resumeCompilationService;
    private ObjectMapper objectMapper;
    private SimpleMeterRegistry meterRegistry;
    private ResumeVersionService service;

    private ResumeProfile testProfile;
    private CompiledResumeResponse testCompiledResponse;

    @BeforeEach
    void setUp() {
        resumeVersionRepository = mock(ResumeVersionRepository.class);
        resumeProfileRepository = mock(ResumeProfileRepository.class);
        resumeCompilationService = mock(ResumeCompilationService.class);
        objectMapper = new ObjectMapper();
        meterRegistry = new SimpleMeterRegistry();

        service = new ResumeVersionService(
                resumeVersionRepository,
                resumeProfileRepository,
                resumeCompilationService,
                objectMapper,
                meterRegistry
        );

        testProfile = new ResumeProfile(100L, "Backend Resume", "Senior Java Engineer", ResumeTemplate.PROFESSIONAL);
        testProfile.setId(1L);

        testCompiledResponse = new CompiledResumeResponse(
                1L, 1L, "Backend Resume", "Senior Java Engineer", ResumeTemplate.PROFESSIONAL, List.of()
        );
    }

    @Test
    @DisplayName("Create version successfully starting at version 1")
    void createVersion_Success_StartsAtVersionOne() throws Exception {
        when(resumeProfileRepository.findByIdAndUserId(1L, 100L)).thenReturn(Optional.of(testProfile));
        when(resumeCompilationService.compileResume(1L, 100L)).thenReturn(testCompiledResponse);
        when(resumeVersionRepository.findMaxVersionNumberByResumeProfileId(1L)).thenReturn(Optional.empty());

        when(resumeVersionRepository.save(any(ResumeVersion.class))).thenAnswer(inv -> {
            ResumeVersion v = inv.getArgument(0);
            v.setId(10L);
            return v;
        });

        CreateResumeVersionRequest request = new CreateResumeVersionRequest("Initial Draft");
        ResumeVersionResponse response = service.createVersion(1L, 100L, request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getResumeProfileId()).isEqualTo(1L);
        assertThat(response.getUserId()).isEqualTo(100L);
        assertThat(response.getVersionNumber()).isEqualTo(1);
        assertThat(response.getName()).isEqualTo("Initial Draft");
        assertThat(response.getStatus()).isEqualTo(ResumeVersionStatus.DRAFT);
        assertThat(response.getSnapshot()).isNotNull();
        assertThat(response.getSnapshot().getTargetRole()).isEqualTo("Senior Java Engineer");
    }

    @Test
    @DisplayName("Create version increments version number")
    void createVersion_IncrementsVersionNumber() {
        when(resumeProfileRepository.findByIdAndUserId(1L, 100L)).thenReturn(Optional.of(testProfile));
        when(resumeCompilationService.compileResume(1L, 100L)).thenReturn(testCompiledResponse);
        when(resumeVersionRepository.findMaxVersionNumberByResumeProfileId(1L)).thenReturn(Optional.of(2));

        when(resumeVersionRepository.save(any(ResumeVersion.class))).thenAnswer(inv -> {
            ResumeVersion v = inv.getArgument(0);
            v.setId(12L);
            return v;
        });

        ResumeVersionResponse response = service.createVersion(1L, 100L, null);

        assertThat(response.getVersionNumber()).isEqualTo(3);
        assertThat(response.getName()).isEqualTo("Version 3");
    }

    @Test
    @DisplayName("Create version enforces ownership and throws ResourceNotFoundException on non-owner access")
    void createVersion_EnforcesOwnership() {
        when(resumeProfileRepository.findByIdAndUserId(1L, 999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createVersion(1L, 999L, new CreateResumeVersionRequest("Title")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Resume profile not found");
    }

    @Test
    @DisplayName("Publish version transitions DRAFT to PUBLISHED and archives previously published version")
    void publishVersion_Success_ArchivesPreviousPublishedVersion() throws Exception {
        String json = objectMapper.writeValueAsString(testCompiledResponse);
        ResumeVersion prevPublished = new ResumeVersion(1L, 100L, 1, "V1", json);
        prevPublished.setId(4L);
        prevPublished.setStatus(ResumeVersionStatus.PUBLISHED);

        ResumeVersion draft = new ResumeVersion(1L, 100L, 2, "V2", json);
        draft.setId(5L);

        when(resumeProfileRepository.findByIdAndUserIdForUpdate(1L, 100L)).thenReturn(Optional.of(testProfile));
        when(resumeVersionRepository.findByIdAndResumeProfileIdAndUserId(5L, 1L, 100L)).thenReturn(Optional.of(draft));
        when(resumeVersionRepository.findByResumeProfileIdAndStatus(1L, ResumeVersionStatus.PUBLISHED)).thenReturn(Optional.of(prevPublished));
        when(resumeVersionRepository.save(any(ResumeVersion.class))).thenAnswer(inv -> inv.getArgument(0));

        ResumeVersionResponse response = service.publishVersion(1L, 5L, 100L);

        assertThat(response.getStatus()).isEqualTo(ResumeVersionStatus.PUBLISHED);
        assertThat(response.getPublishedAt()).isNotNull();
        assertThat(prevPublished.getStatus()).isEqualTo(ResumeVersionStatus.ARCHIVED);
        assertThat(prevPublished.getArchivedAt()).isNotNull();
    }

    @Test
    @DisplayName("Publishing an already published version throws IllegalArgumentException")
    void publishVersion_FromPublished_ThrowsException() throws Exception {
        String json = objectMapper.writeValueAsString(testCompiledResponse);
        ResumeVersion published = new ResumeVersion(1L, 100L, 1, "V1", json);
        published.setId(5L);
        published.setStatus(ResumeVersionStatus.PUBLISHED);

        when(resumeProfileRepository.findByIdAndUserIdForUpdate(1L, 100L)).thenReturn(Optional.of(testProfile));
        when(resumeVersionRepository.findByIdAndResumeProfileIdAndUserId(5L, 1L, 100L)).thenReturn(Optional.of(published));

        assertThatThrownBy(() -> service.publishVersion(1L, 5L, 100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only DRAFT versions can be published");
    }

    @Test
    @DisplayName("Publishing an archived version throws IllegalArgumentException")
    void publishVersion_FromArchived_ThrowsException() throws Exception {
        String json = objectMapper.writeValueAsString(testCompiledResponse);
        ResumeVersion archived = new ResumeVersion(1L, 100L, 1, "V1", json);
        archived.setId(5L);
        archived.setStatus(ResumeVersionStatus.ARCHIVED);

        when(resumeProfileRepository.findByIdAndUserIdForUpdate(1L, 100L)).thenReturn(Optional.of(testProfile));
        when(resumeVersionRepository.findByIdAndResumeProfileIdAndUserId(5L, 1L, 100L)).thenReturn(Optional.of(archived));

        assertThatThrownBy(() -> service.publishVersion(1L, 5L, 100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only DRAFT versions can be published");
    }

    @Test
    @DisplayName("Get published version returns current published version")
    void getPublishedVersion_Success() throws Exception {
        String json = objectMapper.writeValueAsString(testCompiledResponse);
        ResumeVersion published = new ResumeVersion(1L, 100L, 2, "V2 Published", json);
        published.setId(10L);
        published.setStatus(ResumeVersionStatus.PUBLISHED);

        when(resumeProfileRepository.findByIdAndUserId(1L, 100L)).thenReturn(Optional.of(testProfile));
        when(resumeVersionRepository.findByResumeProfileIdAndUserIdAndStatus(1L, 100L, ResumeVersionStatus.PUBLISHED))
                .thenReturn(Optional.of(published));

        ResumeVersionResponse response = service.getPublishedVersion(1L, 100L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getStatus()).isEqualTo(ResumeVersionStatus.PUBLISHED);
    }

    @Test
    @DisplayName("Get published version throws ResourceNotFoundException when no published version exists")
    void getPublishedVersion_NotFound_ThrowsResourceNotFoundException() {
        when(resumeProfileRepository.findByIdAndUserId(1L, 100L)).thenReturn(Optional.of(testProfile));
        when(resumeVersionRepository.findByResumeProfileIdAndUserIdAndStatus(1L, 100L, ResumeVersionStatus.PUBLISHED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPublishedVersion(1L, 100L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No published resume version found");
    }

    @Test
    @DisplayName("Archive version transitions PUBLISHED to ARCHIVED and sets archivedAt")
    void archiveVersion_Success() throws Exception {
        String json = objectMapper.writeValueAsString(testCompiledResponse);
        ResumeVersion pub = new ResumeVersion(1L, 100L, 1, "V1", json);
        pub.setId(5L);
        pub.setStatus(ResumeVersionStatus.PUBLISHED);

        when(resumeProfileRepository.findByIdAndUserId(1L, 100L)).thenReturn(Optional.of(testProfile));
        when(resumeVersionRepository.findByIdAndResumeProfileIdAndUserId(5L, 1L, 100L)).thenReturn(Optional.of(pub));
        when(resumeVersionRepository.save(any(ResumeVersion.class))).thenAnswer(inv -> inv.getArgument(0));

        ResumeVersionResponse response = service.archiveVersion(1L, 5L, 100L);

        assertThat(response.getStatus()).isEqualTo(ResumeVersionStatus.ARCHIVED);
        assertThat(response.getArchivedAt()).isNotNull();
    }

    @Test
    @DisplayName("List versions returns versions ordered by versionNumber DESC")
    void listVersions_Ordered() throws Exception {
        String json = objectMapper.writeValueAsString(testCompiledResponse);
        ResumeVersion v1 = new ResumeVersion(1L, 100L, 1, "V1", json);
        v1.setId(1L);
        ResumeVersion v2 = new ResumeVersion(1L, 100L, 2, "V2", json);
        v2.setId(2L);

        when(resumeProfileRepository.findByIdAndUserId(1L, 100L)).thenReturn(Optional.of(testProfile));
        when(resumeVersionRepository.findAllByResumeProfileIdAndUserIdOrderByVersionNumberDesc(1L, 100L))
                .thenReturn(List.of(v2, v1));

        List<ResumeVersionResponse> list = service.listVersions(1L, 100L);

        assertThat(list).hasSize(2);
        assertThat(list.get(0).getVersionNumber()).isEqualTo(2);
        assertThat(list.get(1).getVersionNumber()).isEqualTo(1);
    }

    @Test
    @DisplayName("Version name validation sanitizes HTML and rejects control characters")
    void createVersion_VersionNameValidation() {
        when(resumeProfileRepository.findByIdAndUserId(1L, 100L)).thenReturn(Optional.of(testProfile));
        when(resumeCompilationService.compileResume(1L, 100L)).thenReturn(testCompiledResponse);
        when(resumeVersionRepository.findMaxVersionNumberByResumeProfileId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createVersion(1L, 100L, new CreateResumeVersionRequest("Bad\nName")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("control characters");
    }
}
