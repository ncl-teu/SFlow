package net.gripps.environment;

import java.util.Comparator;

/**
 * Author: H. Kanemitsu
 * Date: 2010/06/04
 */
public class HopComparator implements Comparator {
        public int compare(Object o1, Object o2) {
         //hop数の小さい順にソートする．
         HopInfo hop1 = (HopInfo)o1;
         HopInfo hop2 = (HopInfo)o2;
         if(hop1.getHop() > hop2.getHop()){
             return 1;
         }
         if(hop1.getHop() <= hop2.getHop()){
             return -1;
         }

         return 0;
    }
}
