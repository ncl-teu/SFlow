package net.gripps.clustering.algorithms.loadbacancing;

/**
 * Author: H. Kanemitsu
 * Date: 2008/02/09
 */
public class ClusterInfo {

    private Long id;
    private long size;

    public ClusterInfo(Long id, long size) {
        this.id = id;
        this.size = size;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
