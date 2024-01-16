package net.gripps.scheduling.algorithms.heterogeneous.heft;

import net.gripps.clustering.common.aplmodel.AbstractTask;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Author: H. Kanemitsu
 * Date: 14/09/28
 */
public class StartTimeComparator implements Comparator,Serializable {
    public int compare(Object o1, Object o2) {
     //優先度の小さい順にソートする．
    // long value = ((AbstractTask)o1).getPriorityBlevel() - ((AbstractTask)o2).getPriorityBlevel();
     AbstractTask m1 = (AbstractTask)o1;
     AbstractTask m2 = (AbstractTask)o2;
     if(m1.getStartTime() > m2.getStartTime()){
         return 1;
     }
     if(m1.getStartTime() < m2.getStartTime()){
         return -1;
     }

     return 0;
}
}
