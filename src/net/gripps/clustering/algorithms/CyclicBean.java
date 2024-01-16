package net.gripps.clustering.algorithms;

import net.gripps.clustering.common.aplmodel.CustomIDSet;

/**
 * Author: H. Kanemitsu
 * Date: 2008/09/08
 */
public class CyclicBean {
    private long tlevel;
    private CustomIDSet cyclicClusterSet;

    public CyclicBean(long tlevel, CustomIDSet cyclicClusterSet) {
        this.tlevel = tlevel;
        this.cyclicClusterSet = cyclicClusterSet;
    }

    public CyclicBean(){
        this.tlevel = 0;
        this.cyclicClusterSet = new CustomIDSet();
    }

    public long getTlevel() {
        return tlevel;
    }

    public void setTlevel(long tlevel) {
        this.tlevel = tlevel;
    }

    public CustomIDSet getCyclicClusterSet() {
        return cyclicClusterSet;
    }

    public void setCyclicClusterSet(CustomIDSet cyclicClusterSet) {
        this.cyclicClusterSet = cyclicClusterSet;
    }
}
