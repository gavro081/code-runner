package com.github.gavro081.codeexecutionservice.listeners;

import com.github.gavro081.codeexecutionservice.service.DemoService;
import com.github.gavro081.common.config.RabbitMQConfig;
import com.github.gavro081.common.events.JobCreatedEvent;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RabbitListener(queues = RabbitMQConfig.WORKER_QUEUE)
public class JobEventListener {
    DemoService demoService;

    public JobEventListener(DemoService demoService) {
        this.demoService = demoService;
    }

    @RabbitHandler
    public void handleJobCreatedEvent(JobCreatedEvent event){
        demoService.foo(event);
    }
}
