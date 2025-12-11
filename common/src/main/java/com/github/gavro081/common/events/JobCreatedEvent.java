package com.github.gavro081.common.events;

import com.github.gavro081.common.model.ProgrammingLanguage;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;


@Builder
public record JobCreatedEvent(
        UUID jobId,
        String problemId,
        Instant timestamp,
        ProgrammingLanguage language,
        String code,
        String serverId
) implements Serializable {

    @Override
    public String toString() {
        return "JobCreatedEvent{" +
                "jobId=" + jobId +
                ", timestamp=" + timestamp +
                '}';
    }
}