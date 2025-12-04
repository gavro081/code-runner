package com.github.gavro081.codeexecutionservice.service;

import com.github.gavro081.codeexecutionservice.repository.JobHandlerRepository;
import com.github.gavro081.common.events.JobCreatedEvent;
import com.github.gavro081.common.events.JobStatusEvent;
import com.github.gavro081.common.model.Job;
import com.github.gavro081.common.model.JobStatus;
import jakarta.transaction.Transactional;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

import static com.github.gavro081.common.config.RabbitMQConstants.EXCHANGE_NAME;
import static com.github.gavro081.common.config.RabbitMQConstants.JOB_FINISHED_ROUTING_KEY;


@Service
public class JobHandlerService {
    private final JobHandlerRepository repository;
    private final RabbitTemplate rabbitTemplate;

    public JobHandlerService(JobHandlerRepository repository, RabbitTemplate rabbitTemplate) {
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
    }

//    deprecated
//    @Transactional
//    public Job markAndGetJob(UUID uuid) {
//        Job job = repository.findAndLockBId(uuid)
//                .orElseThrow(() -> new JobNotFoundException("Job not found: " + uuid));
//        if (job.getStatus() != JobStatus.PENDING){
//            // other worker already picked up job
//            return null;
//        }
//        job.setStatus(JobStatus.RUNNING);
//        return repository.save(job);
//    }

    @Transactional
    public void finalizeJob(JobCreatedEvent job, JobStatus newStatus, String stdout, String stderr){
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

        // create and publish event
        JobStatusEvent event = JobStatusEvent.builder()
                .jobId(job.jobId())
                .status(newStatus)
                .stderr(stderr)
                .stdout(stdout)
                .build();

        rabbitTemplate.convertAndSend(EXCHANGE_NAME, JOB_FINISHED_ROUTING_KEY, event);
    }
}
