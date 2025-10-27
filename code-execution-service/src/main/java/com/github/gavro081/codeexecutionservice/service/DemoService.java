package com.github.gavro081.codeexecutionservice.service;

import com.github.gavro081.common.events.JobCreatedEvent;
import org.springframework.stereotype.Service;

@Service
public class DemoService {
    public void foo(JobCreatedEvent event){
        System.out.println("received event: " + event.toString());
    }
}
