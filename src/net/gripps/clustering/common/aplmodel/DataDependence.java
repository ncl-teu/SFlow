package net.gripps.clustering.common.aplmodel;

import java.util.Vector;
import java.io.Serializable;

/**
 * Author: Hidehiro Kanemitsu<br>
 * Date: 2007/07/06
 */
public class DataDependence implements Serializable{

    /**
     *
     */
    private Vector<Long> fromID;

    /**
     *
     */
    private Vector<Long> toID;

    /**
     *  List of data size(size is variable depending on execution condition
     *  by control dependencies
     */
    private long maxDataSize;
    
    /**
     * 
     */ 
    private long aveDataSize;
    
    /**
     * 
     */ 
    private long minDataSize;

    private boolean isChecked;

    private boolean outIsChecked;

    private boolean isReady;

    protected long ave_comTime;





    /**
     *
     * @param from
     * @param to
     * @param max
     * @param ave
     * @param min
     */
    public DataDependence(Vector<Long> from,
                          Vector<Long> to,
                          long max,
                          long ave,
                          long min){
        this.setFromID(from);
        this.setToID(to);
        this.maxDataSize = max;
        this.aveDataSize = ave;
        this.minDataSize = min;
        this.isChecked = false;
        this.setOutIsChecked(false);
        this.isReady = false;
        this.ave_comTime = 0;

    }

    
    public boolean getIsChecked(){
        return this.isChecked;
    }

    public void setIsChecked(boolean ret){
        this.isChecked = ret;
    }

    public long getMaxDataSize() {
        return maxDataSize;
    }

    public void setMaxDataSize(long size) {
     //   if(this.maxDataSize > size){
            this.maxDataSize = size;
     //   }
    }

    public long getAveDataSize() {
        return aveDataSize;
    }

    public void setAveDataSize(long size) {
       // this.aveDataSize = (this.aveDataSize+size)/2;
        this.aveDataSize = size;
    }

    public long getMinDataSize() {
        return minDataSize;
    }

    public void setMinDataSize(long size) {
       // if(this.minDataSize > size){
            this.minDataSize = size;
      //  }
    }

    public Vector<Long> getFromID() {
        return fromID;
    }

    public void setFromID(Vector<Long> fromID) {
        if(this.toID == null){
            this.fromID = fromID;
            return;

        }else if((this.toID != null)&&(AplOperator.getInstance().isIDEqual(fromID,this.toID))){
                return;
        }

        this.fromID = fromID;
    }

    public Vector<Long> getToID() {
        return toID;
    }

    public void setToID(Vector<Long> toID) {
        if(this.fromID == null){
            this.toID = toID;
            return;

        }else if((this.fromID != null)&&(AplOperator.getInstance().isIDEqual(this.fromID,toID))){
            return;
        }

        this.toID = toID;
    }

    public boolean isOutIsChecked() {
        return outIsChecked;
    }

    public void setOutIsChecked(boolean outIsChecked) {
        this.outIsChecked = outIsChecked;
    }

    public boolean isReady() {
        return isReady;
    }

    public void setReady(boolean ready) {
        isReady = ready;
    }

    public long getAve_comTime() {
        return ave_comTime;
    }

    public void setAve_comTime(long ave_comTime) {
        this.ave_comTime = ave_comTime;
    }
}
