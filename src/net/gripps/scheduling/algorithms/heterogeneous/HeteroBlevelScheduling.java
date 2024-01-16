package net.gripps.scheduling.algorithms.heterogeneous;

import net.gripps.clustering.common.aplmodel.*;
import net.gripps.environment.CPU;
import net.gripps.environment.Environment;
import net.gripps.scheduling.algorithms.heterogeneous.mwsl.MWSL_RCPScheduling;
import net.gripps.scheduling.common.TaskPriorityBlevelComparator;
import net.gripps.scheduling.common.TaskPriorityTlevelComparator;

import java.util.*;

/**
 * Created by kanemih on 2015/05/16.
 */
public class HeteroBlevelScheduling extends MWSL_RCPScheduling{
    public HeteroBlevelScheduling(BBTask apl, String filename, Environment env) {
        super(apl, filename, env);
    }

    public HeteroBlevelScheduling(BBTask apl, String file, Environment env, Hashtable<Long, CPU> in_cpuList) {
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
        Arrays.sort(oa, new TaskPriorityBlevelComparator());
        //先頭のタスクをレディタスクとする．
        AbstractTask tmpReadyTask = this.retApl.findTaskByLastID(((AbstractTask) oa[0]).getIDVector().get(1));
        int cnt = array.size();
        long pTlevel = tmpReadyTask.getPriorityTlevel();
        long pBlevel = tmpReadyTask.getPriorityBlevel();

        AbstractTask readyTask = tmpReadyTask;
        for (int i = 1; i < cnt; i++) {
            AbstractTask tmpTask = this.retApl.findTaskByLastID(((AbstractTask) oa[i]).getIDVector().get(1));
            if (tmpTask.getPriorityBlevel() == pBlevel) {
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

    /**
     * 後続タスクたちのTlevelを更新します．
     * もしすべての入力辺がチェック済みであれば，Freeリストへ追加します．
     *
     * @param task
     */
    public void updateSucTasks(AbstractTask task) {
        Long clusterID = task.getClusterID();
        TaskCluster cluster = this.retApl.findTaskCluster(clusterID);

        //後続タスクに対するループ
        Iterator<DataDependence> dsucIte = task.getDsucList().iterator();
        while (dsucIte.hasNext()) {
            DataDependence dsuc = dsucIte.next();
            //後続タスクを取得する．
            AbstractTask sucTask = this.retApl.findTaskByLastID(dsuc.getToID().get(1));
            //後続タスクの入力辺をチェック済みとする．
            DataDependence dpred = sucTask.findDDFromDpredList(task.getIDVector(), sucTask.getIDVector());
            dpred.setReady(true);
            //もし後続タスクがFREEとなれば，FREEリストへ追加する
            if ((this.isFreeTask(sucTask)) && (this.unScheduledIDList.contains(sucTask.getIDVector().get(1)))) {
                this.freeIDList.add(sucTask.getIDVector().get(1));
            }
            //後続タスクのレベル値を更新する．
            this.updatePriorityTlevel(sucTask);


        }


    }


}
