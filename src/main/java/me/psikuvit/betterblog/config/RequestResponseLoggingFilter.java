package me.psikuvit.betterblog.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
// ...existing imports...
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

@Component
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);
    private static final int MAX_PAYLOAD_LENGTH = 1024;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // buffer up to 10KB of request payload for logging
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, 10 * 1024);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long duration = System.currentTimeMillis() - start;
            logRequestAndResponse(wrappedRequest, wrappedResponse, duration);
            wrappedResponse.copyBodyToResponse();
        }
    }

    private void logRequestAndResponse(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response, long duration) {
        StringBuilder msg = new StringBuilder();
        msg.append(request.getMethod()).append(' ').append(request.getRequestURI());
        if (request.getQueryString() != null) {
            msg.append('?').append(request.getQueryString());
        }
        msg.append(" from ").append(request.getRemoteAddr());
        msg.append(" took ").append(duration).append("ms");

        // Headers (excluding Authorization)
        msg.append(" headers=[");
        Enumeration<String> headerNames = request.getHeaderNames();
        boolean first = true;
        while (headerNames != null && headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            if ("authorization".equalsIgnoreCase(name) || "cookie".equalsIgnoreCase(name)) continue;
            if (!first) msg.append(',');
            msg.append(name).append(':').append(request.getHeader(name));
            first = false;
        }
        msg.append(']');

        // Request body (truncated) - redact sensitive endpoints
        boolean redactRequestBody = request.getRequestURI().contains("/auth/login")
                || request.getRequestURI().contains("/auth/reset-password")
                || request.getRequestURI().contains("/auth/register");

        byte[] buf = request.getContentAsByteArray();
        if (buf.length > 0) {
            if (redactRequestBody) {
                msg.append(" requestBody=[REDACTED]");
            } else {
                String payload = new String(buf, 0, Math.min(buf.length, MAX_PAYLOAD_LENGTH), StandardCharsets.UTF_8);
                msg.append(" requestBody=").append(payload);
                if (buf.length > MAX_PAYLOAD_LENGTH) msg.append("...[truncated]");
            }
        }

        // Response status and body (truncated)
        msg.append(" responseStatus=").append(response.getStatus());
        // Redact response bodies for auth endpoints that may contain tokens
        boolean redactResponseBody = request.getRequestURI().contains("/auth/login")
                || request.getRequestURI().contains("/auth/refresh");
        byte[] respBuf = response.getContentAsByteArray();
        if (respBuf.length > 0) {
            if (redactResponseBody) {
                msg.append(" responseBody=[REDACTED]");
            } else {
                String respPayload = new String(respBuf, 0, Math.min(respBuf.length, MAX_PAYLOAD_LENGTH), StandardCharsets.UTF_8);
                msg.append(" responseBody=").append(respPayload);
                if (respBuf.length > MAX_PAYLOAD_LENGTH) msg.append("...[truncated]");
            }
        }

        log.info(msg.toString());
    }
}



