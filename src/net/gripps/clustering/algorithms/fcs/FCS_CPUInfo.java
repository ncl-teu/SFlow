package net.gripps.clustering.algorithms.fcs;

import net.gripps.environment.CPU;

/**
 * Author: H. Kanemitsu
 * Date: 14/10/05
 */
public class FCS_CPUInfo {
    private CPU cpu;

    /**
     * CPU Speed / Bandwidth of the Machine
     */
    private double tau;

    public FCS_CPUInfo(CPU cpu, double tau) {
        this.cpu = cpu;
        this.tau = tau;
    }

    public double getTau() {
        return tau;
    }

    public void setTau(double tau) {
        this.tau = tau;
    }

    public CPU getCpu() {
        return cpu;
    }

    public void setCpu(CPU cpu) {
        this.cpu = cpu;
    }
}
