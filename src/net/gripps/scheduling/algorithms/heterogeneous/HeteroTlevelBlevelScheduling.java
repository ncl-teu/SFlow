package net.gripps.scheduling.algorithms.heterogeneous;

import net.gripps.clustering.common.aplmodel.AbstractTask;
import net.gripps.clustering.common.aplmodel.BBTask;
import net.gripps.clustering.common.aplmodel.CustomIDSet;
import net.gripps.clustering.common.aplmodel.TaskCluster;
import net.gripps.environment.CPU;
import net.gripps.environment.Environment;
import net.gripps.scheduling.common.TaskPriorityBlevelComparator;
import net.gripps.scheduling.common.TaskPriorityTlevelBlevelComparator;

import java.util.*;

/**
 * Created by kanemih on 2015/05/17.
 */
public class HeteroTlevelBlevelScheduling extends HeteroBlevelScheduling {

    public HeteroTlevelBlevelScheduling(BBTask apl, String filename, Environment env) {
        super(apl, filename, env);
    }

    public HeteroTlevelBlevelScheduling(BBTask apl, String file, Environment env, Hashtable<Long, CPU> in_cpuList) {
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
        //blevelの大きい順にソートする．もし同じであれば，tlevelの小さい方とする．
        // Arrays.sort(oa, new TaskPriorityTlevelComparator());
        Arrays.sort(oa, new TaskPriorityTlevelBlevelComparator());
        //先頭のタスクをレディタスクとする．
        AbstractTask tmpReadyTask = this.retApl.findTaskByLastID(((AbstractTask) oa[0]).getIDVector().get(1));
        int cnt = array.size();
        long pTlevel = tmpReadyTask.getPriorityTlevel();
        long pBlevel = tmpReadyTask.getPriorityBlevel();

        AbstractTask readyTask = tmpReadyTask;
        for (int i = 1; i < cnt; i++) {
            AbstractTask tmpTask = this.retApl.findTaskByLastID(((AbstractTask) oa[i]).getIDVector().get(1));
            if (tmpTask.getPriorityTlevel()+tmpTask.getPriorityBlevel() == pTlevel+ pBlevel) {
                if (pTlevel > tmpTask.getPriorityTlevel()) {
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
