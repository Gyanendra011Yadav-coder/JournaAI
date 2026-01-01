package ai.journa.prcontrol.service.llm;

public class LlmCallException extends RuntimeException {
  private final Integer statusCode;
  private final boolean retryable;

  public LlmCallException(String message, Integer statusCode, boolean retryable, Throwable cause) {
    super(message, cause);
    this.statusCode = statusCode;
    this.retryable = retryable;
  }

  public Integer getStatusCode() {
    return statusCode;
  }

  public boolean isRetryable() {
    return retryable;
  }
}
