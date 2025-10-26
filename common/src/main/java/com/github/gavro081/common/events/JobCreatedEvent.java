package com.github.gavro081.common.events;

import com.github.gavro081.common.model.ProgrammingLanguage;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor
@Builder
@Getter @Setter
public class JobCreatedEvent implements Serializable {
    private final UUID jobId = UUID.randomUUID();
    private final Instant timestamp = Instant.now();

    private String code;
    private ProgrammingLanguage language;
}