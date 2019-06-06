package org.ncl.workflow.util;

/**
 * Created by Hidehiro Kanemitsu on 2019/05/31
 */
public class ProcessMgr {

    private static ProcessMgr own;

    private long startTime;

    private long finishTime;

    private ProcessMgr(){


    }

    public static ProcessMgr getIns(){
        if(ProcessMgr.own == null){
            ProcessMgr.own = new ProcessMgr();
        }
        return ProcessMgr.own;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(long finishTime) {
        this.finishTime = finishTime;
    }
}
