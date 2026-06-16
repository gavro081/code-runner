package com.github.gavro081.codeexecutionservice.service;

import com.github.gavro081.codeexecutionservice.repository.JobHandlerRepository;
import com.github.gavro081.common.events.JobCreatedEvent;
import com.github.gavro081.common.events.JobStatusEvent;
import com.github.gavro081.common.model.Job;
import com.github.gavro081.common.model.enums.JobStatus;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

import static com.github.gavro081.common.config.RabbitMQConstants.EXCHANGE_NAME;


@Service
public class JobHandlerService {
    private final JobHandlerRepository repository;
    private final RabbitTemplate rabbitTemplate;

    public JobHandlerService(JobHandlerRepository repository, RabbitTemplate rabbitTemplate) {
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
    }

    public void finalizeJob(JobCreatedEvent job, JobStatus newStatus, String stdout, String stderr){
        Optional<Job> jobOptional = repository.findById(job.jobId());

        if (jobOptional.isEmpty()){
            // maybe i should throw an exception
            repository.save(Job.builder()
                    .id(job.jobId())
                    .code(job.code())
                    .language(job.language())
                    .status(newStatus)
                    .stderr(stderr)
                    .stdout(stdout)
                    .completedAt(Instant.now())
                    .build()
            );
        } else {
            Job jobRecord = jobOptional.get();
            jobRecord.setStatus(newStatus);
            jobRecord.setStdout(stdout);
            jobRecord.setStderr(stderr);
            jobRecord.setCompletedAt(Instant.now());
            repository.save(jobRecord);
        }


        // create and publish event
        JobStatusEvent event = JobStatusEvent.builder()
                .jobId(job.jobId())
                .status(newStatus)
                .stderr(stderr)
                .stdout(stdout)
                .build();

        rabbitTemplate.convertAndSend(EXCHANGE_NAME, job.serverId(), event);
    }
}
