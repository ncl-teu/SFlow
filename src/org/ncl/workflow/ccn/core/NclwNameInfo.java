package org.ncl.workflow.ccn.core;

import java.io.Serializable;

/**
 * Created by Hidehiro Kanemitsu on 2019/12/05
 */
public class NclwNameInfo implements Serializable {

    private long jobID;

    private long taskID;

    public NclwNameInfo(long jobID, long taskID) {
        this.jobID = jobID;
        this.taskID = taskID;
    }

    public long getJobID() {
        return jobID;
    }

    public void setJobID(long jobID) {
        this.jobID = jobID;
    }

    public long getTaskID() {
        return taskID;
    }

    public void setTaskID(long taskID) {
        this.taskID = taskID;
    }
}
