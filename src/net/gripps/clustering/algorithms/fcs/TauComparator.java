package net.gripps.clustering.algorithms.fcs;

import java.util.Comparator;

/**
 * Author: H. Kanemitsu
 * Date: 14/10/05
 */
public class TauComparator implements Comparator {

    public int compare(Object o1, Object o2) {
     //優先度の小さい順にソートする．
    // long value = ((AbstractTask)o1).getPriorityBlevel() - ((AbstractTask)o2).getPriorityBlevel();
        FCS_CPUInfo m1 = (FCS_CPUInfo)o1;
        FCS_CPUInfo m2 = (FCS_CPUInfo)o2;
     if(m1.getTau() > m2.getTau()){
         return 1;
     }
     if(m1.getTau() <= m2.getTau()){
         return -1;
     }

     return 0;
}
}
