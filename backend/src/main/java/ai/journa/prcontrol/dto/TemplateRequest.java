package ai.journa.prcontrol.dto;

import jakarta.validation.constraints.NotBlank;

public class TemplateRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String subject;

    @NotBlank
    private String body;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
