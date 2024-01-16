package net.gripps.grouping;

import net.gripps.environment.CPU;

/**
 * Created by kanemih on 2016/01/18.
 */
public class DistanceInfo {
    private CPU cpu;
    private double distance;

    private double lowerbound;

    public DistanceInfo(CPU cpu, double distance, double bound) {
        this.cpu = cpu;
        this.distance = distance;
        this.lowerbound = bound;
    }

    public double getLowerbound() {
        return lowerbound;
    }

    public void setLowerbound(double lowerbound) {
        this.lowerbound = lowerbound;
    }

    public CPU getCpu() {
        return cpu;
    }

    public void setCpu(CPU cpu) {
        this.cpu = cpu;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }
}
