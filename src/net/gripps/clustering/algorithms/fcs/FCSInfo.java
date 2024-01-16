package net.gripps.clustering.algorithms.fcs;

import net.gripps.clustering.common.aplmodel.AbstractTask;
import net.gripps.clustering.common.aplmodel.TaskCluster;

/**
 * Author: H. Kanemitsu
 * Date: 14/10/09
 */
public class FCSInfo {

    AbstractTask task;
    TaskCluster cluster;

    public FCSInfo(AbstractTask task, TaskCluster cluster) {
        this.task = task;
        this.cluster = cluster;
    }

    public TaskCluster getCluster() {
        return cluster;
    }

    public void setCluster(TaskCluster cluster) {
        this.cluster = cluster;
    }

    public AbstractTask getTask() {
        return task;
    }

    public void setTask(AbstractTask task) {
        this.task = task;
    }
}
