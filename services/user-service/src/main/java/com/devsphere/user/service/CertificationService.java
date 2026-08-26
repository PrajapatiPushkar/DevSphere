package com.devsphere.user.service;

import com.devsphere.user.dto.CertificationRequest;
import com.devsphere.user.dto.CertificationResponse;
import com.devsphere.user.entity.Certification;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.repository.CertificationRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CertificationService {

    private static final Logger log = LoggerFactory.getLogger(CertificationService.class);

    private final CertificationRepository certificationRepository;
    private final MeterRegistry meterRegistry;

    public CertificationService(CertificationRepository certificationRepository) {
        this(certificationRepository, new SimpleMeterRegistry());
    }

    @Autowired
    public CertificationService(CertificationRepository certificationRepository, MeterRegistry meterRegistry) {
        this.certificationRepository = certificationRepository;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public CertificationResponse createCertification(Long userId, CertificationRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }

        validateCertificationDates(request);

        Certification cert = new Certification(
                userId,
                request.getName().trim(),
                request.getIssuingOrganization().trim()
        );
        cert.setIssueDate(request.getIssueDate());
        cert.setExpirationDate(request.getExpirationDate());
        cert.setCredentialId(trimToNull(request.getCredentialId()));
        cert.setCredentialUrl(trimToNull(request.getCredentialUrl()));
        cert.setDescription(trimToNull(request.getDescription()));
        cert.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);

        Certification saved = certificationRepository.save(cert);
        meterRegistry.counter("devsphere_certification_created_total").increment();
        log.info("Created certification ID: {} for userId: {}", saved.getId(), userId);

        return new CertificationResponse(saved);
    }

    @Transactional(readOnly = true)
    public CertificationResponse getCertification(Long id, Long userId) {
        Certification cert = certificationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("CERTIFICATION_NOT_FOUND", "Certification record not found"));
        return new CertificationResponse(cert);
    }

    @Transactional(readOnly = true)
    public List<CertificationResponse> listCertifications(Long userId) {
        return certificationRepository.findAllByUserIdOrderByDisplayOrderAscIssueDateDesc(userId)
                .stream()
                .map(CertificationResponse::new)
                .toList();
    }

    @Transactional
    public CertificationResponse updateCertification(Long id, Long userId, CertificationRequest request) {
        Certification cert = certificationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("CERTIFICATION_NOT_FOUND", "Certification record not found"));

        validateCertificationDates(request);

        cert.setName(request.getName().trim());
        cert.setIssuingOrganization(request.getIssuingOrganization().trim());
        cert.setIssueDate(request.getIssueDate());
        cert.setExpirationDate(request.getExpirationDate());
        cert.setCredentialId(trimToNull(request.getCredentialId()));
        cert.setCredentialUrl(trimToNull(request.getCredentialUrl()));
        cert.setDescription(trimToNull(request.getDescription()));
        if (request.getDisplayOrder() != null) {
            cert.setDisplayOrder(request.getDisplayOrder());
        }

        Certification updated = certificationRepository.save(cert);
        meterRegistry.counter("devsphere_certification_updated_total").increment();
        log.info("Updated certification ID: {} for userId: {}", updated.getId(), userId);

        return new CertificationResponse(updated);
    }

    @Transactional
    public void deleteCertification(Long id, Long userId) {
        Certification cert = certificationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("CERTIFICATION_NOT_FOUND", "Certification record not found"));

        certificationRepository.delete(cert);
        meterRegistry.counter("devsphere_certification_deleted_total").increment();
        log.info("Deleted certification ID: {} for userId: {}", id, userId);
    }

    private void validateCertificationDates(CertificationRequest request) {
        if (request.getIssueDate() != null && request.getExpirationDate() != null) {
            if (request.getExpirationDate().isBefore(request.getIssueDate())) {
                throw new IllegalArgumentException("expirationDate must not be before issueDate");
            }
        }
    }

    private String trimToNull(String val) {
        return (val == null || val.isBlank()) ? null : val.trim();
    }
}
