package com.devsphere.user.dto.publicresume;

import com.devsphere.user.dto.compilation.CompiledResumeResponse;
import com.devsphere.user.dto.compilation.CompiledResumeSectionResponse;
import com.devsphere.user.dto.compilation.CompiledSummaryResponse;
import com.devsphere.user.entity.ResumeSectionType;
import com.devsphere.user.entity.ResumeTemplate;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PublicResumeResponseTest {

    @Test
    void generateTitle_WithBothNameAndTargetRole_FormatsCorrectly() {
        String title = PublicResumeResponse.generateTitle("  John Doe  ", " Senior Java Engineer ");
        assertThat(title).isEqualTo("John Doe — Senior Java Engineer");
    }

    @Test
    void generateTitle_WithHTMLAndExtraWhitespace_StripsHTMLAndNormalizesWhitespace() {
        String title = PublicResumeResponse.generateTitle("<b>John Doe</b>", "<i>DevOps   Specialist</i>");
        assertThat(title).isEqualTo("John Doe — DevOps Specialist");
    }

    @Test
    void generateTitle_WithoutTargetRole_UsesFallback() {
        String title = PublicResumeResponse.generateTitle("Jane Smith", null);
        assertThat(title).isEqualTo("Jane Smith — Resume");
    }

    @Test
    void generateTitle_WithoutName_UsesFallback() {
        String title = PublicResumeResponse.generateTitle("", "Cloud Architect");
        assertThat(title).isEqualTo("Cloud Architect — Resume");
    }

    @Test
    void generateTitle_WithoutBoth_ReturnsDefaultTitle() {
        String title = PublicResumeResponse.generateTitle(null, "   ");
        assertThat(title).isEqualTo("Professional Resume");
    }

    @Test
    void generateTitle_WhenExceedingLength_TruncatesWithEllipsis() {
        String longName = "A".repeat(200);
        String longRole = "B".repeat(100);
        String title = PublicResumeResponse.generateTitle(longName, longRole);
        assertThat(title).hasSize(255);
        assertThat(title).endsWith("...");
    }

    @Test
    void generateDescription_WithSummarySection_ExtractsAndSanitizesText() {
        CompiledSummaryResponse summaryContent = new CompiledSummaryResponse("<p>Experienced <b>software engineer</b> building high-scale microservices.</p>");
        CompiledResumeSectionResponse summarySection = new CompiledResumeSectionResponse(ResumeSectionType.SUMMARY, 1, true, summaryContent);

        String description = PublicResumeResponse.generateDescription(List.of(summarySection));
        assertThat(description).isEqualTo("Experienced software engineer building high-scale microservices.");
    }

    @Test
    void generateDescription_WithoutSummarySection_ReturnsDefaultDescription() {
        String description = PublicResumeResponse.generateDescription(List.of());
        assertThat(description).isEqualTo("Professional resume and career profile.");
    }

    @Test
    void generateDescription_WhenExceedingLength_TruncatesWithEllipsis() {
        String longSummary = "word ".repeat(100); // 500 chars
        CompiledSummaryResponse summaryContent = new CompiledSummaryResponse(longSummary);
        CompiledResumeSectionResponse summarySection = new CompiledResumeSectionResponse(ResumeSectionType.SUMMARY, 1, true, summaryContent);

        String description = PublicResumeResponse.generateDescription(List.of(summarySection));
        assertThat(description).hasSize(300);
        assertThat(description).endsWith("...");
    }

    @Test
    void fullResponseConstructor_EnrichesMetadataAndOmitsInternalIds() {
        CompiledResumeResponse compiled = new CompiledResumeResponse(100L, 10L, "Alice Developer", "Fullstack Lead", ResumeTemplate.MODERN, List.of());
        PublicResumeResponse response = new PublicResumeResponse("pub-token-999", 2, compiled);

        assertThat(response.getPublicResumeId()).isEqualTo("pub-token-999");
        assertThat(response.getPublishedVersion()).isEqualTo(2);
        assertThat(response.getName()).isEqualTo("Alice Developer");
        assertThat(response.getTargetRole()).isEqualTo("Fullstack Lead");
        assertThat(response.getTitle()).isEqualTo("Alice Developer — Fullstack Lead");
        assertThat(response.getDescription()).isEqualTo("Professional resume and career profile.");
        assertThat(response.getTemplate()).isEqualTo(ResumeTemplate.MODERN);
    }
}
