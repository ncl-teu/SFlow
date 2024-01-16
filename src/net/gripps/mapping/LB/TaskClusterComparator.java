package net.gripps.mapping.LB;

import net.gripps.clustering.common.aplmodel.TaskCluster;
import net.gripps.clustering.common.aplmodel.CustomIDSet;
import net.gripps.clustering.common.aplmodel.AbstractTask;

import java.util.Comparator;
import java.util.Iterator;

/**
 * Author: H. Kanemitsu
 * Date: 2008/11/01
 */
public class TaskClusterComparator implements Comparator {


    /**
     * 
     * @param o1
     * @param o2
     * @return
     */
    public int compare(Object o1, Object o2) {
        TaskCluster c1 = (TaskCluster)o1;
        TaskCluster c2 = (TaskCluster)o2;

        //サイズの大きい順にする
        if(c1.getClusterSize() > c2.getClusterSize()){
            return -1;
        }else{
            return 1;
        }


    }



}
