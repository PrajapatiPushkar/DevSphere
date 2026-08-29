package com.devsphere.user.dto.publicresume;

import com.devsphere.user.dto.compilation.CompiledResumeResponse;
import com.devsphere.user.dto.compilation.CompiledResumeSectionResponse;
import com.devsphere.user.entity.ResumeTemplate;
import java.util.ArrayList;
import java.util.List;

public class PublicResumeResponse {

    private String name;
    private String targetRole;
    private ResumeTemplate template;
    private List<CompiledResumeSectionResponse> sections = new ArrayList<>();

    public PublicResumeResponse() {
    }

    public PublicResumeResponse(String name, String targetRole, ResumeTemplate template, List<CompiledResumeSectionResponse> sections) {
        this.name = name;
        this.targetRole = targetRole;
        this.template = template;
        this.sections = sections != null ? sections : new ArrayList<>();
    }

    public PublicResumeResponse(CompiledResumeResponse compiled) {
        if (compiled != null) {
            this.name = compiled.getName();
            this.targetRole = compiled.getTargetRole();
            this.template = compiled.getTemplate();
            this.sections = compiled.getSections() != null ? compiled.getSections() : new ArrayList<>();
        }
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
