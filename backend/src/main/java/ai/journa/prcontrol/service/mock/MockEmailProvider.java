package ai.journa.prcontrol.service.mock;

import ai.journa.prcontrol.service.integration.EmailProvider;
import ai.journa.prcontrol.service.integration.model.EmailRequest;
import ai.journa.prcontrol.service.integration.model.EmailResult;

import java.util.UUID;

public class MockEmailProvider implements EmailProvider {
    @Override
    public EmailResult sendEmail(EmailRequest request) {
        return new EmailResult("SENT", "mock-" + UUID.randomUUID());
    }
}
