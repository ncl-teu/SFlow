package net.gripps.scheduling.algorithms.heterogeneous.peft;

import net.gripps.clustering.common.aplmodel.AbstractTask;

import java.util.Comparator;

/**
 * Author: H. Kanemitsu
 * Date: 14/09/16
 */
public class OCTComparator implements Comparator {
    public int compare(Object o1, Object o2) {
     //優先度の大きい順にソートする．
    // long value = ((AbstractTask)o1).getPriorityBlevel() - ((AbstractTask)o2).getPriorityBlevel();
     AbstractTask m1 = (AbstractTask)o1;
     AbstractTask m2 = (AbstractTask)o2;
     if(m1.getAve_oct() > m2.getAve_oct()){
         return -1;
     }
     if(m1.getAve_oct() < m2.getAve_oct()){
         return 1;
     }

     return 0;
}
}
