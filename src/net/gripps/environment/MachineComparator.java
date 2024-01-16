package net.gripps.environment;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Author: H. Kanemitsu
 * Date: 11/06/15
 */
public class MachineComparator implements Comparator, Serializable {
        public int compare(Object o1, Object o2) {
         //評価値数の小さい順にソートする．
         TmpMachine m1 = (TmpMachine)o1;
         TmpMachine m2 = (TmpMachine)o2;
         if(m1.getEvalValue() > m2.getEvalValue()){
             return 1;
         }
         if(m1.getEvalValue() <= m2.getEvalValue()){
             return -1;
         }

         return 0;
    }
}
