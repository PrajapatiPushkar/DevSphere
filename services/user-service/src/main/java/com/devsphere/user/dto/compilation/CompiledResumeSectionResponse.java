package com.devsphere.user.dto.compilation;

import com.devsphere.user.entity.ResumeSectionType;

public class CompiledResumeSectionResponse {

    private ResumeSectionType sectionType;
    private Integer displayOrder;
    private Boolean visible;
    private Object content;

    public CompiledResumeSectionResponse() {
    }

    public CompiledResumeSectionResponse(ResumeSectionType sectionType, Integer displayOrder, Boolean visible, Object content) {
        this.sectionType = sectionType;
        this.displayOrder = displayOrder;
        this.visible = visible;
        this.content = content;
    }

    public ResumeSectionType getSectionType() {
        return sectionType;
    }

    public void setSectionType(ResumeSectionType sectionType) {
        this.sectionType = sectionType;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }

    public Object getContent() {
        return content;
    }

    public void setContent(Object content) {
        this.content = content;
    }
}
