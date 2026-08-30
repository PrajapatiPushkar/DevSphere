package com.devsphere.user;

import com.devsphere.user.dto.publicresume.PublicResumeResponse;
import com.devsphere.user.dto.publicresume.PublicShareStatusResponse;
import com.devsphere.user.entity.ResumeProfile;
import com.devsphere.user.entity.ResumeTemplate;
import com.devsphere.user.repository.ResumeProfileRepository;
import com.devsphere.user.repository.ResumeVersionRepository;
import com.devsphere.user.service.PublicResumeService;
import com.devsphere.user.service.ResumeVersionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PublicResumeIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ResumeProfileRepository resumeProfileRepository;

    @Autowired
    private ResumeVersionRepository resumeVersionRepository;

    @Autowired
    private ResumeVersionService resumeVersionService;

    @Autowired
    private PublicResumeService publicResumeService;

    private Long userId = 500L;

    @BeforeEach
    void setUp() {
        resumeVersionRepository.deleteAll();
        resumeProfileRepository.deleteAll();
    }

    @Test
    void getPublicResume_WithoutJwt_Returns200AndPresentationDataOnly() {
        // 1. Create profile
        ResumeProfile profile = new ResumeProfile(userId, "Cloud Engineer", "DevOps Specialist", ResumeTemplate.MODERN);
        profile = resumeProfileRepository.save(profile);

        // 2. Create version 1 (DRAFT)
        com.devsphere.user.dto.ResumeVersionResponse version1 = resumeVersionService.createVersion(profile.getId(), userId, new com.devsphere.user.dto.CreateResumeVersionRequest("Version 1"));

        // 3. Verify public endpoint returns 404 when status is DRAFT
        ResponseEntity<String> unpublishedResp = restTemplate.getForEntity("/api/v1/public/resumes/" + profile.getPublicId(), String.class);
        assertThat(unpublishedResp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // 4. Publish version 1
        resumeVersionService.publishVersion(profile.getId(), version1.getId(), userId);

        // 5. Enable public sharing
        publicResumeService.enablePublicSharing(profile.getId(), userId);

        // 6. Access public endpoint WITHOUT authentication
        ResponseEntity<PublicResumeResponse> publicResp = restTemplate.getForEntity("/api/v1/public/resumes/" + profile.getPublicId(), PublicResumeResponse.class);
        assertThat(publicResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(publicResp.getBody()).isNotNull();
        assertThat(publicResp.getBody().getName()).isEqualTo("Cloud Engineer");
        assertThat(publicResp.getBody().getTargetRole()).isEqualTo("DevOps Specialist");
        assertThat(publicResp.getBody().getTemplate()).isEqualTo(ResumeTemplate.MODERN);

        // 7. Mutate live profile data (e.g. change name to "Architect")
        profile = resumeProfileRepository.findById(profile.getId()).orElseThrow();
        profile.setName("Architect");
        resumeProfileRepository.save(profile);

        // 8. Verify public endpoint still returns published snapshot ("Cloud Engineer"), preserving immutability!
        ResponseEntity<PublicResumeResponse> cachedResp = restTemplate.getForEntity("/api/v1/public/resumes/" + profile.getPublicId(), PublicResumeResponse.class);
        assertThat(cachedResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cachedResp.getBody().getName()).isEqualTo("Cloud Engineer");

        // 9. Create version 2 and publish it
        com.devsphere.user.dto.ResumeVersionResponse version2 = resumeVersionService.createVersion(profile.getId(), userId, new com.devsphere.user.dto.CreateResumeVersionRequest("Version 2"));
        resumeVersionService.publishVersion(profile.getId(), version2.getId(), userId);

        // 10. Verify public endpoint now resolves new published snapshot ("Architect")
        ResponseEntity<PublicResumeResponse> updatedPublicResp = restTemplate.getForEntity("/api/v1/public/resumes/" + profile.getPublicId(), PublicResumeResponse.class);
        assertThat(updatedPublicResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updatedPublicResp.getBody().getName()).isEqualTo("Architect");
    }

    @Test
    void getPublicResume_WhenArchivedOnlyVersion_Returns404() {
        // 1. Create profile & version 1
        ResumeProfile profile = new ResumeProfile(userId, "Cloud Engineer", "DevOps Specialist", ResumeTemplate.MODERN);
        profile = resumeProfileRepository.save(profile);
        com.devsphere.user.dto.ResumeVersionResponse v1 = resumeVersionService.createVersion(profile.getId(), userId, new com.devsphere.user.dto.CreateResumeVersionRequest("V1"));

        // 2. Publish version 1 and enable sharing
        resumeVersionService.publishVersion(profile.getId(), v1.getId(), userId);
        publicResumeService.enablePublicSharing(profile.getId(), userId);

        // 3. Archive version 1 (leaving no PUBLISHED version)
        resumeVersionService.archiveVersion(profile.getId(), v1.getId(), userId);

        // 4. Verify public endpoint returns 404
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/public/resumes/" + profile.getPublicId(), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("PUBLIC_RESUME_NOT_FOUND");
    }

    @Test
    void getPublicResume_WhenPublicIdUnknown_Returns404() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/public/resumes/non-existent-uuid", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("PUBLIC_RESUME_NOT_FOUND");
    }

    @Test
    void securityBoundary_PublicEndpointAllowsAccessWithoutJwt_PrivateEndpointRequiresJwt() {
        // 1. Create and publish profile
        ResumeProfile profile = new ResumeProfile(userId, "Security Engineer", "AppSec Lead", ResumeTemplate.PROFESSIONAL);
        profile = resumeProfileRepository.save(profile);
        com.devsphere.user.dto.ResumeVersionResponse v1 = resumeVersionService.createVersion(profile.getId(), userId, new com.devsphere.user.dto.CreateResumeVersionRequest("V1"));
        resumeVersionService.publishVersion(profile.getId(), v1.getId(), userId);
        publicResumeService.enablePublicSharing(profile.getId(), userId);

        // 2. Access public endpoint WITHOUT JWT -> HTTP 200 OK
        ResponseEntity<PublicResumeResponse> publicResp = restTemplate.getForEntity("/api/v1/public/resumes/" + profile.getPublicId(), PublicResumeResponse.class);
        assertThat(publicResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(publicResp.getBody()).isNotNull();

        // 3. Access private endpoint WITHOUT JWT -> HTTP 401 Unauthorized
        ResponseEntity<String> privateResp = restTemplate.getForEntity("/api/v1/resumes/" + profile.getId(), String.class);
        assertThat(privateResp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getPublicResume_JsonOutputContainsOnlyPresentationFieldsAndNoInternalIds() {
        ResumeProfile profile = new ResumeProfile(userId, "Cloud Engineer", "DevOps Specialist", ResumeTemplate.MODERN);
        profile = resumeProfileRepository.save(profile);
        com.devsphere.user.dto.ResumeVersionResponse v1 = resumeVersionService.createVersion(profile.getId(), userId, new com.devsphere.user.dto.CreateResumeVersionRequest("V1"));
        resumeVersionService.publishVersion(profile.getId(), v1.getId(), userId);
        publicResumeService.enablePublicSharing(profile.getId(), userId);

        ResponseEntity<String> rawJsonResp = restTemplate.getForEntity("/api/v1/public/resumes/" + profile.getPublicId(), String.class);
        assertThat(rawJsonResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        String json = rawJsonResp.getBody();
        assertThat(json).isNotNull();
        assertThat(json).contains("\"name\":\"Cloud Engineer\"");
        assertThat(json).contains("\"targetRole\":\"DevOps Specialist\"");
        assertThat(json).contains("\"template\":\"MODERN\"");

        // Verify absence of sensitive/internal fields
        assertThat(json).doesNotContain("\"id\":");
        assertThat(json).doesNotContain("\"userId\":");
        assertThat(json).doesNotContain("\"resumeProfileId\":");
        assertThat(json).doesNotContain("\"versionId\":");
        assertThat(json).doesNotContain("\"publishedAt\":");
        assertThat(json).doesNotContain("\"createdAt\":");
        assertThat(json).doesNotContain("\"updatedAt\":");
    }

    @Test
    void publicSharingLifecycle_FullFlow() {
        // 1. Create profile
        ResumeProfile profile = new ResumeProfile(userId, "Staff Engineer", "Platform Lead", ResumeTemplate.MINIMAL);
        profile = resumeProfileRepository.save(profile);

        // 2. Attempt enable sharing before publishing version -> throws IllegalArgumentException
        final Long profileId = profile.getId();
        assertThatThrownBy(() -> publicResumeService.enablePublicSharing(profileId, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot enable public sharing without an active published resume version");

        // 3. Create and publish version 1
        com.devsphere.user.dto.ResumeVersionResponse v1 = resumeVersionService.createVersion(profile.getId(), userId, new com.devsphere.user.dto.CreateResumeVersionRequest("V1"));
        resumeVersionService.publishVersion(profile.getId(), v1.getId(), userId);

        // 4. Access public endpoint before enabling public sharing -> HTTP 404
        ResponseEntity<String> disabledResp = restTemplate.getForEntity("/api/v1/public/resumes/" + profile.getPublicId(), String.class);
        assertThat(disabledResp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // 5. Enable public sharing
        PublicShareStatusResponse enabledStatus = publicResumeService.enablePublicSharing(profile.getId(), userId);
        assertThat(enabledStatus.isPublicEnabled()).isTrue();
        assertThat(enabledStatus.getPublicEnabledAt()).isNotNull();

        // 6. Access public endpoint after enabling sharing -> HTTP 200 OK
        ResponseEntity<PublicResumeResponse> enabledPublicResp = restTemplate.getForEntity("/api/v1/public/resumes/" + profile.getPublicId(), PublicResumeResponse.class);
        assertThat(enabledPublicResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(enabledPublicResp.getBody().getName()).isEqualTo("Staff Engineer");

        // 7. Rotate public token
        String oldPublicId = profile.getPublicId();
        PublicShareStatusResponse rotatedStatus = publicResumeService.rotatePublicToken(profile.getId(), userId);
        String newPublicId = rotatedStatus.getPublicResumeId();
        assertThat(newPublicId).isNotEqualTo(oldPublicId);

        // 8. Access old link -> HTTP 404
        ResponseEntity<String> oldLinkResp = restTemplate.getForEntity("/api/v1/public/resumes/" + oldPublicId, String.class);
        assertThat(oldLinkResp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // 9. Access new link -> HTTP 200 OK
        ResponseEntity<PublicResumeResponse> newLinkResp = restTemplate.getForEntity("/api/v1/public/resumes/" + newPublicId, PublicResumeResponse.class);
        assertThat(newLinkResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 10. Revoke public sharing
        PublicShareStatusResponse revokedStatus = publicResumeService.revokePublicSharing(profile.getId(), userId);
        assertThat(revokedStatus.isPublicEnabled()).isFalse();
        assertThat(revokedStatus.getPublicEnabledAt()).isNull();

        // 11. Access new link after revocation -> HTTP 404
        ResponseEntity<String> revokedResp = restTemplate.getForEntity("/api/v1/public/resumes/" + newPublicId, String.class);
        assertThat(revokedResp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
