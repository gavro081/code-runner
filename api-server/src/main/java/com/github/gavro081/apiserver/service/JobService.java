package com.github.gavro081.apiserver.service;

import com.github.gavro081.apiserver.dto.CodeSubmissionDto;
import com.github.gavro081.apiserver.dto.JobStatusDto;
import com.github.gavro081.apiserver.exceptions.JobNotFoundException;
import com.github.gavro081.apiserver.repository.JobRepository;
import com.github.gavro081.apiserver.repository.JobStatusEventStore;
import com.github.gavro081.common.events.JobCreatedEvent;
import com.github.gavro081.common.events.JobStatusEvent;
import com.github.gavro081.common.model.Job;
import com.github.gavro081.common.model.JobStatus;
import com.github.gavro081.common.model.ProgrammingLanguage;
import jakarta.validation.constraints.NotNull;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static com.github.gavro081.common.config.RabbitMQConstants.EXCHANGE_NAME;
import static com.github.gavro081.common.config.RabbitMQConstants.JOB_CREATED_ROUTING_KEY;

@Service
public class JobService {
    private final JobRepository jobRepository;
    private final RabbitTemplate rabbitTemplate;
    private final String instanceId;

    public JobService(JobRepository jobRepository, RabbitTemplate rabbitTemplate, String instanceId) {
        this.jobRepository = jobRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.instanceId = instanceId;
    }

    public UUID createJob(CodeSubmissionDto codeSubmissionDto){
        ProgrammingLanguage language = codeSubmissionDto.language();
        String code = codeSubmissionDto.code();
        Job job = Job.builder()
                .id(UUID.randomUUID())
                .language(language)
                .code(code)
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
        JobStatus jobStatus;
        String stderr;
        String stdout;
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException(String.format("Job %s not found", jobId)));
        jobStatus = job.getStatus();
        stderr = job.getStderr();
        stdout = job.getStdout();
        System.out.printf("Serving response for job %s from db%n", jobId);

        return JobStatusDto.builder()
                .jobStatus(jobStatus)
                .stderr(stderr)
                .stdout(stdout)
                .build();
    }
}
