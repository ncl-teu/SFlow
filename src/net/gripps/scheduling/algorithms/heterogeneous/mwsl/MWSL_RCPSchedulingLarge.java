package net.gripps.scheduling.algorithms.heterogeneous.mwsl;

import net.gripps.clustering.common.aplmodel.AbstractTask;
import net.gripps.clustering.common.aplmodel.BBTask;
import net.gripps.clustering.common.aplmodel.CustomIDSet;
import net.gripps.clustering.common.aplmodel.TaskCluster;
import net.gripps.environment.CPU;
import net.gripps.environment.Environment;
import net.gripps.scheduling.common.TaskPriorityLargeTlevelComparator;
import net.gripps.scheduling.common.TaskPriorityTlevelComparator;

import java.util.*;

/**
 * Author: H. Kanemitsu
 * Date: 15/05/18
 */
public class MWSL_RCPSchedulingLarge extends MWSL_RCPScheduling {
    public MWSL_RCPSchedulingLarge(BBTask apl, String filename, Environment env) {
        super(apl, filename, env);
    }

    public MWSL_RCPSchedulingLarge(BBTask apl, String file, Environment env, Hashtable<Long, CPU> in_cpuList) {
        super(apl, file, env, in_cpuList);
    }

    public AbstractTask getReadyTask() {
           //Freeリストから，もっともTlevelが小さいものを選択する．
           //もし複数あれば，その中から最もblevelの大きいものをReadyタスクとする．
           Iterator<Long> freeIte = this.freeIDList.iterator();
           CustomIDSet readySet = new CustomIDSet();

           ArrayList<AbstractTask> array = new ArrayList<AbstractTask>();
           while (freeIte.hasNext()) {
               Long freeID = freeIte.next();
               AbstractTask freeTask = this.retApl.findTaskByLastID(freeID);
               array.add(freeTask);
           }

           Object[] oa = array.toArray();
           //Tlevelの大きい順→blevelの大きい順にソートする．
           Arrays.sort(oa, new TaskPriorityLargeTlevelComparator());
           //先頭のタスクをレディタスクとする．
           AbstractTask tmpReadyTask = this.retApl.findTaskByLastID(((AbstractTask) oa[0]).getIDVector().get(1));
           int cnt = array.size();
           long pTlevel = tmpReadyTask.getPriorityTlevel();
           long pBlevel = tmpReadyTask.getPriorityBlevel();

           AbstractTask readyTask = tmpReadyTask;
           for (int i = 1; i < cnt; i++) {
               AbstractTask tmpTask = this.retApl.findTaskByLastID(((AbstractTask) oa[i]).getIDVector().get(1));
               if (tmpTask.getPriorityTlevel() == pTlevel) {
                   if (pBlevel < tmpTask.getPriorityBlevel()) {
                       readyTask = tmpTask;
                   } else {
                       break;
                   }
               }
           }

           TaskCluster cluster = this.retApl.findTaskCluster(readyTask.getClusterID());
           LinkedList<Long> schedList = cluster.getScheduledTaskList();
           //クラスタの中に，スケジュール順を追加しておく．
           schedList.add(readyTask.getIDVector().get(1));

           //そして，FREEからレディタスクを削除する．
           this.freeIDList.remove(readyTask.getIDVector().get(1));
           //未スケジュールタスクリストからレディタスクを削除する．
           this.unScheduledIDList.remove(readyTask.getIDVector().get(1));

           return readyTask;
       }

}
