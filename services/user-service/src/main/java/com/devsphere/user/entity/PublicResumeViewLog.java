package com.devsphere.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "public_resume_view_logs")
public class PublicResumeViewLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, length = 255)
    private String publicId;

    @Column(name = "resume_profile_id", nullable = false)
    private Long resumeProfileId;

    @Column(name = "accessed_at", nullable = false)
    private Instant accessedAt;

    @Column(name = "ip_hash", nullable = false, length = 64)
    private String ipHash;

    @Column(name = "referrer", length = 512)
    private String referrer;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    public PublicResumeViewLog() {
    }

    public PublicResumeViewLog(String publicId, Long resumeProfileId, String ipHash, String referrer, String userAgent) {
        this.publicId = publicId;
        this.resumeProfileId = resumeProfileId;
        this.ipHash = ipHash;
        this.referrer = referrer;
        this.userAgent = userAgent;
        this.accessedAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.accessedAt == null) {
            this.accessedAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public Long getResumeProfileId() {
        return resumeProfileId;
    }

    public void setResumeProfileId(Long resumeProfileId) {
        this.resumeProfileId = resumeProfileId;
    }

    public Instant getAccessedAt() {
        return accessedAt;
    }

    public void setAccessedAt(Instant accessedAt) {
        this.accessedAt = accessedAt;
    }

    public String getIpHash() {
        return ipHash;
    }

    public void setIpHash(String ipHash) {
        this.ipHash = ipHash;
    }

    public String getReferrer() {
        return referrer;
    }

    public void setReferrer(String referrer) {
        this.referrer = referrer;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
}
