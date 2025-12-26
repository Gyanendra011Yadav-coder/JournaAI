package ai.journa.prcontrol.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.List;
import java.util.UUID;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {
  private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);
  private final RequestLoggingProperties properties;

  public RequestLoggingFilter(RequestLoggingProperties properties) {
    this.properties = properties;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws java.io.IOException, jakarta.servlet.ServletException {
    String requestId = UUID.randomUUID().toString();
    long start = System.currentTimeMillis();
    String path = request.getRequestURI();
    boolean enabled = properties.isEnabled();
    boolean include =
        properties.getIncludePaths().isEmpty() || matches(properties.getIncludePaths(), path);
    boolean exclude = matches(properties.getExcludePaths(), path);
    boolean shouldLog = enabled && include && !exclude;
    try {
      response.setHeader("X-Request-Id", requestId);
      if (shouldLog && properties.isLogStart()) {
        logger.info("API start id={} method={} path={} query={}",
            requestId,
            request.getMethod(),
            request.getRequestURI(),
            request.getQueryString());
      }
      filterChain.doFilter(request, response);
    } finally {
      long durationMs = System.currentTimeMillis() - start;
      if (enabled) {
        boolean slow = properties.getSlowThresholdMs() > 0
            && durationMs >= properties.getSlowThresholdMs();
        boolean error = response.getStatus() >= 400;
        if ((shouldLog && properties.isLogSuccess()) || slow || error) {
          Authentication auth = SecurityContextHolder.getContext().getAuthentication();
          String principal = auth != null ? auth.getName() : "anonymous";
          logger.info("API end id={} status={} durationMs={} user={}",
              requestId,
              response.getStatus(),
              durationMs,
              principal);
        }
      }
    }
  }

  private boolean matches(List<String> prefixes, String path) {
    if (path == null || prefixes == null) {
      return false;
    }
    for (String prefix : prefixes) {
      if (prefix != null && !prefix.isBlank() && path.startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }
}
