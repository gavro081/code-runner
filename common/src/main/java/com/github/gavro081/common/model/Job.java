package com.github.gavro081.common.model;

import com.github.gavro081.common.model.enums.JobStatus;
import com.github.gavro081.common.model.enums.ProgrammingLanguage;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Document(collection = "jobs")
@Getter @Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Job {
    @Id
    private UUID id;

    @NotNull(message = "Status is required")
    private JobStatus status;

    @NotNull(message = "Programming language is required")
    private ProgrammingLanguage language;

    private String code;

    private String stdin;

    private String stdout;

    private String stderr;

    private Instant createdAt;

    private Instant completedAt;

}
