package ai.journa.prcontrol.service.llm;

public record LlmResponse(String providerName,
                          String model,
                          String content,
                          long durationMs) {
}
