package net.gripps.clustering.algorithms.triplet;

import java.util.Comparator;

/**
 * Author: H. Kanemitsu
 * Date: 15/01/01
 */
public class ClusterComparator implements Comparator {
    public int compare(Object o1, Object o2) {
            //優先度の大きい順にソートする．
            // long value = ((AbstractTask)o1).getPriorityBlevel() - ((AbstractTask)o2).getPriorityBlevel();
            ClusterInfo m1 = (ClusterInfo) o1;
            ClusterInfo m2 = (ClusterInfo) o2;
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
