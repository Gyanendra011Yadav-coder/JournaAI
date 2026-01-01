package ai.journa.prcontrol.service.llm;

import ai.journa.prcontrol.domain.LlmProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class LlmTemplateRenderer {
  private final ObjectMapper objectMapper;

  public LlmTemplateRenderer(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public String renderBaseUrl(String baseUrl, LlmProvider provider) {
    if (baseUrl == null) {
      return null;
    }
    return replaceTokens(baseUrl, provider, null);
  }

  public Object renderTemplate(Object template, LlmProvider provider, LlmRequest request) {
    return renderValue(template, provider, request);
  }

  private Object renderValue(Object value, LlmProvider provider, LlmRequest request) {
    if (value == null) {
      return null;
    }
    if (value instanceof Map<?, ?> mapValue) {
      List<Object> numericList = renderNumericList(mapValue, provider, request);
      if (numericList != null) {
        return numericList;
      }
      Map<String, Object> rendered = new LinkedHashMap<>();
      for (Map.Entry<?, ?> entry : mapValue.entrySet()) {
        String key = String.valueOf(entry.getKey());
        rendered.put(key, renderValue(entry.getValue(), provider, request));
      }
      return rendered;
    }
    if (value instanceof List<?> listValue) {
      List<Object> rendered = new ArrayList<>(listValue.size());
      for (Object item : listValue) {
        rendered.add(renderValue(item, provider, request));
      }
      return rendered;
    }
    if (value instanceof String stringValue) {
      return renderString(stringValue, provider, request);
    }
    return value;
  }

  private Object renderString(String value, LlmProvider provider, LlmRequest request) {
    if (value == null) {
      return null;
    }
    if (request != null && "{{MAX_TOKENS}}".equals(value)) {
      return request.maxTokens();
    }
    if (request != null && "{{TEMPERATURE}}".equals(value)) {
      return request.temperature();
    }
    String rendered = replaceTokens(value, provider, request);
    return rendered;
  }

  private List<Object> renderNumericList(Map<?, ?> mapValue, LlmProvider provider, LlmRequest request) {
    if (mapValue.isEmpty()) {
      return null;
    }
    int maxIndex = -1;
    for (Object key : mapValue.keySet()) {
      if (!(key instanceof String stringKey)) {
        return null;
      }
      try {
        int index = Integer.parseInt(stringKey);
        if (index < 0) {
          return null;
        }
        maxIndex = Math.max(maxIndex, index);
      } catch (NumberFormatException ex) {
        return null;
      }
    }
    if (maxIndex + 1 != mapValue.size()) {
      return null;
    }
    List<Object> rendered = new ArrayList<>(mapValue.size());
    for (int index = 0; index <= maxIndex; index++) {
      Object item = mapValue.get(String.valueOf(index));
      if (!mapValue.containsKey(String.valueOf(index))) {
        return null;
      }
      rendered.add(renderValue(item, provider, request));
    }
    return rendered;
  }

  private String replaceTokens(String value, LlmProvider provider, LlmRequest request) {
    String rendered = value;
    if (provider != null) {
      rendered = rendered.replace("{{MODEL}}", safe(provider.getModel()));
    }
    if (request != null) {
      rendered = rendered.replace("{{SYSTEM_PROMPT}}", escapeJson(safe(request.systemPrompt())));
      rendered = rendered.replace("{{USER_PROMPT}}", escapeJson(safe(request.userPrompt())));
      rendered = rendered.replace("{{MAX_TOKENS}}", String.valueOf(request.maxTokens()));
      rendered = rendered.replace("{{TEMPERATURE}}", String.valueOf(request.temperature()));
    }
    return rendered;
  }

  private String safe(String value) {
    return value == null ? "" : value;
  }

  private String escapeJson(String value) {
    try {
      String quoted = objectMapper.writeValueAsString(value);
      if (quoted.length() >= 2) {
        return quoted.substring(1, quoted.length() - 1);
      }
      return value;
    } catch (Exception ex) {
      return value;
    }
  }
}
