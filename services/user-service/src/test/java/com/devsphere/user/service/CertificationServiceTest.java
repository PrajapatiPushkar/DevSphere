package com.devsphere.user.service;

import com.devsphere.user.dto.CertificationRequest;
import com.devsphere.user.dto.CertificationResponse;
import com.devsphere.user.entity.Certification;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.repository.CertificationRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificationServiceTest {

    @Mock
    private CertificationRepository certificationRepository;

    private SimpleMeterRegistry meterRegistry;
    private CertificationService certificationService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        certificationService = new CertificationService(certificationRepository, meterRegistry);
    }

    @Test
    void createCertification_validRequest_createsAndIncrementsCounter() {
        Long userId = 100L;
        CertificationRequest request = new CertificationRequest(
                "AWS Certified Solutions Architect", "Amazon Web Services",
                LocalDate.of(2023, 1, 15), LocalDate.of(2026, 1, 15),
                "AWS-12345", "https://aws.amazon.com/verify/12345", "Architect Cert", 1
        );

        when(certificationRepository.save(any(Certification.class))).thenAnswer(inv -> {
            Certification cert = inv.getArgument(0);
            cert.setId(40L);
            return cert;
        });

        CertificationResponse response = certificationService.createCertification(userId, request);

        assertThat(response.getId()).isEqualTo(40L);
        assertThat(response.getName()).isEqualTo("AWS Certified Solutions Architect");
        assertThat(meterRegistry.find("devsphere_certification_created_total").counter().count()).isEqualTo(1.0);
    }

    @Test
    void createCertification_expirationBeforeIssue_throwsIllegalArgumentException() {
        Long userId = 100L;
        CertificationRequest request = new CertificationRequest(
                "Invalid Cert", "Org",
                LocalDate.of(2023, 1, 15), LocalDate.of(2020, 1, 15),
                null, null, "Invalid dates", 0
        );

        assertThatThrownBy(() -> certificationService.createCertification(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expirationDate must not be before issueDate");
    }

    @Test
    void deleteCertification_found_deletesRecord() {
        Long id = 40L;
        Long userId = 100L;
        Certification cert = new Certification(userId, "CKA", "CNCF");
        cert.setId(id);

        when(certificationRepository.findByIdAndUserId(id, userId)).thenReturn(Optional.of(cert));

        certificationService.deleteCertification(id, userId);

        verify(certificationRepository).delete(cert);
        assertThat(meterRegistry.find("devsphere_certification_deleted_total").counter().count()).isEqualTo(1.0);
    }
}
