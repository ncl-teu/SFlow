package net.gripps.grouping;

import net.gripps.environment.CPU;

import java.util.Comparator;

/**
 * Created by kanemih on 2016/08/20.
 */
public class BWComparator implements Comparator {
    public int compare(Object o1, Object o2) {
        //優先度の大きい順にソートする．
        CPU  m1 = ((IndexInfo) o1).getCpu();
        CPU m2 = ((IndexInfo) o2).getCpu();
        if (m1.getBw() > m2.getBw()) {
            return -1;
        }else if (m1.getBw() < m2.getBw()) {
            return 1;
        }else{
            if(m1.getSpeed() >= m2.getSpeed()){
                return -1;
            }else{
                return 1;
            }
        }

        //return 0;
    }
}
