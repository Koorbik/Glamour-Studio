package com.hszadkowski.iwa_backend.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Enumeration;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class DebugFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String uri = httpRequest.getRequestURI();

        // Only log for PayU webhook
        if (uri.contains("/api/payments/notify")) {
            log.info("=== DEBUG FILTER START ===");
            log.info("Method: {}", httpRequest.getMethod());
            log.info("URI: {}", uri);
            log.info("Remote Address: {}", httpRequest.getRemoteAddr());

            log.info("Headers:");
            Enumeration<String> headerNames = httpRequest.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                log.info("  {}: {}", headerName, httpRequest.getHeader(headerName));
            }

            log.info("=== DEBUG FILTER - Proceeding to next filter ===");
        }

        chain.doFilter(request, response);

        if (uri.contains("/api/payments/notify")) {
            log.info("=== DEBUG FILTER END ===");
            log.info("Response Status: {}", httpResponse.getStatus());
        }
    }
}