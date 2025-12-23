package ai.journa.prcontrol.service.integration;

public class ProviderException extends RuntimeException {
  private final int statusCode;
  private final boolean retryable;

  public ProviderException(String message, int statusCode, boolean retryable) {
    super(message);
    this.statusCode = statusCode;
    this.retryable = retryable;
  }

  public ProviderException(String message, Throwable cause, int statusCode, boolean retryable) {
    super(message, cause);
    this.statusCode = statusCode;
    this.retryable = retryable;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public boolean isRetryable() {
    return retryable;
  }
}
