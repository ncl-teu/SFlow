package net.gripps.mapping.LB;

import net.gripps.environment.CPU;

import java.util.Comparator;

/**
 * Author: H. Kanemitsu
 * Date: 2008/11/01
 */
public class MachineComparator implements Comparator {

    public int compare(Object o1, Object o2) {
        CPU m1 = (CPU)o1;
        CPU m2 = (CPU)o2;
        //マシン速度の，大きい順に並び替える
        if(m1.getSpeed() > m2.getSpeed()){
            return -1;

        }else{
            return 1;
        }
    }
}
