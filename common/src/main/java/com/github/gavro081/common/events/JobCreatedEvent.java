package com.github.gavro081.common.events;

import lombok.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Builder
public record JobCreatedEvent(UUID jobId, Instant timestamp) implements Serializable {
    @Override
    public String toString() {
        return "JobCreatedEvent{" +
                "jobId=" + jobId +
                ", timestamp=" + timestamp +
                '}';
    }
}