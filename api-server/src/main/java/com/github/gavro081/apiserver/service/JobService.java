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
    private final JobStatusEventStore jobStatusEventStore;
    private final JobRepository jobRepository;
    private final RabbitTemplate rabbitTemplate;
    private final String instanceId;

    public JobService(JobStatusEventStore jobStatusEventStore, JobRepository jobRepository, RabbitTemplate rabbitTemplate, String instanceId) {
        this.jobStatusEventStore = jobStatusEventStore;
        this.jobRepository = jobRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.instanceId = instanceId;
    }

    public UUID createJob(CodeSubmissionDto codeSubmissionDto){
        ProgrammingLanguage language = codeSubmissionDto.language();
        String code = codeSubmissionDto.code();
//        Job job = Job.builder()
//                .language(language)
//                .code(code)
//                .status(JobStatus.PENDING)
//                .build();
//        Job savedJob = jobRepository.save(job);
        UUID jobId = UUID.randomUUID();

        JobStatusEvent job = JobStatusEvent.builder()
                .jobId(jobId)
                .status(JobStatus.PENDING)
                .stdout(null)
                .stderr(null)
                .build();

        jobStatusEventStore.save(job);

        JobCreatedEvent event = JobCreatedEvent.builder()
                .problemId(codeSubmissionDto.problemId())
                .jobId(jobId)
                .timestamp(Instant.now())
                .code(code)
                .language(language)
                .serverId(instanceId)
                .build();

        rabbitTemplate.convertAndSend(EXCHANGE_NAME, JOB_CREATED_ROUTING_KEY, event);
        return jobId;
    }

    public JobStatusDto getJobStatus(@NotNull UUID jobId) throws JobNotFoundException{
        Optional<JobStatusEvent> jobStatusOptional = jobStatusEventStore.findById(jobId);
        JobStatus jobStatus;
        String stderr;
        String stdout;
        if (jobStatusOptional.isPresent()){
            JobStatusEvent jobStatusEvent = jobStatusOptional.get();
            jobStatus = jobStatusEvent.status();
            stderr = jobStatusEvent.stderr();
            stdout = jobStatusEvent.stdout();
            System.out.printf("Serving response for job %s from local state%n", jobId);
        } else {
            Job job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new JobNotFoundException(String.format("Job %s not found", jobId)));
            jobStatus = job.getStatus();
            stderr = job.getStderr();
            stdout = job.getStdout();
            System.out.printf("Serving response for job %s from local state%n", jobId);
        }

        return JobStatusDto.builder()
                .jobStatus(jobStatus)
                .stderr(stderr)
                .stdout(stdout)
                .build();
    }

    public void updateJob(JobStatusEvent jobStatusEvent) {
        jobStatusEventStore.save(jobStatusEvent); // replace previous UUID with new status
    }
}
