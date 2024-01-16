package net.gripps.clustering.algorithms.mwsl_delta;

import java.util.Comparator;

/**
 *
 */
public class TaskGrainComparator implements Comparator {

    public int compare(Object o1, Object o2) {
         //優先度の大きい順にソートする．
        // long value = ((AbstractTask)o1).getPriorityBlevel() - ((AbstractTask)o2).getPriorityBlevel();
         TaskGrain task1 = (TaskGrain)o1;
         TaskGrain task2 = (TaskGrain)o2;
         if(task1.getGmax() > task2.getGmax()){
             return -1;
         }
         if(task1.getGmax() <= task2.getGmax()){
             return 1;
         }

         return 0;
    }
}