package com.github.gavro081.codeexecutionservice.listeners;

import com.github.gavro081.codeexecutionservice.components.CpuMonitor;
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
    private static final double CPU_LOAD_THRESHOLD = 0.9;
    private static final long SLEEP_MS = 1_000;

    private final JobHandlerService jobHandlerService;
    private final CodeExecutionService codeExecutionService;
    private final CpuMonitor cpuMonitor;

    public JobEventListener(JobHandlerService jobHandlerService, CodeExecutionService codeExecutionService, CpuMonitor cpuMonitor) {
        this.jobHandlerService = jobHandlerService;
        this.codeExecutionService = codeExecutionService;
        this.cpuMonitor = cpuMonitor;
    }

    @RabbitHandler
    public void handleJobCreatedEvent(JobCreatedEvent job) {
        System.out.printf(
                "%s running job: %s%n",
                Thread.currentThread().getName(), job
                );
        double load = cpuMonitor.getSystemCpuLoad();
        if (load >= CPU_LOAD_THRESHOLD){
            try {
                System.out.println(String.format("THREAD %s:CPU Load is: %f, threshold reached -> sleeping for %d ms.",
                        Thread.currentThread().getName(), load, SLEEP_MS));
                Thread.sleep(SLEEP_MS);
            } catch (InterruptedException e){
                Thread.currentThread().interrupt();
            }
        }
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
