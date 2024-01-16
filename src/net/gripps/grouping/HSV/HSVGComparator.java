package net.gripps.grouping.HSV;

import net.gripps.clustering.common.aplmodel.AbstractTask;

import java.util.Comparator;

/**
 * Created by kanemih on 2016/01/27.
 */
public class HSVGComparator implements Comparator {
    public int compare(Object o1, Object o2) {
        //優先度の大きい順にソートする．
        AbstractTask m1 = (AbstractTask) o1;
        AbstractTask m2 = (AbstractTask) o2;
        if (m1.getHSVRankG() > m2.getHSVRankG()) {
            return -1;
        }
        if (m1.getHSVRankG() < m2.getHSVRankG()) {
            return 1;
        }

        return 0;
    }
}
