package net.gripps.scheduling.common;

import net.gripps.clustering.common.aplmodel.AbstractTask;

import java.util.Comparator;

/**
 * Created by kanemih on 2015/05/17.
 */
public class TaskPriorityTlevelBlevelComparator implements Comparator {

    public int compare(Object o1, Object o2){
        //優先度Blevelの大きい順にソートする．
        // long value = ((AbstractTask)o1).getPriorityBlevel() - ((AbstractTask)o2).getPriorityBlevel();
        AbstractTask task1 = (AbstractTask)o1;
        AbstractTask task2 = (AbstractTask)o2;
        if(task1.getPriorityTlevel()+task1.getPriorityBlevel() > task2.getPriorityTlevel()+task2.getPriorityBlevel()){
            return -1;
        }
        if(task1.getPriorityTlevel()+task1.getPriorityBlevel() == task2.getPriorityTlevel()+task2.getPriorityBlevel()){
            //IDが小さいものを先頭にする．
            // if(task1.getIDVector().get(1).longValue() > task2.getIDVector().get(1).longValue()){
            //Tlevelが小さい順
            if(task1.getPriorityTlevel() > task2.getPriorityTlevel()){
                return 1;
            }else{
                return -1;
            }

        }

        if(task1.getPriorityTlevel()+task1.getPriorityBlevel() < task2.getPriorityTlevel()+task2.getPriorityBlevel()){
            return 1;
        }


         /*if(value == 0){
             //もし値が等しいときは，ランダム（実際にはIDの若いものが先）とする．
             value = ((AbstractTask)o2).getIDVector().get(1).longValue() - ((AbstractTask)o1).getIDVector().get(1).longValue();
         }*/

        //   int retValue = (int)value;


        //   return retValue;
        return 0;
    }
}
