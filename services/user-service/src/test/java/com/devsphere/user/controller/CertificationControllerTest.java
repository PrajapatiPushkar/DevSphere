package com.devsphere.user.controller;

import com.devsphere.user.dto.CertificationRequest;
import com.devsphere.user.dto.CertificationResponse;
import com.devsphere.user.entity.Certification;
import com.devsphere.user.exception.GlobalExceptionHandler;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.service.CertificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = CertificationController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@Import(GlobalExceptionHandler.class)
class CertificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CertificationService certificationService;

    @MockBean(name = "userSecurity")
    private com.devsphere.user.security.UserSecurity userSecurity;

    @MockBean
    private com.devsphere.user.security.JwtValidator jwtValidator;

    @Test
    void createCertification_returns201Created() throws Exception {
        Long userId = 100L;
        CertificationRequest request = new CertificationRequest(
                "AWS Solutions Architect", "Amazon Web Services",
                LocalDate.of(2023, 1, 1), LocalDate.of(2026, 1, 1),
                "AWS-123", "https://aws.amazon.com/verify/123", "AWS Cert", 1
        );

        Certification cert = new Certification(userId, "AWS Solutions Architect", "Amazon Web Services");
        cert.setId(40L);

        when(certificationService.createCertification(eq(userId), any(CertificationRequest.class)))
                .thenReturn(new CertificationResponse(cert));

        mockMvc.perform(post("/api/v1/certifications")
                        .header("X-Authenticated-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(40L))
                .andExpect(jsonPath("$.name").value("AWS Solutions Architect"));
    }

    @Test
    void getCertification_notFound_returns404() throws Exception {
        Long userId = 100L;
        Long id = 99L;

        when(certificationService.getCertification(id, userId))
                .thenThrow(new ResourceNotFoundException("CERTIFICATION_NOT_FOUND", "Certification record not found"));

        mockMvc.perform(get("/api/v1/certifications/{id}", id)
                        .header("X-Authenticated-User-Id", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CERTIFICATION_NOT_FOUND"));
    }

    @Test
    void deleteCertification_returns204NoContent() throws Exception {
        Long userId = 100L;
        Long id = 40L;

        doNothing().when(certificationService).deleteCertification(id, userId);

        mockMvc.perform(delete("/api/v1/certifications/{id}", id)
                        .header("X-Authenticated-User-Id", userId))
                .andExpect(status().isNoContent());

        verify(certificationService).deleteCertification(id, userId);
    }
}
