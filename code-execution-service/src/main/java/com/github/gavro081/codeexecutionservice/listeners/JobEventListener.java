package com.github.gavro081.codeexecutionservice.listeners;

import com.github.gavro081.codeexecutionservice.models.ExecutionResult;
import com.github.gavro081.codeexecutionservice.service.CodeExecutionService;
import com.github.gavro081.codeexecutionservice.service.JobHandlerService;
import com.github.gavro081.common.config.RabbitMQConstants;
import com.github.gavro081.common.events.JobCreatedEvent;
import com.github.gavro081.common.model.Job;
import com.github.gavro081.common.model.JobStatus;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RabbitListener(queues = RabbitMQConstants.WORKER_QUEUE, containerFactory = "jobListenerFactory")
public class JobEventListener {
    private final JobHandlerService jobHandlerService;
    private final CodeExecutionService codeExecutionService;

    public JobEventListener(JobHandlerService jobHandlerService, CodeExecutionService codeExecutionService) {
        this.jobHandlerService = jobHandlerService;
        this.codeExecutionService = codeExecutionService;
    }

    @RabbitHandler
    public void handleJobCreatedEvent(JobCreatedEvent event) {
        Job job = jobHandlerService.markAndGetJob(event.jobId());
        if (job == null) {
            // job was already picked up todo: add logging
            return;
        }

        ExecutionResult result;
        JobStatus finalStatus;
        try {
            result = codeExecutionService.execute(job.getCode(), job.getLanguage());
            finalStatus = result.isSuccess() ? JobStatus.COMPLETED : JobStatus.FAILED;
        } catch (Exception e) {
            System.out.printf("error - jobId: %s - errMsg: %s", job.getId(), e.getMessage());
            result = ExecutionResult.builder()
                    .stderr("Error running job: " + e.getMessage())
                    .exitCode(1)
                    .build();
            finalStatus = JobStatus.FAILED;
        }

        try {
            jobHandlerService.finalizeJob(
                    job.getId(),
                    finalStatus,
                    result.getStdout(),
                    result.getStderr()
            );
            System.out.println("success");
        } catch (Exception e){
            // todo: log
            System.out.println("failed to finalize job: " + e.getMessage());
            throw e; // rethrow error so rabbitmq can re-queue it
        }
    }
}
