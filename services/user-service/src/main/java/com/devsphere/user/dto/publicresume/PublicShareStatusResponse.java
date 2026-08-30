package com.devsphere.user.dto.publicresume;

import com.devsphere.user.entity.ResumeProfile;
import java.time.Instant;

public class PublicShareStatusResponse {

    private String publicResumeId;
    private boolean publicEnabled;
    private Instant publicEnabledAt;
    private String shareUrl;

    public PublicShareStatusResponse() {
    }

    public PublicShareStatusResponse(String publicResumeId, boolean publicEnabled, Instant publicEnabledAt, String shareUrl) {
        this.publicResumeId = publicResumeId;
        this.publicEnabled = publicEnabled;
        this.publicEnabledAt = publicEnabledAt;
        this.shareUrl = shareUrl;
    }

    public PublicShareStatusResponse(ResumeProfile profile) {
        if (profile != null) {
            this.publicResumeId = profile.getPublicId();
            this.publicEnabled = profile.isPublicEnabled();
            this.publicEnabledAt = profile.getPublicEnabledAt();
            this.shareUrl = profile.getPublicId() != null ? "/api/v1/public/resumes/" + profile.getPublicId() : null;
        }
    }

    public String getPublicResumeId() {
        return publicResumeId;
    }

    public void setPublicResumeId(String publicResumeId) {
        this.publicResumeId = publicResumeId;
    }

    public boolean isPublicEnabled() {
        return publicEnabled;
    }

    public void setPublicEnabled(boolean publicEnabled) {
        this.publicEnabled = publicEnabled;
    }

    public Instant getPublicEnabledAt() {
        return publicEnabledAt;
    }

    public void setPublicEnabledAt(Instant publicEnabledAt) {
        this.publicEnabledAt = publicEnabledAt;
    }

    public String getShareUrl() {
        return shareUrl;
    }

    public void setShareUrl(String shareUrl) {
        this.shareUrl = shareUrl;
    }
}
