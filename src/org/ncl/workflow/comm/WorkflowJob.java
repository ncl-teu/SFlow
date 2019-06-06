package org.ncl.workflow.comm;

import net.gripps.clustering.common.aplmodel.CustomIDSet;
import org.ncl.workflow.engine.Task;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by Hidehiro Kanemitsu on 2019/05/07
 */
public class WorkflowJob implements Serializable {

    /**
     * Job ID
     */
    private long jobID;

    /**
     * Map of tasks that this job has.
     */
    private HashMap<Long, Task> taskMap;

    /**
     *
     */
    private CustomIDSet startTaskSet;



    public WorkflowJob(long jobID, HashMap<Long, Task> taskMap) {
        this.jobID = jobID;
        this.taskMap = taskMap;
        this.startTaskSet = new CustomIDSet();
    }

    public long getJobID() {
        return jobID;
    }

    public void setJobID(long jobID) {
        this.jobID = jobID;
    }

    public HashMap<Long, Task> getTaskMap() {
        return taskMap;
    }

    public void setTaskMap(HashMap<Long, Task> taskMap) {
        this.taskMap = taskMap;
    }

    public CustomIDSet getStartTaskSet() {
        return startTaskSet;
    }

    public void setStartTaskSet(CustomIDSet startTaskSet) {
        this.startTaskSet = startTaskSet;
    }
}
