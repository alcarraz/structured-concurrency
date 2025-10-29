package com.example.scopedvalues;

import java.time.LocalDateTime;

public record RequestContext(
    String sessionId,
    String userAgent,
    String clientIp,
    LocalDateTime timestamp,
    String correlationId
) {
    public static RequestContext create(String sessionId, String userAgent, String clientIp) {
        return new RequestContext(
            sessionId,
            userAgent,
            clientIp,
            LocalDateTime.now(),
            java.util.UUID.randomUUID().toString().substring(0, 8)
        );
    }
}