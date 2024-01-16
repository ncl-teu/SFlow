package net.gripps.environment;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Author: H. Kanemitsu
 * Date: 11/06/14
 */
public class CPUComparator implements Comparator, Serializable {

    public int compare(Object o1, Object o2) {
         //優先度の大きい順にソートする．
        // long value = ((AbstractTask)o1).getPriorityBlevel() - ((AbstractTask)o2).getPriorityBlevel();
         CPU cpu1 = (CPU)o1;
         CPU cpu2 = (CPU)o2;
         if(cpu1.getSpeed() > cpu2.getSpeed()){
             return -1;
         }
         if(cpu1.getSpeed() <= cpu2.getSpeed()){
             return 1;
         }

         return 0;
    }
}
