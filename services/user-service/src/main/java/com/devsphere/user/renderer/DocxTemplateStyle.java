package com.devsphere.user.renderer;

import com.devsphere.user.entity.ResumeTemplate;

public class DocxTemplateStyle {

    private final String documentFont;
    private final String headingFont;
    private final String primaryHexColor;
    private final String secondaryHexColor;
    private final int titleSizePt;
    private final int targetRoleSizePt;
    private final int sectionHeadingSizePt;
    private final int itemHeaderSizePt;
    private final int bodySizePt;
    private final int marginTopTwips;
    private final int marginBottomTwips;
    private final int marginLeftTwips;
    private final int marginRightTwips;

    public DocxTemplateStyle(String documentFont,
                              String headingFont,
                              String primaryHexColor,
                              String secondaryHexColor,
                              int titleSizePt,
                              int targetRoleSizePt,
                              int sectionHeadingSizePt,
                              int itemHeaderSizePt,
                              int bodySizePt,
                              int marginTopTwips,
                              int marginBottomTwips,
                              int marginLeftTwips,
                              int marginRightTwips) {
        this.documentFont = documentFont;
        this.headingFont = headingFont;
        this.primaryHexColor = primaryHexColor;
        this.secondaryHexColor = secondaryHexColor;
        this.titleSizePt = titleSizePt;
        this.targetRoleSizePt = targetRoleSizePt;
        this.sectionHeadingSizePt = sectionHeadingSizePt;
        this.itemHeaderSizePt = itemHeaderSizePt;
        this.bodySizePt = bodySizePt;
        this.marginTopTwips = marginTopTwips;
        this.marginBottomTwips = marginBottomTwips;
        this.marginLeftTwips = marginLeftTwips;
        this.marginRightTwips = marginRightTwips;
    }

    public static DocxTemplateStyle forTemplate(ResumeTemplate template) {
        if (template == null) {
            template = ResumeTemplate.PROFESSIONAL;
        }

        return switch (template) {
            case MODERN -> new DocxTemplateStyle(
                    "Segoe UI", "Segoe UI",
                    "0D9488", "0F766E",
                    24, 13, 14, 11, 10,
                    1080, 1080, 1080, 1080
            );
            case MINIMAL -> new DocxTemplateStyle(
                    "Courier New", "Courier New",
                    "374151", "4B5563",
                    20, 12, 13, 10, 10,
                    1440, 1440, 1440, 1440
            );
            case PROFESSIONAL -> new DocxTemplateStyle(
                    "Calibri", "Calibri",
                    "1E40AF", "1F2937",
                    22, 13, 14, 11, 10,
                    1080, 1080, 1080, 1080
            );
        };
    }

    public String getDocumentFont() {
        return documentFont;
    }

    public String getHeadingFont() {
        return headingFont;
    }

    public String getPrimaryHexColor() {
        return primaryHexColor;
    }

    public String getSecondaryHexColor() {
        return secondaryHexColor;
    }

    public int getTitleSizePt() {
        return titleSizePt;
    }

    public int getTargetRoleSizePt() {
        return targetRoleSizePt;
    }

    public int getSectionHeadingSizePt() {
        return sectionHeadingSizePt;
    }

    public int getItemHeaderSizePt() {
        return itemHeaderSizePt;
    }

    public int getBodySizePt() {
        return bodySizePt;
    }

    public int getMarginTopTwips() {
        return marginTopTwips;
    }

    public int getMarginBottomTwips() {
        return marginBottomTwips;
    }

    public int getMarginLeftTwips() {
        return marginLeftTwips;
    }

    public int getMarginRightTwips() {
        return marginRightTwips;
    }
}
