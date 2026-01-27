package com.alpeerkaraca.common.event;

import java.time.Instant;

public interface DomainEvent {
    String getEventId();

    String getAggregateId();

    String getEventType();

    Instant getCreatedAt();

    String getTraceId();

    String getUserId();
}
