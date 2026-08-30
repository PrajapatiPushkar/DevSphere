package com.devsphere.user.dto.publicresume;

import com.devsphere.user.dto.compilation.CompiledResumeResponse;
import com.devsphere.user.dto.compilation.CompiledResumeSectionResponse;
import com.devsphere.user.dto.compilation.CompiledSummaryResponse;
import com.devsphere.user.entity.ResumeSectionType;
import com.devsphere.user.entity.ResumeTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PublicResumeResponse {

    private String title;
    private String description;
    private String publicResumeId;
    private Integer publishedVersion;
    private String name;
    private String targetRole;
    private ResumeTemplate template;
    private List<PublicResumeSectionResponse> sections = new ArrayList<>();

    public PublicResumeResponse() {
    }

    public PublicResumeResponse(String name, String targetRole, ResumeTemplate template, List<PublicResumeSectionResponse> sections) {
        this.name = name;
        this.targetRole = targetRole;
        this.template = template;
        this.sections = sections != null ? sections : new ArrayList<>();
        this.title = generateTitle(name, targetRole);
        this.description = "Professional resume and career profile.";
    }

    public PublicResumeResponse(CompiledResumeResponse compiled) {
        this(null, null, compiled);
    }

    public PublicResumeResponse(String publicResumeId, Integer publishedVersion, CompiledResumeResponse compiled) {
        this.publicResumeId = publicResumeId;
        this.publishedVersion = publishedVersion;
        if (compiled != null) {
            this.name = compiled.getName();
            this.targetRole = compiled.getTargetRole();
            this.template = compiled.getTemplate();
            this.sections = new ArrayList<>();
            if (compiled.getSections() != null) {
                for (CompiledResumeSectionResponse sec : compiled.getSections()) {
                    if (sec != null && Boolean.TRUE.equals(sec.getVisible())) {
                        this.sections.add(new PublicResumeSectionResponse(sec));
                    }
                }
            }
            this.title = generateTitle(this.name, this.targetRole);
            this.description = generateDescription(compiled.getSections());
        } else {
            this.title = generateTitle(null, null);
            this.description = "Professional resume and career profile.";
        }
    }

    public static String generateTitle(String rawName, String rawTargetRole) {
        String name = sanitizeText(rawName);
        String targetRole = sanitizeText(rawTargetRole);

        String result;
        if (name != null && targetRole != null) {
            result = name + " — " + targetRole;
        } else if (name != null) {
            result = name + " — Resume";
        } else if (targetRole != null) {
            result = targetRole + " — Resume";
        } else {
            result = "Professional Resume";
        }

        return truncate(result, 255);
    }

    public static String generateDescription(List<CompiledResumeSectionResponse> sections) {
        String summaryText = extractSummaryText(sections);
        String sanitized = sanitizeText(summaryText);

        if (sanitized != null && !sanitized.isBlank()) {
            return truncate(sanitized, 300);
        }

        return "Professional resume and career profile.";
    }

    private static String extractSummaryText(List<CompiledResumeSectionResponse> sections) {
        if (sections == null) {
            return null;
        }
        for (CompiledResumeSectionResponse sec : sections) {
            if (sec != null && sec.getSectionType() == ResumeSectionType.SUMMARY && sec.getContent() != null) {
                Object content = sec.getContent();
                if (content instanceof CompiledSummaryResponse summaryObj) {
                    return summaryObj.getText();
                } else if (content instanceof String str) {
                    return str;
                } else if (content instanceof Map<?, ?> map && map.containsKey("text")) {
                    Object textObj = map.get("text");
                    return textObj != null ? textObj.toString() : null;
                }
            }
        }
        return null;
    }

    private static String sanitizeText(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String stripped = input.replaceAll("<[^>]*>", " ");
        String normalized = stripped.replaceAll("\\s+", " ").trim();
        return normalized.isBlank() ? null : normalized;
    }

    private static String truncate(String input, int maxLength) {
        if (input == null || input.length() <= maxLength) {
            return input;
        }
        if (maxLength <= 3) {
            return input.substring(0, maxLength);
        }
        return input.substring(0, maxLength - 3).trim() + "...";
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPublicResumeId() {
        return publicResumeId;
    }

    public void setPublicResumeId(String publicResumeId) {
        this.publicResumeId = publicResumeId;
    }

    public Integer getPublishedVersion() {
        return publishedVersion;
    }

    public void setPublishedVersion(Integer publishedVersion) {
        this.publishedVersion = publishedVersion;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }

    public ResumeTemplate getTemplate() {
        return template;
    }

    public void setTemplate(ResumeTemplate template) {
        this.template = template;
    }

    public List<PublicResumeSectionResponse> getSections() {
        return sections;
    }

    public void setSections(List<PublicResumeSectionResponse> sections) {
        this.sections = sections != null ? sections : new ArrayList<>();
    }
}
