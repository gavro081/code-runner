package com.github.gavro081.apiserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class InstanceConfig {

    @Bean
    public String instanceId(){
        return UUID.randomUUID().toString();
    }
}
