package net.gripps.environment;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Author: H. Kanemitsu
 * Date: 11/06/14
 */
public class AverageLinkComparator implements Comparator, Serializable {
        public int compare(Object o1, Object o2) {
         //優先度の大きい順にソートする．
        // long value = ((AbstractTask)o1).getPriorityBlevel() - ((AbstractTask)o2).getPriorityBlevel();
         LinkInfo link1 = (LinkInfo)o1;
         LinkInfo link2 = (LinkInfo)o2;
         if(link1.getAverageLinkSpeed() > link2.getAverageLinkSpeed()){
             return -1;
         }
         if(link1.getAverageLinkSpeed() <= link2.getAverageLinkSpeed()){
             return 1;
         }

         return 0;
    }
}
