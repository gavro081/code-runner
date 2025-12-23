package com.github.gavro081.codeexecutionservice.listeners;

import com.github.gavro081.codeexecutionservice.components.CpuMonitor;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CpuAwareListenerController {
    private static final double CPU_LOAD_THRESHOLD = 0.85;
    private static final long SLEEP_MS = 1_000;

    private final RabbitListenerEndpointRegistry registry;
    private final CpuMonitor cpuMonitor;

    public CpuAwareListenerController(
            RabbitListenerEndpointRegistry registry,
            CpuMonitor cpuMonitor
    ) {
        this.registry = registry;
        this.cpuMonitor = cpuMonitor;
    }

    @Scheduled(fixedDelay = 1000)
    public void controlListeners() {
        double systemCpuLoad = cpuMonitor.getSystemCpuLoad();
        boolean highCpu = systemCpuLoad > CPU_LOAD_THRESHOLD;

        registry.getListenerContainers().forEach(container -> {
            if (highCpu && container.isRunning()) {
                container.stop();
                try {
                    System.out.println(String.format("THREAD %s:CPU Load is: %f, threshold reached -> sleeping for %d ms.",
                            Thread.currentThread().getName(), systemCpuLoad, SLEEP_MS));
                    Thread.sleep(SLEEP_MS);
                } catch (InterruptedException e){
                    Thread.currentThread().interrupt();
                }
            } else if (!highCpu && !container.isRunning()) {
                container.start();
            }
        });
    }
}
