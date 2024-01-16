package net.gripps.grouping;

import net.gripps.environment.CPU;

/**
 * Created by kanemih on 2016/01/16.
 */
public class IndexInfo {
    private CPU cpu;
    private double indexValue;
    private double lowerBound;

    public IndexInfo(CPU cpu, double indexValue, double bound) {
        this.cpu = cpu;
        this.indexValue = indexValue;
        this.lowerBound = bound;
    }

    public double getLowerBound() {
        return lowerBound;
    }

    public void setLowerBound(double lowerBound) {
        this.lowerBound = lowerBound;
    }

    public CPU getCpu() {
        return cpu;
    }

    public void setCpu(CPU cpu) {
        this.cpu = cpu;
    }

    public double getIndexValue() {
        return indexValue;
    }

    public void setIndexValue(double indexValue) {
        this.indexValue = indexValue;
    }
}
