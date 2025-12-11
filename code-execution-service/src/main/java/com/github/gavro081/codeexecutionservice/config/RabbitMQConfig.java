package com.github.gavro081.codeexecutionservice.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.github.gavro081.common.config.RabbitMQConstants.EXCHANGE_NAME;
import static com.github.gavro081.common.config.RabbitMQConstants.JOB_CREATED_ROUTING_KEY;
import static com.github.gavro081.common.config.RabbitMQConstants.WORKER_QUEUE;
import com.github.gavro081.common.events.JobCreatedEvent;
import com.github.gavro081.common.events.JobStatusEvent;

@Configuration
public class RabbitMQConfig {
    @Bean
    TopicExchange exchange(){
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    Queue workerQueue(){
        return new Queue(WORKER_QUEUE, true);
    }

    @Bean
    Binding workersBinding(Queue workerQueue, TopicExchange exchange){
        return BindingBuilder
                .bind(workerQueue)
                .to(exchange)
                .with(JOB_CREATED_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        DefaultClassMapper classMapper = new DefaultClassMapper();

        Map<String, Class<?>> idClassMapping = new HashMap<>();
        idClassMapping.put("com.github.gavro081.common.events.JobCreatedEvent", JobCreatedEvent.class);
        idClassMapping.put("com.github.gavro081.common.events.JobStatusEvent", JobStatusEvent.class);

        classMapper.setIdClassMapping(idClassMapping);
        converter.setClassMapper(classMapper);
        return converter;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory jobListenerFactory(
            ConnectionFactory connectionFactory,
            SimpleRabbitListenerContainerFactoryConfigurer configurer
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);

        factory.setConcurrentConsumers(4);
        factory.setMaxConcurrentConsumers(10);
        // wait 1s before starting a new consumer-worker
        factory.setStartConsumerMinInterval(1000L);
        // consumer should be idle for 10 s before stopping
        factory.setStopConsumerMinInterval(10000L);
        factory.setPrefetchCount(1);
        return factory;
    }
}


