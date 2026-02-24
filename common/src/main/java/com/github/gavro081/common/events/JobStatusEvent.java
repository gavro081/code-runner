package com.github.gavro081.common.events;

import com.github.gavro081.common.model.enums.JobStatus;
import lombok.Builder;

import java.util.UUID;

@Builder
public record JobStatusEvent(
        UUID jobId,
        JobStatus status,
        String stdout,
        String stderr
) {}
