package net.gripps.clustering.algorithms.dsc;

import net.gripps.clustering.common.aplmodel.AbstractTask;

/**
 * Author: H. Kanemitsu
 * Date: 2008/10/28
 * DSCアルゴリズムで，minimization procedureに使われる一時的なクラスです．
 */
public class DSC_MP {

    /**
     * 
     */
    private AbstractTask task;

    /**
     *
     */
    private long nwTime;

    /**
     *
     */
    private long execTime;

    /**
     *
     * @param task
     * @param nwTime
     * @param execTime
     */
    public DSC_MP(AbstractTask task, long execTime, long nwTime ) {
        this.task = task;
        this.nwTime = nwTime;
        this.execTime = execTime;
        
    }

    /**
     *
     * @return
     */
    public AbstractTask getTask() {
        return task;
    }

    /**
     *
     * @param task
     */
    public void setTask(AbstractTask task) {
        this.task = task;
    }


    /**
     *
     * @return
     */
    public long getNwTime() {
        return nwTime;
    }

    /**
     *
     * @param nwTime
     */
    public void setNwTime(long nwTime) {
        this.nwTime = nwTime;
    }

    /**
     *
     * @return
     */
    public long getExecTime() {
        return execTime;
    }

    /**
     *
     * @param execTime
     */
    public void setExecTime(long execTime) {
        this.execTime = execTime;
    }
}
