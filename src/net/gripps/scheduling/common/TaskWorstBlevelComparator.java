package net.gripps.scheduling.common;

import net.gripps.clustering.common.aplmodel.AbstractTask;

import java.util.Comparator;

/**
 * Created by IntelliJ IDEA.
 * User: kanemih
 * Date: 2008/11/06
 * Time: 3:16:32
 * To change this template use File | Settings | File Templates.
 */
public class TaskWorstBlevelComparator implements Comparator {

    public int compare(Object o1, Object o2) {
                //優先度Blevelの大きい順にソートする．
        // long value = ((AbstractTask)o1).getPriorityBlevel() - ((AbstractTask)o2).getPriorityBlevel();
         AbstractTask task1 = (AbstractTask)o1;
         AbstractTask task2 = (AbstractTask)o2;
         if(task1.getBlevel() > task2.getBlevel()){
             return -1;
         }
         if(task1.getBlevel() == task2.getBlevel()){
             //IDが小さいものを先頭にする．
            // if(task1.getIDVector().get(1).longValue() > task2.getIDVector().get(1).longValue()){
             //Tlevelが小さい順
             if(task1.getTlevel() > task2.getTlevel()){
                 return 1;
             }else{
                 return -1;
             }

         }

         if(task1.getBlevel() < task2.getBlevel()){
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
