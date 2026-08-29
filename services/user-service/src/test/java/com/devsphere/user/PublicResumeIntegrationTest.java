package com.devsphere.user;

import com.devsphere.user.dto.publicresume.PublicResumeResponse;
import com.devsphere.user.entity.ResumeProfile;
import com.devsphere.user.entity.ResumeTemplate;
import com.devsphere.user.entity.ResumeVersion;
import com.devsphere.user.entity.ResumeVersionStatus;
import com.devsphere.user.repository.ResumeProfileRepository;
import com.devsphere.user.repository.ResumeVersionRepository;
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

        // 5. Access public endpoint WITHOUT authentication
        ResponseEntity<PublicResumeResponse> publicResp = restTemplate.getForEntity("/api/v1/public/resumes/" + profile.getPublicId(), PublicResumeResponse.class);
        assertThat(publicResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(publicResp.getBody()).isNotNull();
        assertThat(publicResp.getBody().getName()).isEqualTo("Cloud Engineer");
        assertThat(publicResp.getBody().getTargetRole()).isEqualTo("DevOps Specialist");
        assertThat(publicResp.getBody().getTemplate()).isEqualTo(ResumeTemplate.MODERN);

        // 6. Mutate live profile data (e.g. change name to "Architect")
        profile.setName("Architect");
        resumeProfileRepository.save(profile);

        // 7. Verify public endpoint still returns published snapshot ("Cloud Engineer"), preserving immutability!
        ResponseEntity<PublicResumeResponse> cachedResp = restTemplate.getForEntity("/api/v1/public/resumes/" + profile.getPublicId(), PublicResumeResponse.class);
        assertThat(cachedResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cachedResp.getBody().getName()).isEqualTo("Cloud Engineer");

        // 8. Create version 2 and publish it
        com.devsphere.user.dto.ResumeVersionResponse version2 = resumeVersionService.createVersion(profile.getId(), userId, new com.devsphere.user.dto.CreateResumeVersionRequest("Version 2"));
        resumeVersionService.publishVersion(profile.getId(), version2.getId(), userId);

        // 9. Verify public endpoint now resolves new published snapshot ("Architect")
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
        
        // 2. Publish version 1
        resumeVersionService.publishVersion(profile.getId(), v1.getId(), userId);

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
}

