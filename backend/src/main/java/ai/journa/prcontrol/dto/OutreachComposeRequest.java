package ai.journa.prcontrol.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class OutreachComposeRequest {
    @NotNull
    private Long articleId;

    @NotNull
    private Long journalistId;

    @NotNull
    private Long templateId;

    @NotBlank
    private String finalSubject;

    @NotBlank
    private String finalBody;

    public Long getArticleId() {
        return articleId;
    }

    public void setArticleId(Long articleId) {
        this.articleId = articleId;
    }

    public Long getJournalistId() {
        return journalistId;
    }

    public void setJournalistId(Long journalistId) {
        this.journalistId = journalistId;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public String getFinalSubject() {
        return finalSubject;
    }

    public void setFinalSubject(String finalSubject) {
        this.finalSubject = finalSubject;
    }

    public String getFinalBody() {
        return finalBody;
    }

    public void setFinalBody(String finalBody) {
        this.finalBody = finalBody;
    }
}
