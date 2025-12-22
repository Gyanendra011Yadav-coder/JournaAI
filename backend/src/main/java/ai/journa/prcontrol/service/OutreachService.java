package ai.journa.prcontrol.service;

import ai.journa.prcontrol.domain.Article;
import ai.journa.prcontrol.domain.Journalist;
import ai.journa.prcontrol.domain.OutreachEmail;
import ai.journa.prcontrol.domain.OutreachTemplate;
import ai.journa.prcontrol.dto.OutreachComposeRequest;
import ai.journa.prcontrol.repository.ArticleRepository;
import ai.journa.prcontrol.repository.JournalistRepository;
import ai.journa.prcontrol.repository.OutreachEmailRepository;
import ai.journa.prcontrol.repository.OutreachTemplateRepository;
import ai.journa.prcontrol.service.integration.EmailProvider;
import ai.journa.prcontrol.service.integration.model.EmailRequest;
import ai.journa.prcontrol.service.integration.model.EmailResult;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class OutreachService {
    private final OutreachEmailRepository outreachEmailRepository;
    private final OutreachTemplateRepository outreachTemplateRepository;
    private final ArticleRepository articleRepository;
    private final JournalistRepository journalistRepository;
    private final EmailProvider emailProvider;
    private final AuditService auditService;

    public OutreachService(OutreachEmailRepository outreachEmailRepository,
                           OutreachTemplateRepository outreachTemplateRepository,
                           ArticleRepository articleRepository,
                           JournalistRepository journalistRepository,
                           EmailProvider emailProvider,
                           AuditService auditService) {
        this.outreachEmailRepository = outreachEmailRepository;
        this.outreachTemplateRepository = outreachTemplateRepository;
        this.articleRepository = articleRepository;
        this.journalistRepository = journalistRepository;
        this.emailProvider = emailProvider;
        this.auditService = auditService;
    }

    public OutreachEmail send(String actor, OutreachComposeRequest request) {
        Article article = articleRepository.findById(request.getArticleId())
                .orElseThrow(() -> new IllegalStateException("Article not found"));
        Journalist journalist = journalistRepository.findById(request.getJournalistId())
                .orElseThrow(() -> new IllegalStateException("Journalist not found"));
        OutreachTemplate template = outreachTemplateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new IllegalStateException("Template not found"));

        OutreachEmail outreachEmail = new OutreachEmail();
        outreachEmail.setArticle(article);
        outreachEmail.setJournalist(journalist);
        outreachEmail.setTemplate(template);
        outreachEmail.setFinalSubject(request.getFinalSubject());
        outreachEmail.setFinalBody(request.getFinalBody());
        outreachEmail.setStatus("QUEUED");
        outreachEmailRepository.save(outreachEmail);

        EmailRequest emailRequest = new EmailRequest();
        emailRequest.setFrom("noreply@prcontrol.local");
        emailRequest.setTo(List.of(journalist.getEmail()));
        emailRequest.setSubject(request.getFinalSubject());
        emailRequest.setHtmlBody(request.getFinalBody());
        emailRequest.setTextBody(request.getFinalBody());
        emailRequest.setMetadata(Map.of("articleId", String.valueOf(article.getId())));

        EmailResult result = emailProvider.sendEmail(emailRequest);
        outreachEmail.setStatus(result.getStatus());
        outreachEmail.setProviderMessageId(result.getProviderMessageId());
        outreachEmail.setSentAt(Instant.now());
        outreachEmailRepository.save(outreachEmail);

        auditService.record(actor, "SEND", "outreach_email", "{\"emailId\":" + outreachEmail.getId() + "}");
        return outreachEmail;
    }
}
