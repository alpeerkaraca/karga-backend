package com.alpeerkaraca.common.model;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;

import java.time.Instant;

@MappedSuperclass
@Data
public class BaseInboxEntity {
    @Id
    private String messageId;

    private Instant processedAt = Instant.now();

    private InboxStatus status;

    private String eventType;
}
