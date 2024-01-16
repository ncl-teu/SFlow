package net.gripps.scheduling.algorithms.heterogeneous.msl;

import net.gripps.clustering.common.aplmodel.AbstractTask;

import java.util.Comparator;

/**
 * Author: H. Kanemitsu
 * Date: 15/11/09
 */
public class MSLComparator implements Comparator{

    public int compare(Object o1, Object o2) {
        //優先度の大きい順にソートする．
        // long value = ((AbstractTask)o1).getPriorityBlevel() - ((AbstractTask)o2).getPriorityBlevel();
        AbstractTask m1 = (AbstractTask) o1;
        AbstractTask m2 = (AbstractTask) o2;
        if (m1.getMsl_rank() > m2.getMsl_rank()) {
            return -1;
        }
        if (m1.getMsl_rank() < m2.getMsl_rank()) {
            return 1;
        }

        return 0;
    }
}
