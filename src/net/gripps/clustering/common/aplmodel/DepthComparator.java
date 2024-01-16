package net.gripps.clustering.common.aplmodel;

import java.util.Comparator;

/**
 * Author: H. Kanemitsu
 * Date: 14/09/25
 */
public class DepthComparator implements Comparator {

    public int compare(Object o1, Object o2) {
        //depthの小さい順にソートする．
        // long value = ((AbstractTask)o1).getPriorityBlevel() - ((AbstractTask)o2).getPriorityBlevel();
        AbstractTask m1 = (AbstractTask) o1;
        AbstractTask m2 = (AbstractTask) o2;
        if (m1.getDepth() > m2.getDepth()) {
            return 1;
        }
        if (m1.getDepth() < m2.getDepth()) {
            return -1;
        }

        return 0;
    }
}
