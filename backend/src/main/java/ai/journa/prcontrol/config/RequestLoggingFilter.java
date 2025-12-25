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

import java.util.UUID;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {
  private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

  @Override
  protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws java.io.IOException, jakarta.servlet.ServletException {
    String requestId = UUID.randomUUID().toString();
    long start = System.currentTimeMillis();
    try {
      response.setHeader("X-Request-Id", requestId);
      logger.info("API start id={} method={} path={} query={}",
          requestId,
          request.getMethod(),
          request.getRequestURI(),
          request.getQueryString());
      filterChain.doFilter(request, response);
    } finally {
      long durationMs = System.currentTimeMillis() - start;
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
