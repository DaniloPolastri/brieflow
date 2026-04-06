package com.briefflow.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (!isRateLimited(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = request.getRemoteAddr();
        Bucket bucket = buckets.computeIfAbsent(ip, k -> createBucket());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"timestamp\":\"" + java.time.LocalDateTime.now() +
                    "\",\"status\":429,\"error\":\"Too Many Requests\"," +
                    "\"message\":\"Limite de requisicoes excedido. Tente novamente em 1 minuto.\"}");
        }
    }

    private boolean isRateLimited(String path) {
        return path.startsWith("/api/v1/auth/login") || path.startsWith("/api/v1/auth/register");
    }

    private Bucket createBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.simple(5, Duration.ofMinutes(1)))
                .build();
    }
}
