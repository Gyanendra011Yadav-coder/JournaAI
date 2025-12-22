package ai.journa.prcontrol.service.integration.model;

public class EmailResult {
    private String status;
    private String providerMessageId;

    public EmailResult(String status, String providerMessageId) {
        this.status = status;
        this.providerMessageId = providerMessageId;
    }

    public String getStatus() {
        return status;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }
}
