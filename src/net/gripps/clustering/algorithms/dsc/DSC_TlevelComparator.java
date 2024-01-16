package net.gripps.clustering.algorithms.dsc;

import net.gripps.clustering.common.aplmodel.AbstractTask;

import java.util.Comparator;

/**
 * Author: H. Kanemitsu
 * Date: 2008/10/30
 */
public class DSC_TlevelComparator implements Comparator {
        /**
     *
     * @param o1
     * @param o2
     * @return
     */
    public int compare(Object o1, Object o2){
        AbstractTask task1 = (AbstractTask)o1;
        AbstractTask task2 = (AbstractTask)o2;

        long value1 = task1.getTlevel();
        long value2 = task2.getTlevel();

        //tlevelの昇順に並び替える．
        if(value1 >= value2){
            return 1;
        }else{
            return -1;
        }
    }
}
