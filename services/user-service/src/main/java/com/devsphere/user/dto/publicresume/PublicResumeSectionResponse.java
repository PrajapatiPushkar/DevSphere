package com.devsphere.user.dto.publicresume;

import com.devsphere.user.dto.compilation.CompiledCertificationResponse;
import com.devsphere.user.dto.compilation.CompiledEducationResponse;
import com.devsphere.user.dto.compilation.CompiledExperienceResponse;
import com.devsphere.user.dto.compilation.CompiledProjectResponse;
import com.devsphere.user.dto.compilation.CompiledResumeSectionResponse;
import com.devsphere.user.dto.compilation.CompiledSkillsResponse;
import com.devsphere.user.dto.compilation.CompiledSummaryResponse;
import com.devsphere.user.entity.ResumeSectionType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PublicResumeSectionResponse {

    private ResumeSectionType sectionType;
    private Integer displayOrder;
    private Boolean visible;
    private Object content;

    public PublicResumeSectionResponse() {
    }

    public PublicResumeSectionResponse(ResumeSectionType sectionType, Integer displayOrder, Boolean visible, Object content) {
        this.sectionType = sectionType;
        this.displayOrder = displayOrder;
        this.visible = visible;
        this.content = sanitizeContent(sectionType, content);
    }

    public PublicResumeSectionResponse(CompiledResumeSectionResponse compiled) {
        if (compiled != null) {
            this.sectionType = compiled.getSectionType();
            this.displayOrder = compiled.getDisplayOrder();
            this.visible = compiled.getVisible();
            this.content = sanitizeContent(compiled.getSectionType(), compiled.getContent());
        }
    }

    private static Object sanitizeContent(ResumeSectionType type, Object rawContent) {
        if (rawContent == null || type == null) {
            return rawContent;
        }

        switch (type) {
            case SUMMARY -> {
                if (rawContent instanceof CompiledSummaryResponse csr) {
                    return new PublicSummaryResponse(csr);
                }
                return rawContent;
            }
            case EXPERIENCE -> {
                if (rawContent instanceof Map<?, ?> map && map.containsKey("items")) {
                    Object itemsRaw = map.get("items");
                    if (itemsRaw instanceof List<?> list) {
                        List<PublicExperienceResponse> publicItems = new ArrayList<>();
                        for (Object obj : list) {
                            if (obj instanceof CompiledExperienceResponse cer) {
                                publicItems.add(new PublicExperienceResponse(cer));
                            } else {
                                publicItems.add((PublicExperienceResponse) obj);
                            }
                        }
                        return Map.of("items", publicItems);
                    }
                }
                return rawContent;
            }
            case EDUCATION -> {
                if (rawContent instanceof Map<?, ?> map && map.containsKey("items")) {
                    Object itemsRaw = map.get("items");
                    if (itemsRaw instanceof List<?> list) {
                        List<PublicEducationResponse> publicItems = new ArrayList<>();
                        for (Object obj : list) {
                            if (obj instanceof CompiledEducationResponse cer) {
                                publicItems.add(new PublicEducationResponse(cer));
                            } else {
                                publicItems.add((PublicEducationResponse) obj);
                            }
                        }
                        return Map.of("items", publicItems);
                    }
                }
                return rawContent;
            }
            case SKILLS -> {
                if (rawContent instanceof CompiledSkillsResponse csr) {
                    return new PublicSkillsResponse(csr);
                }
                return rawContent;
            }
            case CERTIFICATIONS -> {
                if (rawContent instanceof Map<?, ?> map && map.containsKey("items")) {
                    Object itemsRaw = map.get("items");
                    if (itemsRaw instanceof List<?> list) {
                        List<PublicCertificationResponse> publicItems = new ArrayList<>();
                        for (Object obj : list) {
                            if (obj instanceof CompiledCertificationResponse ccr) {
                                publicItems.add(new PublicCertificationResponse(ccr));
                            } else {
                                publicItems.add((PublicCertificationResponse) obj);
                            }
                        }
                        return Map.of("items", publicItems);
                    }
                }
                return rawContent;
            }
            case PROJECTS -> {
                if (rawContent instanceof Map<?, ?> map && map.containsKey("items")) {
                    Object itemsRaw = map.get("items");
                    if (itemsRaw instanceof List<?> list) {
                        List<PublicProjectResponse> publicItems = new ArrayList<>();
                        for (Object obj : list) {
                            if (obj instanceof CompiledProjectResponse cpr) {
                                publicItems.add(new PublicProjectResponse(cpr));
                            } else {
                                publicItems.add((PublicProjectResponse) obj);
                            }
                        }
                        return Map.of("items", publicItems);
                    }
                }
                return rawContent;
            }
            default -> {
                return rawContent;
            }
        }
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
        this.content = sanitizeContent(this.sectionType, content);
    }
}
