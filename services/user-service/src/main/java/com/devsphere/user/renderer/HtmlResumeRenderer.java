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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class HtmlResumeRenderer implements ResumeRenderer {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

    @Override
    public String render(CompiledResumeResponse compiledResume) {
        if (compiledResume == null) {
            throw new IllegalArgumentException("Compiled resume must not be null");
        }

        ResumeTemplate template = compiledResume.getTemplate() != null ? compiledResume.getTemplate() : ResumeTemplate.PROFESSIONAL;
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\" />\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />\n");
        html.append("    <title>").append(escapeHtml(compiledResume.getName())).append("</title>\n");
        html.append(generateStylesheet(template));
        html.append("</head>\n");
        html.append("<body class=\"template-").append(template.name().toLowerCase()).append("\">\n");
        html.append("    <div class=\"resume-container\">\n");

        // Header
        html.append("        <header class=\"resume-header\">\n");
        html.append("            <h1 class=\"developer-name\">").append(escapeHtml(compiledResume.getName())).append("</h1>\n");
        if (compiledResume.getTargetRole() != null && !compiledResume.getTargetRole().isBlank()) {
            html.append("            <p class=\"target-role\">").append(escapeHtml(compiledResume.getTargetRole())).append("</p>\n");
        }
        html.append("        </header>\n");

        // Main content sections
        html.append("        <main class=\"resume-content\">\n");
        if (compiledResume.getSections() != null) {
            for (CompiledResumeSectionResponse section : compiledResume.getSections()) {
                if (Boolean.TRUE.equals(section.getVisible())) {
                    renderSection(html, section);
                }
            }
        }
        html.append("        </main>\n");

        html.append("    </div>\n");
        html.append("</body>\n");
        html.append("</html>");

        return html.toString();
    }

    private void renderSection(StringBuilder html, CompiledResumeSectionResponse section) {
        if (section.getSectionType() == null) {
            return;
        }

        switch (section.getSectionType()) {
            case SUMMARY -> renderSummarySection(html, section);
            case EXPERIENCE -> renderExperienceSection(html, section);
            case EDUCATION -> renderEducationSection(html, section);
            case SKILLS -> renderSkillsSection(html, section);
            case CERTIFICATIONS -> renderCertificationSection(html, section);
            case PROJECTS -> renderProjectSection(html, section);
        }
    }

    private void renderSummarySection(StringBuilder html, CompiledResumeSectionResponse section) {
        if (!(section.getContent() instanceof CompiledSummaryResponse summary)) {
            return;
        }
        if (summary.getText() == null || summary.getText().isBlank()) {
            return;
        }

        html.append("            <section class=\"resume-section section-summary\">\n");
        html.append("                <h2 class=\"section-title\">Summary</h2>\n");
        html.append("                <p class=\"summary-text\">").append(escapeHtml(summary.getText())).append("</p>\n");
        html.append("            </section>\n");
    }

    @SuppressWarnings("unchecked")
    private void renderExperienceSection(StringBuilder html, CompiledResumeSectionResponse section) {
        List<CompiledExperienceResponse> items = extractItems(section.getContent());
        html.append("            <section class=\"resume-section section-experience\">\n");
        html.append("                <h2 class=\"section-title\">Experience</h2>\n");
        if (items.isEmpty()) {
            html.append("            </section>\n");
            return;
        }
        for (CompiledExperienceResponse exp : items) {
            html.append("                <article class=\"experience-item\">\n");
            html.append("                    <div class=\"item-header\">\n");
            html.append("                        <h3 class=\"job-title\">").append(escapeHtml(exp.getJobTitle())).append("</h3>\n");
            html.append("                        <span class=\"company-name\">").append(escapeHtml(exp.getCompanyName())).append("</span>\n");
            html.append("                    </div>\n");

            html.append("                    <div class=\"item-meta\">\n");
            if (exp.getLocation() != null && !exp.getLocation().isBlank()) {
                html.append("                        <span class=\"location\">").append(escapeHtml(exp.getLocation())).append("</span> | \n");
            }
            if (exp.getEmploymentType() != null) {
                html.append("                        <span class=\"employment-type\">").append(escapeHtml(exp.getEmploymentType().name())).append("</span> | \n");
            }
            html.append("                        <span class=\"date-range\">")
                    .append(formatDate(exp.getStartDate()))
                    .append(" &#8211; ")
                    .append(Boolean.TRUE.equals(exp.getCurrentlyWorking()) || exp.getEndDate() == null ? "Present" : formatDate(exp.getEndDate()))
                    .append("</span>\n");
            html.append("                    </div>\n");

            if (exp.getDescription() != null && !exp.getDescription().isBlank()) {
                html.append("                    <p class=\"item-description\">").append(escapeHtml(exp.getDescription())).append("</p>\n");
            }
            html.append("                </article>\n");
        }
        html.append("            </section>\n");
    }

    @SuppressWarnings("unchecked")
    private void renderEducationSection(StringBuilder html, CompiledResumeSectionResponse section) {
        List<CompiledEducationResponse> items = extractItems(section.getContent());
        html.append("            <section class=\"resume-section section-education\">\n");
        html.append("                <h2 class=\"section-title\">Education</h2>\n");
        if (items.isEmpty()) {
            html.append("            </section>\n");
            return;
        }
        for (CompiledEducationResponse edu : items) {
            html.append("                <article class=\"education-item\">\n");
            html.append("                    <div class=\"item-header\">\n");
            html.append("                        <h3 class=\"degree\">").append(escapeHtml(edu.getDegree())).append("</h3>\n");
            html.append("                        <span class=\"institution\">").append(escapeHtml(edu.getInstitutionName())).append("</span>\n");
            html.append("                    </div>\n");

            html.append("                    <div class=\"item-meta\">\n");
            if (edu.getFieldOfStudy() != null && !edu.getFieldOfStudy().isBlank()) {
                html.append("                        <span class=\"field-of-study\">").append(escapeHtml(edu.getFieldOfStudy())).append("</span> | \n");
            }
            if (edu.getLocation() != null && !edu.getLocation().isBlank()) {
                html.append("                        <span class=\"location\">").append(escapeHtml(edu.getLocation())).append("</span> | \n");
            }
            html.append("                        <span class=\"date-range\">")
                    .append(formatDate(edu.getStartDate()))
                    .append(" &#8211; ")
                    .append(Boolean.TRUE.equals(edu.getCurrentlyStudying()) || edu.getEndDate() == null ? "Present" : formatDate(edu.getEndDate()))
                    .append("</span>\n");
            html.append("                    </div>\n");

            if (edu.getDescription() != null && !edu.getDescription().isBlank()) {
                html.append("                    <p class=\"item-description\">").append(escapeHtml(edu.getDescription())).append("</p>\n");
            }
            html.append("                </article>\n");
        }
        html.append("            </section>\n");
    }

    private void renderSkillsSection(StringBuilder html, CompiledResumeSectionResponse section) {
        List<CompiledSkillItemResponse> items = List.of();
        if (section.getContent() instanceof CompiledSkillsResponse skillsResp) {
            items = skillsResp.getItems() != null ? skillsResp.getItems() : List.of();
        }

        html.append("            <section class=\"resume-section section-skills\">\n");
        html.append("                <h2 class=\"section-title\">Skills</h2>\n");
        if (!items.isEmpty()) {
            html.append("                <ul class=\"skills-list\">\n");
            for (CompiledSkillItemResponse skill : items) {
                html.append("                    <li class=\"skill-item\">");
                html.append("<span class=\"skill-name\">").append(escapeHtml(skill.getName())).append("</span>");
                if (skill.getProficiency() != null) {
                    html.append(" <span class=\"skill-proficiency\">(").append(escapeHtml(skill.getProficiency().name())).append(")</span>");
                }
                html.append("</li>\n");
            }
            html.append("                </ul>\n");
        }
        html.append("            </section>\n");
    }

    @SuppressWarnings("unchecked")
    private void renderCertificationSection(StringBuilder html, CompiledResumeSectionResponse section) {
        List<CompiledCertificationResponse> items = extractItems(section.getContent());
        html.append("            <section class=\"resume-section section-certifications\">\n");
        html.append("                <h2 class=\"section-title\">Certifications</h2>\n");
        if (items.isEmpty()) {
            html.append("            </section>\n");
            return;
        }
        for (CompiledCertificationResponse cert : items) {
            html.append("                <article class=\"certification-item\">\n");
            html.append("                    <div class=\"item-header\">\n");
            html.append("                        <h3 class=\"cert-name\">").append(escapeHtml(cert.getName())).append("</h3>\n");
            html.append("                        <span class=\"issuing-org\">").append(escapeHtml(cert.getIssuingOrganization())).append("</span>\n");
            html.append("                    </div>\n");

            html.append("                    <div class=\"item-meta\">\n");
            if (cert.getIssueDate() != null) {
                html.append("                        <span class=\"issue-date\">Issued: ").append(formatDate(cert.getIssueDate())).append("</span>\n");
            }
            if (cert.getExpirationDate() != null) {
                html.append(" | <span class=\"expiration-date\">Expires: ").append(formatDate(cert.getExpirationDate())).append("</span>\n");
            }
            html.append("                    </div>\n");

            if (cert.getCredentialUrl() != null && isSafeUrl(cert.getCredentialUrl())) {
                html.append("                    <p class=\"credential-link\"><a href=\"").append(escapeHtml(cert.getCredentialUrl()))
                        .append("\" target=\"_blank\" rel=\"noopener noreferrer\">Verify Credential</a></p>\n");
            }
            if (cert.getDescription() != null && !cert.getDescription().isBlank()) {
                html.append("                    <p class=\"item-description\">").append(escapeHtml(cert.getDescription())).append("</p>\n");
            }
            html.append("                </article>\n");
        }
        html.append("            </section>\n");
    }

    @SuppressWarnings("unchecked")
    private void renderProjectSection(StringBuilder html, CompiledResumeSectionResponse section) {
        List<CompiledProjectResponse> items = extractItems(section.getContent());
        html.append("            <section class=\"resume-section section-projects\">\n");
        html.append("                <h2 class=\"section-title\">Projects</h2>\n");
        if (items.isEmpty()) {
            html.append("            </section>\n");
            return;
        }
        for (CompiledProjectResponse proj : items) {
            html.append("                <article class=\"project-item\">\n");
            html.append("                    <div class=\"item-header\">\n");
            html.append("                        <h3 class=\"project-name\">").append(escapeHtml(proj.getName())).append("</h3>\n");
            if (proj.getProjectType() != null) {
                html.append("                        <span class=\"project-type\">").append(escapeHtml(proj.getProjectType().name())).append("</span>\n");
            }
            html.append("                    </div>\n");

            if (proj.getDescription() != null && !proj.getDescription().isBlank()) {
                html.append("                    <p class=\"item-description\">").append(escapeHtml(proj.getDescription())).append("</p>\n");
            }

            if (proj.getTechStack() != null && !proj.getTechStack().isEmpty()) {
                html.append("                    <p class=\"tech-stack\"><strong>Technologies:</strong> ");
                for (int i = 0; i < proj.getTechStack().size(); i++) {
                    html.append(escapeHtml(proj.getTechStack().get(i)));
                    if (i < proj.getTechStack().size() - 1) {
                        html.append(", ");
                    }
                }
                html.append("</p>\n");
            }

            html.append("                    <div class=\"project-links\">\n");
            if (proj.getRepositoryUrl() != null && isSafeUrl(proj.getRepositoryUrl())) {
                html.append("                        <a href=\"").append(escapeHtml(proj.getRepositoryUrl())).append("\" target=\"_blank\" rel=\"noopener noreferrer\">Repository</a>\n");
            }
            if (proj.getLiveUrl() != null && isSafeUrl(proj.getLiveUrl())) {
                html.append("                        <a href=\"").append(escapeHtml(proj.getLiveUrl())).append("\" target=\"_blank\" rel=\"noopener noreferrer\">Live Demo</a>\n");
            }
            if (proj.getDocumentationUrl() != null && isSafeUrl(proj.getDocumentationUrl())) {
                html.append("                        <a href=\"").append(escapeHtml(proj.getDocumentationUrl())).append("\" target=\"_blank\" rel=\"noopener noreferrer\">Docs</a>\n");
            }
            html.append("                    </div>\n");

            html.append("                </article>\n");
        }
        html.append("            </section>\n");
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
        String lower = url.trim().toLowerCase();
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    private String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String generateStylesheet(ResumeTemplate template) {
        StringBuilder css = new StringBuilder();
        css.append("    <style>\n");
        css.append("        * { box-sizing: border-box; margin: 0; padding: 0; }\n");
        css.append("        body { font-family: 'Segoe UI', system-ui, -apple-system, sans-serif; color: #111827; background-color: #f9fafb; line-height: 1.5; padding: 2rem; }\n");
        css.append("        .resume-container { max-width: 800px; margin: 0 auto; background: #ffffff; padding: 2.5rem; border-radius: 8px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }\n");
        css.append("        .resume-header { margin-bottom: 2rem; padding-bottom: 1rem; border-bottom: 2px solid #e5e7eb; }\n");
        css.append("        .developer-name { font-size: 2.25rem; font-weight: 700; color: #1f2937; margin-bottom: 0.25rem; }\n");
        css.append("        .target-role { font-size: 1.25rem; font-weight: 500; color: #4b5563; }\n");
        css.append("        .resume-section { margin-bottom: 1.75rem; }\n");
        css.append("        .section-title { font-size: 1.35rem; font-weight: 600; text-transform: uppercase; letter-spacing: 0.05em; margin-bottom: 0.75rem; padding-bottom: 0.25rem; border-bottom: 1px solid #d1d5db; }\n");
        css.append("        .item-header { display: flex; justify-content: space-between; align-items: baseline; font-weight: 600; font-size: 1.1rem; }\n");
        css.append("        .item-meta { font-size: 0.9rem; color: #6b7280; margin-bottom: 0.5rem; }\n");
        css.append("        .item-description { font-size: 0.95rem; color: #374151; margin-top: 0.25rem; whitespace: pre-line; }\n");
        css.append("        .skills-list { list-style: none; display: flex; flex-wrap: wrap; gap: 0.5rem; }\n");
        css.append("        .skill-item { background: #f3f4f6; padding: 0.25rem 0.75rem; border-radius: 4px; font-size: 0.9rem; font-weight: 500; }\n");
        css.append("        .project-links { margin-top: 0.4rem; font-size: 0.85rem; }\n");
        css.append("        .project-links a { color: #2563eb; text-decoration: none; margin-right: 0.75rem; font-weight: 500; }\n");
        css.append("        .project-links a:hover { text-decoration: underline; }\n");

        if (template == ResumeTemplate.MODERN) {
            css.append("        .template-modern .developer-name { color: #0f766e; }\n");
            css.append("        .template-modern .section-title { color: #0d9488; border-left: 4px solid #0d9488; padding-left: 0.5rem; border-bottom: none; }\n");
            css.append("        .template-modern .skill-item { background: #ccfbf1; color: #115e59; }\n");
        } else if (template == ResumeTemplate.MINIMAL) {
            css.append("        .template-minimal { font-family: 'Courier New', Courier, monospace; background-color: #ffffff; }\n");
            css.append("        .template-minimal .resume-container { box-shadow: none; border: 1px solid #e5e7eb; }\n");
            css.append("        .template-minimal .section-title { font-weight: 400; border-bottom: 1px dashed #9ca3af; }\n");
        } else {
            css.append("        .template-professional .developer-name { color: #1e3a8a; }\n");
            css.append("        .template-professional .section-title { color: #1e40af; }\n");
        }

        css.append("        @page { size: A4 portrait; margin: 15mm; }\n");
        css.append("        .section-title, .experience-item, .education-item, .certification-item, .project-item { page-break-inside: avoid; break-inside: avoid; }\n");
        css.append("        @media print {\n");
        css.append("            body { background: none; padding: 0; color: #000000; }\n");
        css.append("            .resume-container { box-shadow: none; padding: 0; max-width: 100%; }\n");
        css.append("            a { text-decoration: none; color: #000000; }\n");
        css.append("        }\n");
        css.append("    </style>\n");

        return css.toString();
    }
}
