package net.gripps.clustering.algorithms;

/**
 * Author: H. Kanemitsu
 * Date: 2008/09/11
 */
public class CyclicMap {
    private Long fromTaskID;
    private Long toTaskID;
    private long dataSize;

    public CyclicMap(Long fromTaskID, Long toTaskID, long dataSize) {
        this.fromTaskID = fromTaskID;
        this.toTaskID = toTaskID;
        this.dataSize = dataSize;
    }

    public Long getFromTaskID() {
        return fromTaskID;
    }

    public void setFromTaskID(Long fromTaskID) {
        this.fromTaskID = fromTaskID;
    }

    public Long getToTaskID() {
        return toTaskID;
    }

    public void setToTaskID(Long toTaskID) {
        this.toTaskID = toTaskID;
    }

    public long getDataSize() {
        return dataSize;
    }

    public void setDataSize(long dataSize) {
        this.dataSize = dataSize;
    }
}
