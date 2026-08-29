package com.devsphere.user.controller;

import com.devsphere.user.dto.DocxExportResult;
import com.devsphere.user.dto.PdfExportResult;
import com.devsphere.user.dto.ResumeExportFormat;
import com.devsphere.user.dto.ResumeExportResult;
import com.devsphere.user.exception.UnauthorizedException;
import com.devsphere.user.security.UserPrincipal;
import com.devsphere.user.service.ResumeDocxRenderingService;
import com.devsphere.user.service.ResumeExportService;
import com.devsphere.user.service.ResumePdfRenderingService;
import com.devsphere.user.service.ResumeRenderingService;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/resumes")
public class ResumeRenderingController {

    private static final String AUTH_USER_ID_HEADER = "X-Authenticated-User-Id";
    private static final String DOCX_MEDIA_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private final ResumeExportService resumeExportService;
    private final ResumeRenderingService resumeRenderingService;
    private final ResumePdfRenderingService resumePdfRenderingService;
    private final ResumeDocxRenderingService resumeDocxRenderingService;

    @Autowired
    public ResumeRenderingController(ResumeExportService resumeExportService,
                                       ResumeRenderingService resumeRenderingService,
                                       ResumePdfRenderingService resumePdfRenderingService,
                                       ResumeDocxRenderingService resumeDocxRenderingService) {
        this.resumeExportService = resumeExportService;
        this.resumeRenderingService = resumeRenderingService;
        this.resumePdfRenderingService = resumePdfRenderingService;
        this.resumeDocxRenderingService = resumeDocxRenderingService;
    }

    public ResumeRenderingController(ResumeExportService resumeExportService) {
        this(resumeExportService, null, null, null);
    }

    public ResumeRenderingController(ResumeRenderingService resumeRenderingService,
                                       ResumePdfRenderingService resumePdfRenderingService,
                                       ResumeDocxRenderingService resumeDocxRenderingService) {
        this(null, resumeRenderingService, resumePdfRenderingService, resumeDocxRenderingService);
    }

    @GetMapping(value = "/{resumeId}/render/html", produces = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8")
    public ResponseEntity<String> renderHtml(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable Long resumeId) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        if (resumeExportService != null) {
            ResumeExportResult result = resumeExportService.exportResume(resumeId, userId, ResumeExportFormat.HTML);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(result.getContentType()));
            return ResponseEntity.ok().headers(headers).body(new String(result.getContent(), StandardCharsets.UTF_8));
        }

        String html = resumeRenderingService.renderHtmlResume(resumeId, userId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/html;charset=UTF-8"));
        return ResponseEntity.ok().headers(headers).body(html);
    }

    @GetMapping(value = "/{resumeId}/render/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> renderPdf(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable Long resumeId) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        if (resumeExportService != null) {
            ResumeExportResult result = resumeExportService.exportResume(resumeId, userId, ResumeExportFormat.PDF);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(result.getContentType()));
            headers.setContentDisposition(ContentDisposition.attachment().filename(result.getFilename()).build());
            return ResponseEntity.ok().headers(headers).body(result.getContent());
        }

        PdfExportResult pdfExportResult = resumePdfRenderingService.exportPdfResume(resumeId, userId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename(pdfExportResult.getFilename()).build());
        return ResponseEntity.ok().headers(headers).body(pdfExportResult.getPdfBytes());
    }

    @GetMapping(value = "/{resumeId}/render/docx", produces = DOCX_MEDIA_TYPE)
    public ResponseEntity<byte[]> renderDocx(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable Long resumeId) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        if (resumeExportService != null) {
            ResumeExportResult result = resumeExportService.exportResume(resumeId, userId, ResumeExportFormat.DOCX);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(result.getContentType()));
            headers.setContentDisposition(ContentDisposition.attachment().filename(result.getFilename()).build());
            return ResponseEntity.ok().headers(headers).body(result.getContent());
        }

        DocxExportResult docxExportResult = resumeDocxRenderingService.exportDocxResume(resumeId, userId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(DOCX_MEDIA_TYPE));
        headers.setContentDisposition(ContentDisposition.attachment().filename(docxExportResult.getFilename()).build());
        return ResponseEntity.ok().headers(headers).body(docxExportResult.getDocxBytes());
    }

    @GetMapping(value = "/{resumeId}/versions/{versionId}/render/html", produces = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8")
    public ResponseEntity<String> renderVersionHtml(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable Long resumeId,
            @PathVariable Long versionId) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        ResumeExportResult result = resumeExportService.exportResumeVersion(resumeId, versionId, userId, ResumeExportFormat.HTML);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(result.getContentType()));
        return ResponseEntity.ok().headers(headers).body(new String(result.getContent(), StandardCharsets.UTF_8));
    }

    @GetMapping(value = "/{resumeId}/versions/{versionId}/render/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> renderVersionPdf(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable Long resumeId,
            @PathVariable Long versionId) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        ResumeExportResult result = resumeExportService.exportResumeVersion(resumeId, versionId, userId, ResumeExportFormat.PDF);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(result.getContentType()));
        headers.setContentDisposition(ContentDisposition.attachment().filename(result.getFilename()).build());
        return ResponseEntity.ok().headers(headers).body(result.getContent());
    }

    @GetMapping(value = "/{resumeId}/versions/{versionId}/render/docx", produces = DOCX_MEDIA_TYPE)
    public ResponseEntity<byte[]> renderVersionDocx(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable Long resumeId,
            @PathVariable Long versionId) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        ResumeExportResult result = resumeExportService.exportResumeVersion(resumeId, versionId, userId, ResumeExportFormat.DOCX);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(result.getContentType()));
        headers.setContentDisposition(ContentDisposition.attachment().filename(result.getFilename()).build());
        return ResponseEntity.ok().headers(headers).body(result.getContent());
    }

    private Long extractAndValidateUserId(String authUserIdHeader) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            return userPrincipal.getUserId();
        }

        if (authUserIdHeader != null && !authUserIdHeader.isBlank()) {
            try {
                return Long.parseLong(authUserIdHeader.trim());
            } catch (NumberFormatException e) {
                throw new UnauthorizedException("Invalid authenticated user identity format");
            }
        }

        throw new UnauthorizedException("Authenticated user identity is required");
    }
}
