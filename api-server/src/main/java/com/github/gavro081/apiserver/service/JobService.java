package com.github.gavro081.apiserver.service;

import com.github.gavro081.apiserver.dto.CodeSubmissionDto;
import com.github.gavro081.apiserver.dto.JobStatusDto;
import com.github.gavro081.apiserver.exceptions.JobNotFoundException;
import com.github.gavro081.apiserver.repository.JobRepository;
import com.github.gavro081.apiserver.repository.JobStatusEventStore;
import com.github.gavro081.common.events.JobCreatedEvent;
import com.github.gavro081.common.events.JobStatusEvent;
import com.github.gavro081.common.model.Job;
import com.github.gavro081.common.model.enums.JobStatus;
import com.github.gavro081.common.model.enums.ProgrammingLanguage;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static com.github.gavro081.common.config.RabbitMQConstants.EXCHANGE_NAME;
import static com.github.gavro081.common.config.RabbitMQConstants.JOB_CREATED_ROUTING_KEY;

@Service
@RequiredArgsConstructor
public class JobService {
    private final JobRepository jobRepository;
    private final JobStatusEventStore jobStatusEventStore;
    private final RabbitTemplate rabbitTemplate;
    private final String instanceId;

    public UUID createJob(CodeSubmissionDto codeSubmissionDto) {
        ProgrammingLanguage language = codeSubmissionDto.language();
        String code = codeSubmissionDto.code();
        Job job = Job.builder()
                .id(UUID.randomUUID())
                .language(language)
                .code(code)
                .createdAt(Instant.now())
                .status(JobStatus.PENDING)
                .build();
        jobRepository.save(job);

        JobCreatedEvent event = JobCreatedEvent.builder()
                .problemId(codeSubmissionDto.problemId())
                .jobId(job.getId())
                .timestamp(Instant.now())
                .code(code)
                .language(language)
                .serverId(instanceId)
                .build();

        rabbitTemplate.convertAndSend(EXCHANGE_NAME, JOB_CREATED_ROUTING_KEY, event);
        return job.getId();
    }

    public JobStatusDto getJobStatus(@NotNull UUID jobId) throws JobNotFoundException{
        // proper logic isn't fully implemented
        // no sticky sessions, no clean up on in memory store, may lead to infinite memory usage
        // but good enough for a demo :)

        JobStatus jobStatus;
        String stderr;
        String stdout;

        Optional<JobStatusEvent> jobOptional = jobStatusEventStore.findById(jobId);

        if (jobOptional.isPresent()){
            JobStatusEvent jobStatusEvent = jobOptional.get();
            jobStatus = jobStatusEvent.status();
            stderr = jobStatusEvent.stderr();
            stdout = jobStatusEvent.stdout();
            System.out.printf("Serving response for job %s from local state%n", jobId);
        } else {
            Job job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, String.format("Job with ID %s not found", jobId)
                            )
                    );
            jobStatus = job.getStatus();
            stderr = job.getStderr();
            stdout = job.getStdout();
            System.out.printf("Serving response for job %s from db%n", jobId);
        }

        return JobStatusDto.builder()
                .jobStatus(jobStatus)
                .stderr(stderr)
                .stdout(stdout)
                .build();
    }

    public void updateJob(JobStatusEvent jobStatusEvent) {
        jobStatusEventStore.save(jobStatusEvent);
    }
}
