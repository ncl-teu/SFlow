package net.gripps.grouping;

import java.util.Comparator;

/**
 * Created by kanemih on 2016/01/18.
 */
public class DistanceComparator implements Comparator {
    public int compare(Object o1, Object o2) {
        //優先度の小さい順にソートする．
        DistanceInfo m1 = (DistanceInfo) o1;
        DistanceInfo m2 = (DistanceInfo) o2;
        if (m1.getDistance() < m2.getDistance()) {
            return -1;
        }
        if (m1.getDistance() > m2.getDistance()) {
            return 1;
        }

        return 0;
    }
}
