package com.github.gavro081.codeexecutionservice.listeners;

import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.github.gavro081.codeexecutionservice.models.ExecutionResult;
import com.github.gavro081.codeexecutionservice.service.CodeExecutionService;
import com.github.gavro081.codeexecutionservice.service.JobHandlerService;
import com.github.gavro081.common.config.RabbitMQConstants;
import com.github.gavro081.common.events.JobCreatedEvent;
import com.github.gavro081.common.model.JobStatus;

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
    public void handleJobCreatedEvent(JobCreatedEvent job) {
        ExecutionResult result;
        JobStatus finalStatus;
        try {
            result = codeExecutionService.execute(job);
            finalStatus = result.isSuccess() ? JobStatus.COMPLETED : JobStatus.FAILED;
        } catch (Exception e) {
            System.out.printf("error - jobId: %s - errMsg: %s", job.jobId(), e.getMessage());
            result = ExecutionResult.builder()
                    .stderr("Error running job: " + e.getMessage())
                    .exitCode(1)
                    .build();
            finalStatus = JobStatus.FAILED;
        }

        try {
            jobHandlerService.finalizeJob(
                    job,
                    finalStatus,
                    result.getStdout(),
                    result.getStderr()
            );
        } catch (Exception e){
            System.out.println("failed to finalize job: " + e.getMessage());
            throw e; // rethrow error so rabbitmq can re-queue it
        }
    }
}
