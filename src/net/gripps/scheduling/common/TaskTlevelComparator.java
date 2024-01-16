package net.gripps.scheduling.common;

import net.gripps.clustering.common.aplmodel.AbstractTask;

import java.util.Comparator;

/**
 * Author: H. Kanemitsu
 * Date: 2009/05/15
 */
public class TaskTlevelComparator implements Comparator {
    /**
     * @param o1
     * @param o2
     * @return
     */
    public int compare(Object o1, Object o2) {
        //優先度Tlevelの小さい順にソートする．
        // long value = ((AbstractTask)o1).getPriorityBlevel() - ((AbstractTask)o2).getPriorityBlevel();
        AbstractTask task1 = (AbstractTask) o1;
        AbstractTask task2 = (AbstractTask) o2;
        if (task1.getTlevel() < task2.getTlevel()) {
            return -1;
        }
        if (task1.getTlevel() == task2.getTlevel()) {
            //IDが小さいものを先頭にする．
            if (task1.getBlevel() < task2.getBlevel()) {
                return 1;
            } else {
                return -1;
            }
        }

        if (task1.getTlevel() > task2.getTlevel()) {
            return 1;
        }

        return 0;
    }
}
