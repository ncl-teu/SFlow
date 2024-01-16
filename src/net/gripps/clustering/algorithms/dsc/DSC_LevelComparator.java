package net.gripps.clustering.algorithms.dsc;

import net.gripps.clustering.common.aplmodel.AbstractTask;

import java.util.Comparator;

/**
 * Author: H. Kanemitsu
 * Date: 2008/10/28
 */
public class DSC_LevelComparator implements Comparator {

    /**
     *
     * @param o1
     * @param o2
     * @return
     */
    public int compare(Object o1, Object o2){
        DSC_MP mp1 = (DSC_MP)o1;
        DSC_MP mp2 = (DSC_MP)o2;

        AbstractTask task1 = mp1.getTask();
        AbstractTask task2 = mp2.getTask();
        long value1 = task1.getTlevel() + mp1.getExecTime() + mp1.getNwTime();
        long value2 = task2.getTlevel() + mp2.getExecTime() + mp2.getNwTime();
        if(value2 >= value1){
            return 1;
        }else{
            return -1;
        }
    }
}
