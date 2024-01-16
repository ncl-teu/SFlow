package net.gripps.scheduling.common;

import net.gripps.clustering.common.aplmodel.AbstractTask;

import java.util.Comparator;

/**
 * Author: H. Kanemitsu
 * Date: 2008/10/26
 */
public class TaskPriorityTlevelComparator implements Comparator {
     public int compare(Object o1, Object o2){
         //優先度Tlevelの小さい順にソートする．
        // long value = ((AbstractTask)o1).getPriorityBlevel() - ((AbstractTask)o2).getPriorityBlevel();
         AbstractTask task1 = (AbstractTask)o1;
         AbstractTask task2 = (AbstractTask)o2;
         if(task1.getPriorityTlevel() < task2.getPriorityTlevel()){
             return -1;
         }
         if(task1.getPriorityTlevel() == task2.getPriorityTlevel()){
             //IDが小さいものを先頭にする．
             /*if(task1.getPriorityBlevel() < task2.getPriorityBlevel()){
                 return 1;
             }else{
                 return -1;
             } */

         }

         if(task1.getPriorityTlevel() > task2.getPriorityTlevel()){
             return 1;
         }

         return 0;
  }
}