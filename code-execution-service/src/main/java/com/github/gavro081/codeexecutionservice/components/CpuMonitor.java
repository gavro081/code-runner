package com.github.gavro081.codeexecutionservice.components;

import com.sun.management.OperatingSystemMXBean;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;

@Component
public class CpuMonitor {
    private final OperatingSystemMXBean osBean =
            (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

    public double getSystemCpuLoad() {
        double load = osBean.getCpuLoad();
        return  (load < 0 || Double.isNaN(load)) ? 0: load;
    }
}
