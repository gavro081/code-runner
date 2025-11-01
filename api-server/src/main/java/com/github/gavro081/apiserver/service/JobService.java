package com.github.gavro081.apiserver.service;

import com.github.gavro081.apiserver.dto.CodeSubmissionDto;
import com.github.gavro081.apiserver.dto.JobStatusDto;
import com.github.gavro081.apiserver.exceptions.JobNotFoundException;
import com.github.gavro081.apiserver.repository.JobRepository;
import com.github.gavro081.common.config.RabbitMQConfig;
import com.github.gavro081.common.events.JobCreatedEvent;
import com.github.gavro081.common.model.Job;
import com.github.gavro081.common.model.JobStatus;
import com.github.gavro081.common.model.ProgrammingLanguage;
import jakarta.validation.constraints.NotNull;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class JobService {
    private final JobRepository jobRepository;
    private final RabbitTemplate rabbitTemplate;

    public JobService(JobRepository jobRepository, RabbitTemplate rabbitTemplate) {
        this.jobRepository = jobRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    public UUID createJob(CodeSubmissionDto codeSubmissionDto){
        ProgrammingLanguage language = codeSubmissionDto.language();
        String code = codeSubmissionDto.code();
        Job job = Job.builder()
                .language(language)
                .code(code)
                .status(JobStatus.PENDING)
                .build();

        Job savedJob = jobRepository.save(job);

        JobCreatedEvent event = JobCreatedEvent.builder()
                .jobId(savedJob.getId())
                .timestamp(Instant.now())
                .build();

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, "job.created", event);
        return savedJob.getId();
    }

    public JobStatusDto getJobStatus(@NotNull UUID jobId) throws JobNotFoundException{
        Job job = jobRepository
                .findById(jobId)
                .orElseThrow(() -> new JobNotFoundException(String.format("Job %s not found", jobId)));
        return JobStatusDto.builder()
                .jobStatus(job.getStatus())
                .stderr(job.getStderr())
                .stdout(job.getStdout())
                .build();
    }
}
