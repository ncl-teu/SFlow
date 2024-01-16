package net.gripps.clustering.algorithms.fcs;

import net.gripps.clustering.common.aplmodel.AbstractTask;

import java.util.Comparator;

/**
 * Author: H. Kanemitsu
 * Date: 14/10/09
 */
public class FCS_TaskComparator implements Comparator {
    public int compare(Object o1, Object o2) {
     //優先度の小さい順にソートする．
    // long value = ((AbstractTask)o1).getPriorityBlevel() - ((AbstractTask)o2).getPriorityBlevel();
        Long m1 = (Long)o1;
        Long m2 = (Long)o2;
     if(m1.longValue()> m2.longValue()){
         return 1;
     }
     if(m1.longValue() <= m2.longValue()){
         return -1;
     }

     return 0;
}
}

