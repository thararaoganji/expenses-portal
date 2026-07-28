package portal.expenses.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.stream.Collectors;

@Component
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();

        // Log request
        logRequest(requestWrapper);

        // Process request
        filterChain.doFilter(requestWrapper, responseWrapper);

        long duration = System.currentTimeMillis() - startTime;

        // Log response
        logResponse(responseWrapper, duration);

        // Copy response body back to original response
        responseWrapper.copyBodyToResponse();
    }

    private void logRequest(ContentCachingRequestWrapper request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        String queryString = request.getQueryString();
        String headers = Collections.list(request.getHeaderNames())
                .stream()
                .map(headerName -> headerName + ": " + request.getHeader(headerName))
                .collect(Collectors.joining(", "));

        log.info("==> Incoming Request: {} {} {}",
                   method,
                   uri,
                   queryString != null ? "?" + queryString : "");
        log.debug("Request Headers: {}", headers);

        // Log request body for non-multipart requests
        String contentType = request.getContentType();
        if (contentType != null && !contentType.contains("multipart/form-data")) {
            byte[] content = request.getContentAsByteArray();
            if (content.length > 0) {
                String body = new String(content, StandardCharsets.UTF_8);
                log.debug("Request Body: {}", body);
            }
        } else if (contentType != null && contentType.contains("multipart/form-data")) {
            log.debug("Request Body: [multipart/form-data - not logged]");
        }
    }

    private void logResponse(ContentCachingResponseWrapper response, long duration) {
        int status = response.getStatus();

        log.info("<== Response: {} ({}ms)", status, duration);

        // Log response body
        byte[] content = response.getContentAsByteArray();
        if (content.length > 0) {
            String body = new String(content, StandardCharsets.UTF_8);
            // Limit response body logging to 1000 characters
            if (log.isDebugEnabled()) {
                if (body.length() > 1000) {
                    log.debug("Response Body: {}... [truncated]", body.substring(0, 1000));
                } else {
                    log.debug("Response Body: {}", body);
                }
            }
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Don't log actuator endpoints
        return path.startsWith("/actuator");
    }
}
