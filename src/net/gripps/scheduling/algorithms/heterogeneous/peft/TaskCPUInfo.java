package net.gripps.scheduling.algorithms.heterogeneous.peft;

import net.gripps.environment.CPU;

import java.util.Hashtable;

/**
 * Author: H. Kanemitsu
 * Date: 14/09/24
 */
public class TaskCPUInfo {


    /**
     * CPUID, OCT値の組を格納する．
     */
    private Hashtable<Long, Long> cpuTable;

    public TaskCPUInfo(Hashtable<Long, Long> cpuTable) {

        this.cpuTable = cpuTable;
    }

    public Hashtable<Long, Long> getCpuTable() {
        return cpuTable;
    }

    public void setCpuTable(Hashtable<Long, Long> cpuTable) {
        this.cpuTable = cpuTable;
    }
}
