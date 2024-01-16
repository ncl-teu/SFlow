package net.gripps.scheduling.algorithms.heterogeneous.mwsl;

import net.gripps.clustering.common.aplmodel.AbstractTask;

import java.util.Comparator;

/**
 * Author: H. Kanemitsu
 * Date: 14/09/26
 */
public class WSLComparator implements Comparator {

    public int compare(Object o1, Object o2) {
        //優先度の大きい順にソートする．
        AbstractTask m1 = (AbstractTask) o1;
        AbstractTask m2 = (AbstractTask) o2;
        if (m1.getTlevel()+m1.getBlevel() > m2.getTlevel()+m2.getBlevel()) {
            return -1;
        }
        if (m1.getTlevel()+m1.getBlevel() < m2.getTlevel()+m2.getBlevel()) {
            return 1;
        }

        return 0;
    }
}
