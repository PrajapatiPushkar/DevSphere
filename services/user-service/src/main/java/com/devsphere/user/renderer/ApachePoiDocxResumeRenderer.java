package com.devsphere.user.renderer;

import com.devsphere.user.dto.compilation.CompiledCertificationResponse;
import com.devsphere.user.dto.compilation.CompiledEducationResponse;
import com.devsphere.user.dto.compilation.CompiledExperienceResponse;
import com.devsphere.user.dto.compilation.CompiledProjectResponse;
import com.devsphere.user.dto.compilation.CompiledResumeResponse;
import com.devsphere.user.dto.compilation.CompiledResumeSectionResponse;
import com.devsphere.user.dto.compilation.CompiledSkillItemResponse;
import com.devsphere.user.dto.compilation.CompiledSkillsResponse;
import com.devsphere.user.dto.compilation.CompiledSummaryResponse;
import com.devsphere.user.entity.ResumeTemplate;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.xwpf.usermodel.Borders;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHyperlinkRun;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.springframework.stereotype.Component;

@Component
public class ApachePoiDocxResumeRenderer implements DocxResumeRenderer {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

    @Override
    public byte[] render(CompiledResumeResponse compiledResume) {
        if (compiledResume == null) {
            throw new IllegalArgumentException("Compiled resume must not be null");
        }

        ResumeTemplate template = compiledResume.getTemplate() != null ? compiledResume.getTemplate() : ResumeTemplate.PROFESSIONAL;
        DocxTemplateStyle style = DocxTemplateStyle.forTemplate(template);

        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            configurePageLayout(document, style);
            renderHeader(document, compiledResume, style);

            if (compiledResume.getSections() != null) {
                for (CompiledResumeSectionResponse section : compiledResume.getSections()) {
                    if (Boolean.TRUE.equals(section.getVisible())) {
                        renderSection(document, section, style);
                    }
                }
            }

            document.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to render DOCX resume", e);
        }
    }

    private void configurePageLayout(XWPFDocument document, DocxTemplateStyle style) {
        CTSectPr sectPr = document.getDocument().getBody().isSetSectPr()
                ? document.getDocument().getBody().getSectPr()
                : document.getDocument().getBody().addNewSectPr();

        CTPageMar pageMar = sectPr.isSetPgMar() ? sectPr.getPgMar() : sectPr.addNewPgMar();
        pageMar.setTop(BigInteger.valueOf(style.getMarginTopTwips()));
        pageMar.setBottom(BigInteger.valueOf(style.getMarginBottomTwips()));
        pageMar.setLeft(BigInteger.valueOf(style.getMarginLeftTwips()));
        pageMar.setRight(BigInteger.valueOf(style.getMarginRightTwips()));

        CTPageSz pageSz = sectPr.isSetPgSz() ? sectPr.getPgSz() : sectPr.addNewPgSz();
        pageSz.setW(BigInteger.valueOf(11906)); // A4 Width in twips
        pageSz.setH(BigInteger.valueOf(16838)); // A4 Height in twips
    }

    private void renderHeader(XWPFDocument document, CompiledResumeResponse compiledResume, DocxTemplateStyle style) {
        XWPFParagraph headerParagraph = document.createParagraph();
        headerParagraph.setSpacingAfter(120);

        XWPFRun nameRun = headerParagraph.createRun();
        setTextWithBreaks(nameRun, compiledResume.getName());
        nameRun.setBold(true);
        nameRun.setFontSize(style.getTitleSizePt());
        nameRun.setFontFamily(style.getHeadingFont());
        nameRun.setColor(style.getPrimaryHexColor());

        if (compiledResume.getTargetRole() != null && !compiledResume.getTargetRole().isBlank()) {
            nameRun.addBreak();
            XWPFRun roleRun = headerParagraph.createRun();
            setTextWithBreaks(roleRun, compiledResume.getTargetRole());
            roleRun.setFontSize(style.getTargetRoleSizePt());
            roleRun.setFontFamily(style.getDocumentFont());
            roleRun.setColor(style.getSecondaryHexColor());
        }

        headerParagraph.setBorderBottom(Borders.SINGLE);
    }

    private void renderSection(XWPFDocument document, CompiledResumeSectionResponse section, DocxTemplateStyle style) {
        if (section.getSectionType() == null) {
            return;
        }

        switch (section.getSectionType()) {
            case SUMMARY -> renderSummarySection(document, section, style);
            case EXPERIENCE -> renderExperienceSection(document, section, style);
            case EDUCATION -> renderEducationSection(document, section, style);
            case SKILLS -> renderSkillsSection(document, section, style);
            case CERTIFICATIONS -> renderCertificationSection(document, section, style);
            case PROJECTS -> renderProjectSection(document, section, style);
        }
    }

    private void renderSectionTitle(XWPFDocument document, String title, DocxTemplateStyle style) {
        XWPFParagraph titleParagraph = document.createParagraph();
        titleParagraph.setSpacingBefore(200);
        titleParagraph.setSpacingAfter(80);

        XWPFRun run = titleParagraph.createRun();
        run.setText(title.toUpperCase(Locale.ENGLISH));
        run.setBold(true);
        run.setFontSize(style.getSectionHeadingSizePt());
        run.setFontFamily(style.getHeadingFont());
        run.setColor(style.getPrimaryHexColor());

        titleParagraph.setBorderBottom(Borders.SINGLE);
    }

    private void renderSummarySection(XWPFDocument document, CompiledResumeSectionResponse section, DocxTemplateStyle style) {
        if (!(section.getContent() instanceof CompiledSummaryResponse summary)) {
            return;
        }
        if (summary.getText() == null || summary.getText().isBlank()) {
            return;
        }

        renderSectionTitle(document, "Summary", style);

        XWPFParagraph p = document.createParagraph();
        p.setSpacingAfter(120);

        XWPFRun run = p.createRun();
        setTextWithBreaks(run, summary.getText());
        run.setFontSize(style.getBodySizePt());
        run.setFontFamily(style.getDocumentFont());
    }

    private void renderExperienceSection(XWPFDocument document, CompiledResumeSectionResponse section, DocxTemplateStyle style) {
        List<CompiledExperienceResponse> items = extractItems(section.getContent());
        renderSectionTitle(document, "Experience", style);

        for (CompiledExperienceResponse exp : items) {
            XWPFParagraph headerP = document.createParagraph();
            headerP.setSpacingBefore(120);
            headerP.setSpacingAfter(20);

            XWPFRun titleRun = headerP.createRun();
            setTextWithBreaks(titleRun, exp.getJobTitle());
            titleRun.setBold(true);
            titleRun.setFontSize(style.getItemHeaderSizePt());
            titleRun.setFontFamily(style.getHeadingFont());
            titleRun.setColor(style.getPrimaryHexColor());

            if (exp.getCompanyName() != null && !exp.getCompanyName().isBlank()) {
                XWPFRun companyRun = headerP.createRun();
                companyRun.setText(" — " + exp.getCompanyName());
                companyRun.setBold(true);
                companyRun.setFontSize(style.getBodySizePt());
                companyRun.setFontFamily(style.getDocumentFont());
                companyRun.setColor(style.getSecondaryHexColor());
            }

            XWPFParagraph metaP = document.createParagraph();
            metaP.setSpacingAfter(40);

            StringBuilder metaText = new StringBuilder();
            if (exp.getLocation() != null && !exp.getLocation().isBlank()) {
                metaText.append(exp.getLocation()).append(" | ");
            }
            if (exp.getEmploymentType() != null) {
                metaText.append(exp.getEmploymentType().name()).append(" | ");
            }
            metaText.append(formatDate(exp.getStartDate()))
                    .append(" – ")
                    .append(Boolean.TRUE.equals(exp.getCurrentlyWorking()) || exp.getEndDate() == null ? "Present" : formatDate(exp.getEndDate()));

            XWPFRun metaRun = metaP.createRun();
            metaRun.setText(metaText.toString());
            metaRun.setItalic(true);
            metaRun.setFontSize(style.getBodySizePt());
            metaRun.setFontFamily(style.getDocumentFont());
            metaRun.setColor(style.getSecondaryHexColor());

            if (exp.getDescription() != null && !exp.getDescription().isBlank()) {
                XWPFParagraph descP = document.createParagraph();
                descP.setSpacingAfter(100);

                XWPFRun descRun = descP.createRun();
                setTextWithBreaks(descRun, exp.getDescription());
                descRun.setFontSize(style.getBodySizePt());
                descRun.setFontFamily(style.getDocumentFont());
            }
        }
    }

    private void renderEducationSection(XWPFDocument document, CompiledResumeSectionResponse section, DocxTemplateStyle style) {
        List<CompiledEducationResponse> items = extractItems(section.getContent());
        renderSectionTitle(document, "Education", style);

        for (CompiledEducationResponse edu : items) {
            XWPFParagraph headerP = document.createParagraph();
            headerP.setSpacingBefore(120);
            headerP.setSpacingAfter(20);

            XWPFRun degreeRun = headerP.createRun();
            setTextWithBreaks(degreeRun, edu.getDegree());
            degreeRun.setBold(true);
            degreeRun.setFontSize(style.getItemHeaderSizePt());
            degreeRun.setFontFamily(style.getHeadingFont());
            degreeRun.setColor(style.getPrimaryHexColor());

            if (edu.getInstitutionName() != null && !edu.getInstitutionName().isBlank()) {
                XWPFRun instRun = headerP.createRun();
                instRun.setText(" — " + edu.getInstitutionName());
                instRun.setBold(true);
                instRun.setFontSize(style.getBodySizePt());
                instRun.setFontFamily(style.getDocumentFont());
                instRun.setColor(style.getSecondaryHexColor());
            }

            XWPFParagraph metaP = document.createParagraph();
            metaP.setSpacingAfter(40);

            StringBuilder metaText = new StringBuilder();
            if (edu.getFieldOfStudy() != null && !edu.getFieldOfStudy().isBlank()) {
                metaText.append(edu.getFieldOfStudy()).append(" | ");
            }
            if (edu.getLocation() != null && !edu.getLocation().isBlank()) {
                metaText.append(edu.getLocation()).append(" | ");
            }
            metaText.append(formatDate(edu.getStartDate()))
                    .append(" – ")
                    .append(Boolean.TRUE.equals(edu.getCurrentlyStudying()) || edu.getEndDate() == null ? "Present" : formatDate(edu.getEndDate()));

            XWPFRun metaRun = metaP.createRun();
            metaRun.setText(metaText.toString());
            metaRun.setItalic(true);
            metaRun.setFontSize(style.getBodySizePt());
            metaRun.setFontFamily(style.getDocumentFont());
            metaRun.setColor(style.getSecondaryHexColor());

            if (edu.getDescription() != null && !edu.getDescription().isBlank()) {
                XWPFParagraph descP = document.createParagraph();
                descP.setSpacingAfter(100);

                XWPFRun descRun = descP.createRun();
                setTextWithBreaks(descRun, edu.getDescription());
                descRun.setFontSize(style.getBodySizePt());
                descRun.setFontFamily(style.getDocumentFont());
            }
        }
    }

    private void renderSkillsSection(XWPFDocument document, CompiledResumeSectionResponse section, DocxTemplateStyle style) {
        List<CompiledSkillItemResponse> items = List.of();
        if (section.getContent() instanceof CompiledSkillsResponse skillsResp) {
            items = skillsResp.getItems() != null ? skillsResp.getItems() : List.of();
        }

        renderSectionTitle(document, "Skills", style);

        if (!items.isEmpty()) {
            XWPFParagraph p = document.createParagraph();
            p.setSpacingAfter(120);

            for (int i = 0; i < items.size(); i++) {
                CompiledSkillItemResponse skill = items.get(i);
                XWPFRun nameRun = p.createRun();
                nameRun.setText(skill.getName());
                nameRun.setBold(true);
                nameRun.setFontSize(style.getBodySizePt());
                nameRun.setFontFamily(style.getDocumentFont());

                if (skill.getProficiency() != null) {
                    XWPFRun profRun = p.createRun();
                    profRun.setText(" (" + skill.getProficiency().name() + ")");
                    profRun.setFontSize(style.getBodySizePt());
                    profRun.setFontFamily(style.getDocumentFont());
                    profRun.setColor(style.getSecondaryHexColor());
                }

                if (i < items.size() - 1) {
                    XWPFRun sepRun = p.createRun();
                    sepRun.setText("  •  ");
                    sepRun.setFontSize(style.getBodySizePt());
                    sepRun.setFontFamily(style.getDocumentFont());
                }
            }
        }
    }

    private void renderCertificationSection(XWPFDocument document, CompiledResumeSectionResponse section, DocxTemplateStyle style) {
        List<CompiledCertificationResponse> items = extractItems(section.getContent());
        renderSectionTitle(document, "Certifications", style);

        for (CompiledCertificationResponse cert : items) {
            XWPFParagraph headerP = document.createParagraph();
            headerP.setSpacingBefore(120);
            headerP.setSpacingAfter(20);

            XWPFRun nameRun = headerP.createRun();
            setTextWithBreaks(nameRun, cert.getName());
            nameRun.setBold(true);
            nameRun.setFontSize(style.getItemHeaderSizePt());
            nameRun.setFontFamily(style.getHeadingFont());
            nameRun.setColor(style.getPrimaryHexColor());

            if (cert.getIssuingOrganization() != null && !cert.getIssuingOrganization().isBlank()) {
                XWPFRun orgRun = headerP.createRun();
                orgRun.setText(" — " + cert.getIssuingOrganization());
                orgRun.setFontSize(style.getBodySizePt());
                orgRun.setFontFamily(style.getDocumentFont());
                orgRun.setColor(style.getSecondaryHexColor());
            }

            XWPFParagraph metaP = document.createParagraph();
            metaP.setSpacingAfter(40);

            StringBuilder metaText = new StringBuilder();
            if (cert.getIssueDate() != null) {
                metaText.append("Issued: ").append(formatDate(cert.getIssueDate()));
            }
            if (cert.getExpirationDate() != null) {
                if (!metaText.isEmpty()) {
                    metaText.append(" | ");
                }
                metaText.append("Expires: ").append(formatDate(cert.getExpirationDate()));
            }

            if (!metaText.isEmpty()) {
                XWPFRun metaRun = metaP.createRun();
                metaRun.setText(metaText.toString());
                metaRun.setItalic(true);
                metaRun.setFontSize(style.getBodySizePt());
                metaRun.setFontFamily(style.getDocumentFont());
                metaRun.setColor(style.getSecondaryHexColor());
            }

            if (cert.getCredentialUrl() != null && isSafeUrl(cert.getCredentialUrl())) {
                XWPFParagraph linkP = document.createParagraph();
                linkP.setSpacingAfter(40);
                addHyperlink(linkP, cert.getCredentialUrl(), "Verify Credential", style);
            }

            if (cert.getDescription() != null && !cert.getDescription().isBlank()) {
                XWPFParagraph descP = document.createParagraph();
                descP.setSpacingAfter(100);

                XWPFRun descRun = descP.createRun();
                setTextWithBreaks(descRun, cert.getDescription());
                descRun.setFontSize(style.getBodySizePt());
                descRun.setFontFamily(style.getDocumentFont());
            }
        }
    }

    private void renderProjectSection(XWPFDocument document, CompiledResumeSectionResponse section, DocxTemplateStyle style) {
        List<CompiledProjectResponse> items = extractItems(section.getContent());
        renderSectionTitle(document, "Projects", style);

        for (CompiledProjectResponse proj : items) {
            XWPFParagraph headerP = document.createParagraph();
            headerP.setSpacingBefore(120);
            headerP.setSpacingAfter(20);

            XWPFRun nameRun = headerP.createRun();
            setTextWithBreaks(nameRun, proj.getName());
            nameRun.setBold(true);
            nameRun.setFontSize(style.getItemHeaderSizePt());
            nameRun.setFontFamily(style.getHeadingFont());
            nameRun.setColor(style.getPrimaryHexColor());

            if (proj.getProjectType() != null) {
                XWPFRun typeRun = headerP.createRun();
                typeRun.setText(" (" + proj.getProjectType().name() + ")");
                typeRun.setFontSize(style.getBodySizePt());
                typeRun.setFontFamily(style.getDocumentFont());
                typeRun.setColor(style.getSecondaryHexColor());
            }

            if (proj.getDescription() != null && !proj.getDescription().isBlank()) {
                XWPFParagraph descP = document.createParagraph();
                descP.setSpacingAfter(40);

                XWPFRun descRun = descP.createRun();
                setTextWithBreaks(descRun, proj.getDescription());
                descRun.setFontSize(style.getBodySizePt());
                descRun.setFontFamily(style.getDocumentFont());
            }

            if (proj.getTechStack() != null && !proj.getTechStack().isEmpty()) {
                XWPFParagraph techP = document.createParagraph();
                techP.setSpacingAfter(40);

                XWPFRun labelRun = techP.createRun();
                labelRun.setText("Technologies: ");
                labelRun.setBold(true);
                labelRun.setFontSize(style.getBodySizePt());
                labelRun.setFontFamily(style.getDocumentFont());

                XWPFRun valRun = techP.createRun();
                valRun.setText(String.join(", ", proj.getTechStack()));
                valRun.setFontSize(style.getBodySizePt());
                valRun.setFontFamily(style.getDocumentFont());
            }

            boolean hasRepo = proj.getRepositoryUrl() != null && isSafeUrl(proj.getRepositoryUrl());
            boolean hasLive = proj.getLiveUrl() != null && isSafeUrl(proj.getLiveUrl());
            boolean hasDocs = proj.getDocumentationUrl() != null && isSafeUrl(proj.getDocumentationUrl());

            if (hasRepo || hasLive || hasDocs) {
                XWPFParagraph linksP = document.createParagraph();
                linksP.setSpacingAfter(100);

                if (hasRepo) {
                    addHyperlink(linksP, proj.getRepositoryUrl(), "Repository", style);
                    if (hasLive || hasDocs) {
                        addSpacingText(linksP, "  |  ", style);
                    }
                }
                if (hasLive) {
                    addHyperlink(linksP, proj.getLiveUrl(), "Live Demo", style);
                    if (hasDocs) {
                        addSpacingText(linksP, "  |  ", style);
                    }
                }
                if (hasDocs) {
                    addHyperlink(linksP, proj.getDocumentationUrl(), "Docs", style);
                }
            }
        }
    }

    private void addHyperlink(XWPFParagraph paragraph, String url, String linkText, DocxTemplateStyle style) {
        XWPFHyperlinkRun hyperlinkRun = paragraph.createHyperlinkRun(url);
        hyperlinkRun.setText(linkText);
        hyperlinkRun.setColor("2563EB");
        hyperlinkRun.setUnderline(UnderlinePatterns.SINGLE);
        hyperlinkRun.setFontSize(style.getBodySizePt());
        hyperlinkRun.setFontFamily(style.getDocumentFont());
    }

    private void addSpacingText(XWPFParagraph paragraph, String text, DocxTemplateStyle style) {
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setFontSize(style.getBodySizePt());
        run.setFontFamily(style.getDocumentFont());
        run.setColor(style.getSecondaryHexColor());
    }

    private void setTextWithBreaks(XWPFRun run, String text) {
        if (text == null) {
            return;
        }
        String[] lines = text.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            run.setText(lines[i]);
            if (i < lines.length - 1) {
                run.addBreak();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> extractItems(Object content) {
        if (content instanceof List<?> list) {
            return (List<T>) list;
        }
        if (content instanceof Map<?, ?> map && map.containsKey("items") && map.get("items") instanceof List<?> list) {
            return (List<T>) list;
        }
        return List.of();
    }

    private String formatDate(LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.format(DATE_FORMATTER);
    }

    private boolean isSafeUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String lower = url.trim().toLowerCase(Locale.ENGLISH);
        return lower.startsWith("http://") || lower.startsWith("https://");
    }
}
