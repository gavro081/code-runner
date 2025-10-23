package com.github.gavro081.apiserver.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "jobs")
@Getter
@Setter
@Builder
@EqualsAndHashCode(of = "id")
@AllArgsConstructor
@NoArgsConstructor
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Status is required")
    private JobStatus status;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Programming language is required")
    private ProgrammingLanguage language;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String code;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String stdin;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String stdout;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String stderr;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

}
