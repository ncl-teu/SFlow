package net.gripps.clustering.algorithms.mwsl_delta;

/**
 * Author: H. Kanemitsu
 * Date: 15/05/19
 */
public class WSLInfo {

    private long minDataSize;

    private long maxDataSize;

    private long minTaskSize;

    private long maxTaskSize;

    private long wsl_weight;

    private long nonClusteredTaskSize;

    public WSLInfo(long minDataSize, long maxDataSize, long minTaskSize, long maxTaskSize, long wsl_weight, long value) {
        this.minDataSize = minDataSize;
        this.maxDataSize = maxDataSize;
        this.minTaskSize = minTaskSize;
        this.maxTaskSize = maxTaskSize;
        this.wsl_weight = wsl_weight;
        this.nonClusteredTaskSize = value;
    }

    public WSLInfo(){
        this.minDataSize = 1000000;
        this.maxDataSize = 0;
        this.minTaskSize = 1000000;
        this.maxTaskSize = 0;
        this.wsl_weight = 0;
        this.nonClusteredTaskSize = 0;
    }

    public long getMinDataSize() {
        return minDataSize;
    }

    public void setMinDataSize(long minDataSize) {
        this.minDataSize = minDataSize;
    }

    public long getMaxDataSize() {
        return maxDataSize;
    }

    public void setMaxDataSize(long maxDataSize) {
        this.maxDataSize = maxDataSize;
    }

    public long getMinTaskSize() {
        return minTaskSize;
    }

    public void setMinTaskSize(long minTaskSize) {
        this.minTaskSize = minTaskSize;
    }

    public long getMaxTaskSize() {
        return maxTaskSize;
    }

    public void setMaxTaskSize(long maxTaskSize) {
        this.maxTaskSize = maxTaskSize;
    }

    public long getWsl_weight() {
        return wsl_weight;
    }

    public void setWsl_weight(long wsl_weight) {
        this.wsl_weight = wsl_weight;
    }

    public long getNonClusteredTaskSize() {
        return nonClusteredTaskSize;
    }

    public void setNonClusteredTaskSize(long nonClusteredTaskSize) {
        this.nonClusteredTaskSize = nonClusteredTaskSize;
    }
}
