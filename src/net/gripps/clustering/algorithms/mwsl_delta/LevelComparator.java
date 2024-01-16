package net.gripps.clustering.algorithms.mwsl_delta;

import java.util.Comparator;

/**
 * Author: H. Kanemitsu
 * Date: 14/08/22
 */
public class LevelComparator implements Comparator {

    public int compare(Object o1, Object o2) {
           //優先度の大きい順にソートする．
          // long value = ((AbstractTask)o1).getPriorityBlevel() - ((AbstractTask)o2).getPriorityBlevel();
           LevelInfo m1 = (LevelInfo)o1;
           LevelInfo m2 = (LevelInfo)o2;
           if(m1.getTmpBlevel() > m2.getTmpBlevel()){
               return -1;
           }
           if(m1.getTmpBlevel() <= m2.getTmpBlevel()){
               return 1;
           }

           return 0;
      }
}
