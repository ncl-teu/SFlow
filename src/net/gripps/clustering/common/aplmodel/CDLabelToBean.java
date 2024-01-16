package net.gripps.clustering.common.aplmodel;

import java.util.Vector;

/**
 * Author: Hidehiro Kanemitsu<br>
 * Date: 2007/07/31
 */
public class CDLabelToBean {
    private long max;
    private long ave;
    private long min;
    private Vector<Long> labelToID;

    public CDLabelToBean(long in_max, long in_ave, long in_min, Vector<Long> in_labelToID){
        this.setMax(in_max);
        this.setAve(in_ave);
        this.setMin(in_min);
        this.setLabelToID(in_labelToID);
    }


    public long getMax() {
        return max;
    }

    public void setMax(long max) {
        this.max = max;
    }

    public long getAve() {
        return ave;
    }

    public void setAve(long ave) {
        this.ave = ave;
    }

    public long getMin() {
        return min;
    }

    public void setMin(long min) {
        this.min = min;
    }

    public Vector<Long> getLabelToID() {
        return labelToID;
    }

    public void setLabelToID(Vector<Long> labelToID) {
        this.labelToID = labelToID;
    }

}
