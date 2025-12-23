package com.github.gavro081.apiserver.listeners;

import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.github.gavro081.apiserver.service.JobService;
import com.github.gavro081.common.events.JobStatusEvent;

@Component
@RabbitListener(queues = "#{serverQueue.name}")
public class JobEventListener {
//    private final JobService jobService;

//    public JobEventListener(JobService jobService) {
//        this.jobService = jobService;
//    }

    @RabbitHandler
    public void handleJobStatusEvent(JobStatusEvent job){
        System.out.printf("Received status for %s%n", job);
//        jobService.updateJob(job);
    }
}
