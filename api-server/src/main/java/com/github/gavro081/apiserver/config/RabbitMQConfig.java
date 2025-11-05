package com.github.gavro081.apiserver.config;

import com.github.gavro081.common.events.JobCreatedEvent;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

import static com.github.gavro081.common.config.RabbitMQConstants.EXCHANGE_NAME;

@Configuration
public class RabbitMQConfig {
    @Bean
    TopicExchange exchange(){
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        DefaultClassMapper classMapper = new DefaultClassMapper();

        Map<String, Class<?>> idClassMapping = new HashMap<>();
        idClassMapping.put("com.github.gavro081.common.events.JobCreatedEvent", JobCreatedEvent.class);

        classMapper.setIdClassMapping(idClassMapping);
        converter.setClassMapper(classMapper);
        return converter;
    }
}
