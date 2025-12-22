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
import ai.journa.prcontrol.service.integration.model.EmailResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutreachServiceTest {
    @Mock
    private OutreachEmailRepository outreachEmailRepository;
    @Mock
    private OutreachTemplateRepository outreachTemplateRepository;
    @Mock
    private ArticleRepository articleRepository;
    @Mock
    private JournalistRepository journalistRepository;
    @Mock
    private EmailProvider emailProvider;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private OutreachService outreachService;

    @Test
    void sendCreatesOutreachRecord() {
        Article article = new Article();
        article.setId(1L);
        Journalist journalist = new Journalist();
        journalist.setId(2L);
        journalist.setEmail("reporter@example.com");
        OutreachTemplate template = new OutreachTemplate();
        template.setId(3L);

        OutreachComposeRequest request = new OutreachComposeRequest();
        request.setArticleId(1L);
        request.setJournalistId(2L);
        request.setTemplateId(3L);
        request.setFinalSubject("Hello");
        request.setFinalBody("Body");

        when(articleRepository.findById(1L)).thenReturn(Optional.of(article));
        when(journalistRepository.findById(2L)).thenReturn(Optional.of(journalist));
        when(outreachTemplateRepository.findById(3L)).thenReturn(Optional.of(template));
        when(outreachEmailRepository.save(any(OutreachEmail.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(emailProvider.sendEmail(any())).thenReturn(new EmailResult("SENT", "mock-1"));

        OutreachEmail email = outreachService.send("user@example.com", request);

        assertThat(email.getStatus()).isEqualTo("SENT");
        assertThat(email.getProviderMessageId()).isEqualTo("mock-1");
    }
}
