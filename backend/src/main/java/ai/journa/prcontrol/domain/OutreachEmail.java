package ai.journa.prcontrol.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "outreach_emails")
public class OutreachEmail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id")
    private Article article;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journalist_id")
    private Journalist journalist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private OutreachTemplate template;

    @Column(nullable = false)
    private String finalSubject;

    @Column(columnDefinition = "text", nullable = false)
    private String finalBody;

    @Column(nullable = false)
    private String status;

    private Instant sentAt;

    private String providerMessageId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Article getArticle() {
        return article;
    }

    public void setArticle(Article article) {
        this.article = article;
    }

    public Journalist getJournalist() {
        return journalist;
    }

    public void setJournalist(Journalist journalist) {
        this.journalist = journalist;
    }

    public OutreachTemplate getTemplate() {
        return template;
    }

    public void setTemplate(OutreachTemplate template) {
        this.template = template;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public void setProviderMessageId(String providerMessageId) {
        this.providerMessageId = providerMessageId;
    }
}
