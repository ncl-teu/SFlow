package net.gripps.clustering.algorithms.mwsl_delta;

/**
 * Author: H. Kanemitsu
 * Date: 14/08/22
 */
public class LevelInfo {

    private long fromTaskID;

    private long toTaskID;

    private long tmpBlevel;

    public LevelInfo(long fromTaskID, long toTaskID, long tmpBlevel) {
        this.fromTaskID = fromTaskID;
        this.toTaskID = toTaskID;
        this.tmpBlevel = tmpBlevel;
    }

    public long getFromTaskID() {
        return fromTaskID;
    }

    public void setFromTaskID(long fromTaskID) {
        this.fromTaskID = fromTaskID;
    }

    public long getToTaskID() {
        return toTaskID;
    }

    public void setToTaskID(long toTaskID) {
        this.toTaskID = toTaskID;
    }

    public long getTmpBlevel() {
        return tmpBlevel;
    }

    public void setTmpBlevel(long tmpBlevel) {
        this.tmpBlevel = tmpBlevel;
    }
}
