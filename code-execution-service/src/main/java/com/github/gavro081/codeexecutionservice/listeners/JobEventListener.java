package com.github.gavro081.codeexecutionservice.listeners;

import com.github.gavro081.codeexecutionservice.service.JobHandlerService;
import com.github.gavro081.common.config.RabbitMQConfig;
import com.github.gavro081.common.events.JobCreatedEvent;
import com.github.gavro081.common.model.Job;
import com.github.gavro081.common.model.JobStatus;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RabbitListener(queues = RabbitMQConfig.WORKER_QUEUE)
public class JobEventListener {
    private final JobHandlerService jobHandlerService;

    public JobEventListener(JobHandlerService jobHandlerService) {
        this.jobHandlerService = jobHandlerService;
    }

    @RabbitHandler
    public void handleJobCreatedEvent(JobCreatedEvent event) {
        Job job = jobHandlerService.markAndGetJob(event.jobId());
        if (job == null) {
            // job was already picked up todo: add logging
            return;
        }
        // todo: pass job to container

        try {
            jobHandlerService.finalizeJob(job.getId(), JobStatus.COMPLETED, "stdout placeholder", "stderr placeholder");
        } catch (Exception e){
            // todo: log
            throw e; // rethrow error so rabbitmq can re-queue it
        }
    }
}
