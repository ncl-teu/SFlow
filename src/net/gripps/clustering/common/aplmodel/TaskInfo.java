package net.gripps.clustering.common.aplmodel;

import java.io.Serializable;

/**
 * Author: H. Kanemitsu
 * Date: 2009/05/09
 */
public class TaskInfo implements Serializable {

    private Long taskID;

    private long startTime;

    //private long finishTime;
    
    private long taskSize;

    /**
     *
     * @param taskID
     * @param taskSize
     */
    public TaskInfo(Long taskID, long taskSize) {
        this.taskID = taskID;
        this.taskSize = taskSize;
        this.startTime = -1;

    }

   

    public Long getTaskID() {
        return taskID;
    }

    public void setTaskID(Long taskID) {
        this.taskID = taskID;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }




    public long getTaskSize() {
        return taskSize;
    }

    public void setTaskSize(long taskSize) {
        this.taskSize = taskSize;
    }
}
