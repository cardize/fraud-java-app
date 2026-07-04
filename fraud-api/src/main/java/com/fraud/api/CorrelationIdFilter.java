package com.fraud.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Correlation id for end-to-end log tracing.
 *
 * Accepts the caller's X-Correlation-Id header (or generates a UUID when absent/invalid), puts it
 * into the SLF4J MDC and echoes it in the response. The logging pattern (application.yml) prints
 * it on every line, so one request's logs can be grepped out of concurrent traffic — and a caller
 * reporting an error can quote the id from the response header.
 *
 * Runs FIRST (highest precedence — before the tenant filter and the security chain) so even
 * rejected requests get a correlation id in their logs.
 *
 * The header value is validated against a strict charset before being echoed back — an arbitrary
 * reflected header would otherwise be a header-injection surface.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String cid = request.getHeader(HEADER);
        if (cid == null || !cid.matches("[A-Za-z0-9_-]{1,64}")) {
            cid = UUID.randomUUID().toString();
        }
        MDC.put(MDC_KEY, cid);
        response.setHeader(HEADER, cid);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY); // the thread goes back to the pool — a stale id must not leak into the next request
        }
    }
}
