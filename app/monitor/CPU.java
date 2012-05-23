package lila.monitor;

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.*;

public class CPU { 
    
    private int  availableProcessors = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
    
    private long lastSystemTime = 0;
    private long lastProcessCpuTime = 0;

    public synchronized double getCpuUsage() {
        if(lastSystemTime == 0) {
            baselineCounters();
            return 0;
        }

        long systemTime = System.nanoTime();
        long processCpuTime = 0;

        if(ManagementFactory.getOperatingSystemMXBean() instanceof OperatingSystemMXBean) {
            processCpuTime = ((OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean()).getProcessCpuTime();
        }

        double cpuUsage = (double) (processCpuTime - lastProcessCpuTime) / (systemTime - lastSystemTime);

        lastSystemTime = systemTime;
        lastProcessCpuTime = processCpuTime;

        return cpuUsage / availableProcessors;
    }

    private void baselineCounters() {
        lastSystemTime = System.nanoTime();

        if (ManagementFactory.getOperatingSystemMXBean() instanceof OperatingSystemMXBean) {
            lastProcessCpuTime = ( (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean() ).getProcessCpuTime();
        }
    }
    
}
