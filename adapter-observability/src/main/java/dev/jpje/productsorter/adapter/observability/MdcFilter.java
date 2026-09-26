package dev.jpje.productsorter.adapter.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

import io.opentelemetry.api.trace.Span;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class MdcFilter extends OncePerRequestFilter {

  private static final String TRACE_ID = "traceId";
  private static final String REQUEST_URI = "requestUri";
  private static final String REQUEST_ID = "requestId";
  private static final String X_REQUEST_ID = "X-Request-Id";

  @Override
  protected void doFilterInternal(final @NonNull HttpServletRequest request,
                                   final @NonNull HttpServletResponse response,
                                   final @NonNull FilterChain chain) throws IOException, ServletException {
    try {
      MDC.put(TRACE_ID, resolveTraceId());
      MDC.put(REQUEST_URI, request.getRequestURI());
      putRequestId(request);
      chain.doFilter(request, response);
    } finally {
      MDC.clear();
    }
  }

  private static String resolveTraceId() {
    final var spanContext = Span.current().getSpanContext();
    if (spanContext.isValid()) {
      return spanContext.getTraceId();
    }
    final var existing = MDC.get(TRACE_ID);
    return existing != null ? existing : UUID.randomUUID().toString().substring(0, 6);
  }

  private static void putRequestId(final HttpServletRequest request) {
    final var header = request.getHeader(X_REQUEST_ID);
    if (header != null && !header.isBlank()) {
      MDC.put(REQUEST_ID, header);
    }
  }
}
