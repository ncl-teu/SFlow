package net.gripps.clustering.algorithms.triplet;

import net.gripps.clustering.common.aplmodel.AbstractTask;
import net.gripps.clustering.common.aplmodel.BBTask;
import net.gripps.clustering.common.aplmodel.CustomIDSet;
import net.gripps.clustering.common.aplmodel.DataDependence;

import java.util.Iterator;

/**
 * Author: H. Kanemitsu
 * Date: 14/12/18
 */
public class TripletInfo {

    private int totalDegree;

    private long totalDataSize;

    private CustomIDSet taskSet;


    public TripletInfo(CustomIDSet set, BBTask apl){
        this.taskSet = set;
        //Degreeの設定
        Iterator<Long> setIte = set.iterator();
        int totaldegree = 0;
        long datasize = 0;

        while(setIte.hasNext()){
            Long id = setIte.next();
            AbstractTask task = apl.findTaskByLastID(id);
            totaldegree += task.getDsucList().size();
            totaldegree += task.getDpredList().size();
            Iterator<DataDependence> dpredIte = task.getDpredList().iterator();
            Iterator<DataDependence> dsucIte = task.getDsucList().iterator();

            while(dpredIte.hasNext()){
                DataDependence dpred = dpredIte.next();
                if(this.taskSet.contains(dpred.getFromID().get(1))){
                    continue;
                }else{
                    datasize += dpred.getMaxDataSize();
                }
            }

            while(dsucIte.hasNext()){
                DataDependence dsuc = dsucIte.next();
                if(this.taskSet.contains(dsuc.getToID().get(1))){
                    continue;
                }else{
                    datasize += dsuc.getMaxDataSize();
                }
            }

        }
        this.setTotalDegree(totaldegree);
        this.setTotalDataSize(datasize);


    }

    public TripletInfo(int totalDegree, int totalDataSize, CustomIDSet taskSet) {
        this.totalDegree = totalDegree;
        this.totalDataSize = totalDataSize;
        this.taskSet = taskSet;
    }

    public boolean containsAtLeastOne(CustomIDSet set){
        boolean ret = false;
        Iterator<Long> setIte = set.iterator();
        while(setIte.hasNext()){
            Long id = setIte.next();
            if(this.taskSet.contains(id)){
                ret = true;
                break;
            }
        }
        return ret;
    }

    public boolean containsAll(CustomIDSet set){
        boolean ret = true;
        Iterator<Long> setIte = set.iterator();
        while(setIte.hasNext()){
            Long id = setIte.next();
            if(this.taskSet.contains(id)){
                continue;
            }else{
                ret = false;
                break;
            }
        }
        return ret;

    }



    public int getTotalDegree() {
        return totalDegree;
    }

    public void setTotalDegree(int totalDegree) {
        this.totalDegree = totalDegree;
    }

    public long getTotalDataSize() {
        return totalDataSize;
    }

    public void setTotalDataSize(long totalDataSize) {
        this.totalDataSize = totalDataSize;
    }

    public CustomIDSet getTaskSet() {
        return taskSet;
    }

    public void setTaskSet(CustomIDSet taskSet) {
        this.taskSet = taskSet;
    }
}
