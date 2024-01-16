package net.gripps.clustering.algorithms.triplet;

import net.gripps.clustering.common.aplmodel.AbstractTask;

import java.util.Comparator;

/**
 * Author: H. Kanemitsu
 * Date: 14/12/20
 */
 public class TripletComparator implements Comparator {
    public int compare(Object o1, Object o2) {
        //優先度の大きい順にソートする．
        // long value = ((AbstractTask)o1).getPriorityBlevel() - ((AbstractTask)o2).getPriorityBlevel();
        TripletInfo m1 = (TripletInfo) o1;
        TripletInfo m2 = (TripletInfo) o2;
        if (m1.getTotalDegree() > m2.getTotalDegree()) {
            return -1;
        }
        if (m1.getTotalDegree() < m2.getTotalDegree()) {
            return 1;
        }

        if (m1.getTotalDegree() == m2.getTotalDegree()) {
            //Degreeが同じであれば，データサイズを見る．
            if (m1.getTotalDataSize() >= m2.getTotalDataSize()) {
                return -1;
            } else {
                return 1;
            }
        }
        return 0;
    }
}
