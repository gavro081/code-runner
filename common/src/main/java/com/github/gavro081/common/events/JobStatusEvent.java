package com.github.gavro081.common.events;

import com.github.gavro081.common.model.JobStatus;
import lombok.Builder;
import lombok.Setter;

import java.util.UUID;

@Builder
public record JobStatusEvent(
        UUID jobId,
        JobStatus status,
        String stdout,
        String stderr
) {}
