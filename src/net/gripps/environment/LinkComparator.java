package net.gripps.environment;

import java.util.Comparator;
import java.io.Serializable;

/**
 * Author: H. Kanemitsu
 * Date: 2009/12/17
 */
public class LinkComparator implements Comparator, Serializable {

    public int compare(Object o1, Object o2) {
         //優先度の大きい順にソートする．
        // long value = ((AbstractTask)o1).getPriorityBlevel() - ((AbstractTask)o2).getPriorityBlevel();
         LinkInfo link1 = (LinkInfo)o1;
         LinkInfo link2 = (LinkInfo)o2;
         if(link1.getLinkSpeed() > link2.getLinkSpeed()){
             return -1;
         }
         if(link1.getLinkSpeed() <= link2.getLinkSpeed()){
             return 1;
         }

         return 0;
    }
}
