package ai.journa.prcontrol.service.summarizer;

import org.springframework.stereotype.Component;

@Component
public class RuleBasedSummarizer implements Summarizer {
    @Override
    public String summarize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String[] sentences = text.split("(?<=[.!?])\\s+");
        if (sentences.length == 0) {
            return text;
        }
        int count = Math.min(2, sentences.length);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                builder.append(" ");
            }
            builder.append(sentences[i]);
        }
        return builder.toString().trim();
    }
}
