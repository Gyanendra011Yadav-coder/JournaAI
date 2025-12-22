package ai.journa.prcontrol.service.integration.real;

import ai.journa.prcontrol.service.integration.EmailProvider;
import ai.journa.prcontrol.service.integration.model.EmailRequest;
import ai.journa.prcontrol.service.integration.model.EmailResult;

public class SmtpEmailProvider implements EmailProvider {
    @Override
    public EmailResult sendEmail(EmailRequest request) {
        String smtpHost = System.getenv("SMTP_HOST");
        String smtpUser = System.getenv("SMTP_USER");
        if (smtpHost == null || smtpHost.isBlank() || smtpUser == null || smtpUser.isBlank()) {
            throw new IllegalStateException("SMTP_HOST and SMTP_USER are not configured");
        }
        return new EmailResult("SENT", "smtp-placeholder");
    }
}
