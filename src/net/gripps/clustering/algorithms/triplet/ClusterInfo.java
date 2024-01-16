package net.gripps.clustering.algorithms.triplet;

import net.gripps.clustering.common.aplmodel.TaskCluster;

/**
 * Author: H. Kanemitsu
 * Date: 15/01/01
 */
public class ClusterInfo {
    private TaskCluster cluster;

    private int totalDegree;

    private long totalDataSize;

    private long totalTaskSize;

    public ClusterInfo(TaskCluster cluster, int totalDegree, long totalDataSize) {
        this.cluster = cluster;
        this.totalDegree = totalDegree;
        this.totalDataSize = totalDataSize;
    }

    public ClusterInfo(TaskCluster cluster, int totalDegree, long totalDataSize, long totalTaskSize) {
        this.cluster = cluster;
        this.totalDegree = totalDegree;
        this.totalDataSize = totalDataSize;
        this.totalTaskSize = totalTaskSize;
    }

    public long getTotalTaskSize() {
        return totalTaskSize;
    }

    public void setTotalTaskSize(long totalTaskSize) {
        this.totalTaskSize = totalTaskSize;
    }

    public TaskCluster getCluster() {
        return cluster;
    }

    public void setCluster(TaskCluster cluster) {
        this.cluster = cluster;
    }

    public int getTotalDegree() {
        return totalDegree;
    }

    public void setTotalDegree(int totalDegree) {
        this.totalDegree = totalDegree;
    }

    public long getTotalDataSize() {
        return totalDataSize;
    }

    public void setTotalDataSize(long totalDataSize) {
        this.totalDataSize = totalDataSize;
    }
}
