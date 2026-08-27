package com.devsphere.user.dto.compilation;

import com.devsphere.user.entity.ResumeTemplate;
import java.util.ArrayList;
import java.util.List;

public class CompiledResumeResponse {

    private Long id;
    private Long resumeProfileId;
    private String name;
    private String targetRole;
    private ResumeTemplate template;
    private List<CompiledResumeSectionResponse> sections = new ArrayList<>();

    public CompiledResumeResponse() {
    }

    public CompiledResumeResponse(Long id, Long resumeProfileId, String name, String targetRole, ResumeTemplate template, List<CompiledResumeSectionResponse> sections) {
        this.id = id;
        this.resumeProfileId = resumeProfileId;
        this.name = name;
        this.targetRole = targetRole;
        this.template = template;
        this.sections = sections != null ? sections : new ArrayList<>();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getResumeProfileId() {
        return resumeProfileId;
    }

    public void setResumeProfileId(Long resumeProfileId) {
        this.resumeProfileId = resumeProfileId;
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

    public List<CompiledResumeSectionResponse> getSections() {
        return sections;
    }

    public void setSections(List<CompiledResumeSectionResponse> sections) {
        this.sections = sections != null ? sections : new ArrayList<>();
    }
}
