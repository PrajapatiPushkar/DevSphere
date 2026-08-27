package com.devsphere.user.renderer;

import com.devsphere.user.dto.compilation.CompiledCertificationResponse;
import com.devsphere.user.dto.compilation.CompiledExperienceResponse;
import com.devsphere.user.dto.compilation.CompiledProjectResponse;
import com.devsphere.user.dto.compilation.CompiledResumeResponse;
import com.devsphere.user.dto.compilation.CompiledResumeSectionResponse;
import com.devsphere.user.dto.compilation.CompiledSkillItemResponse;
import com.devsphere.user.dto.compilation.CompiledSkillsResponse;
import com.devsphere.user.dto.compilation.CompiledSummaryResponse;
import com.devsphere.user.entity.Certification;
import com.devsphere.user.entity.DeveloperProject;
import com.devsphere.user.entity.EmploymentType;
import com.devsphere.user.entity.Experience;
import com.devsphere.user.entity.Proficiency;
import com.devsphere.user.entity.ProjectType;
import com.devsphere.user.entity.ResumeSectionType;
import com.devsphere.user.entity.ResumeTemplate;
import com.devsphere.user.entity.Skill;
import com.devsphere.user.entity.SkillCategory;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HtmlResumeRendererTest {

    private HtmlResumeRenderer renderer;

    @BeforeEach
    void setUp() {
        renderer = new HtmlResumeRenderer();
    }

    @Test
    void render_validCompiledResume_returnsCompleteHtmlDocument() {
        CompiledResumeSectionResponse summarySection = new CompiledResumeSectionResponse(
                ResumeSectionType.SUMMARY, 1, true, new CompiledSummaryResponse("Experienced Java Developer")
        );

        CompiledResumeResponse compiled = new CompiledResumeResponse(
                50L, 50L, "Java Resume", "Senior Java Engineer", ResumeTemplate.PROFESSIONAL, List.of(summarySection)
        );

        String html = renderer.render(compiled);

        assertThat(html).startsWith("<!DOCTYPE html>");
        assertThat(html).contains("<title>Java Resume</title>");
        assertThat(html).contains("Experienced Java Developer");
        assertThat(html).endsWith("</html>");
    }

    @Test
    void render_xssPayloadInSummaryAndDescription_escapesHtmlEntities() {
        String xssPayload = "<script>alert('xss')</script>";

        CompiledResumeSectionResponse summarySection = new CompiledResumeSectionResponse(
                ResumeSectionType.SUMMARY, 1, true, new CompiledSummaryResponse(xssPayload)
        );

        Experience expEntity = new Experience(100L, "Acme", "Dev", EmploymentType.FULL_TIME, LocalDate.now());
        expEntity.setDescription(xssPayload);
        CompiledExperienceResponse expResp = new CompiledExperienceResponse(expEntity, 1);

        CompiledResumeSectionResponse expSection = new CompiledResumeSectionResponse(
                ResumeSectionType.EXPERIENCE, 2, true, Map.of("items", List.of(expResp))
        );

        CompiledResumeResponse compiled = new CompiledResumeResponse(
                50L, 50L, xssPayload, "Role", ResumeTemplate.PROFESSIONAL, List.of(summarySection, expSection)
        );

        String html = renderer.render(compiled);

        assertThat(html).doesNotContain("<script>");
        assertThat(html).contains("&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;");
    }

    @Test
    void render_unsafeUrlSchemes_doesNotRenderClickableLink() {
        DeveloperProject projEntity = new DeveloperProject(100L, "Project", ProjectType.PERSONAL);
        projEntity.setRepositoryUrl("javascript:alert(1)");
        projEntity.setLiveUrl("data:text/html,<script>alert(1)</script>");

        CompiledProjectResponse projResp = new CompiledProjectResponse(projEntity, 1);
        CompiledResumeSectionResponse projSection = new CompiledResumeSectionResponse(
                ResumeSectionType.PROJECTS, 1, true, Map.of("items", List.of(projResp))
        );

        CompiledResumeResponse compiled = new CompiledResumeResponse(
                50L, 50L, "Resume", "Role", ResumeTemplate.PROFESSIONAL, List.of(projSection)
        );

        String html = renderer.render(compiled);

        assertThat(html).doesNotContain("javascript:alert");
        assertThat(html).doesNotContain("data:text/html");
    }

    @Test
    void render_safeHttpsUrl_rendersClickableLink() {
        DeveloperProject projEntity = new DeveloperProject(100L, "Project", ProjectType.PERSONAL);
        projEntity.setRepositoryUrl("https://github.com/myrepo");

        CompiledProjectResponse projResp = new CompiledProjectResponse(projEntity, 1);
        CompiledResumeSectionResponse projSection = new CompiledResumeSectionResponse(
                ResumeSectionType.PROJECTS, 1, true, Map.of("items", List.of(projResp))
        );

        CompiledResumeResponse compiled = new CompiledResumeResponse(
                50L, 50L, "Resume", "Role", ResumeTemplate.PROFESSIONAL, List.of(projSection)
        );

        String html = renderer.render(compiled);

        assertThat(html).contains("href=\"https://github.com/myrepo\"");
    }

    @Test
    void render_templateVariations_appliesTemplateClassAndStyling() {
        CompiledResumeResponse prof = new CompiledResumeResponse(50L, 50L, "R", "Role", ResumeTemplate.PROFESSIONAL, List.of());
        CompiledResumeResponse modern = new CompiledResumeResponse(50L, 50L, "R", "Role", ResumeTemplate.MODERN, List.of());
        CompiledResumeResponse minimal = new CompiledResumeResponse(50L, 50L, "R", "Role", ResumeTemplate.MINIMAL, List.of());

        assertThat(renderer.render(prof)).contains("class=\"template-professional\"");
        assertThat(renderer.render(modern)).contains("class=\"template-modern\"");
        assertThat(renderer.render(minimal)).contains("class=\"template-minimal\"");
    }

    @Test
    void render_deterministicOutput_multipleCallsProduceIdenticalHtml() {
        CompiledResumeSectionResponse summarySection = new CompiledResumeSectionResponse(
                ResumeSectionType.SUMMARY, 1, true, new CompiledSummaryResponse("Experienced Java Developer")
        );

        CompiledResumeResponse compiled = new CompiledResumeResponse(
                50L, 50L, "Java Resume", "Senior Java Engineer", ResumeTemplate.PROFESSIONAL, List.of(summarySection)
        );

        String html1 = renderer.render(compiled);
        String html2 = renderer.render(compiled);

        assertThat(html1).isEqualTo(html2);
    }
}
