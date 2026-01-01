package ai.journa.prcontrol.service.llm;

public record LlmRequest(String systemPrompt,
                         String userPrompt,
                         double temperature,
                         int maxTokens) {
}
