package com.github.gavro081.common.config;

public class RabbitMQConstants {
    public static final String EXCHANGE_NAME = "job_events_exchange";
    public static final String WORKER_QUEUE = "worker_queue";
    public static final String SERVER_QUEUE = "server_queue";
    public static final String JOB_CREATED_ROUTING_KEY = "job.created";
    public static final String JOB_FINISHED_ROUTING_KEY = "job.finished";
}