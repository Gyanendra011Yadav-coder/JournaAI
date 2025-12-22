package ai.journa.prcontrol.service.integration;

import ai.journa.prcontrol.service.integration.model.EmailRequest;
import ai.journa.prcontrol.service.integration.model.EmailResult;

public interface EmailProvider {
    EmailResult sendEmail(EmailRequest request);
}
