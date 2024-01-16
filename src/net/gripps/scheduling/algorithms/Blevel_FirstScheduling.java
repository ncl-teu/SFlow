package net.gripps.scheduling.algorithms;

import net.gripps.scheduling.AbstractTaskSchedulingAlgorithm;
import net.gripps.scheduling.common.TaskPriorityBlevelComparator;
import net.gripps.clustering.common.aplmodel.*;
import net.gripps.environment.Environment;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Author: H. Kanemitsu
 * Date: 2008/10/25
 * blevel-firstなスケジューリング方針であり，
 * Sarkar's Scheduling Algorithmです．
 */
public class Blevel_FirstScheduling extends AbstractTaskSchedulingAlgorithm {
    /**
     * @param filename
     * @param apl
     * @param env
     */
    public Blevel_FirstScheduling(String filename, BBTask apl, Environment env) {
        super(filename, apl, env);

    }

    public Blevel_FirstScheduling() {
    }


    

    /**
     * スケジュール前処理を行います．
     * ここでは，DAG内のblevel値の計算を行います．
     */
    public void initialize() {

        int size = this.retApl.getTaskList().size();
        //ENDタスクを取得する．
        AbstractTask endTask = this.retApl.findTaskByLastID(new Long(size));

        //priorityTlevelをセットする．
        this.calcPriorityTlevel(endTask, false);

        CustomIDSet startIDSet = this.retApl.getStartTaskSet();
        Iterator<Long> startTaskIte = startIDSet.iterator();

        //STARTタスクに対するループです．
        while (startTaskIte.hasNext()) {
            Long startTaskID = startTaskIte.next();
            //STARTタスクを取得する．
            AbstractTask startTask = this.retApl.findTaskByLastID(startTaskID);
            //各スタートタスクに対し，PriorityBlevelをセットする．
            this.calcPriorityBlevel(startTask, false);
        }

    }


    /**
     * scheduleメソッド実装です．
     * Scheduled DAGを作るのが目的です．
     * Sarkar'sアルゴリズムでは，同一タスククラスタ内ではblevel-firstです．
     */
    public BBTask schedule() {
        //まずは，スケジュール優先度の割り当て
        this.initialize();

        //そして，各クラスタに対して，blevelの大きい順に実行させるようにする．
        //そのためには，blevelの大きい順のリストを作る必要がある．
        Iterator<TaskCluster> clusterIte = this.retApl.clusterIterator();
        //各タスククラスタに対するループ処理
        while (clusterIte.hasNext()) {
            TaskCluster cluster = clusterIte.next();
            //タスクリストを取得する．
            Iterator<Long> taskIDIte = cluster.getTaskSet().iterator();
            ArrayList<AbstractTask> array = new ArrayList<AbstractTask>();

            while (taskIDIte.hasNext()) {
                Long taskID = taskIDIte.next();
                AbstractTask task = this.retApl.findTaskByLastID(taskID);
                array.add(task);
            }

            Object[] oa = array.toArray();
            //ソートする．
            Arrays.sort(oa, new TaskPriorityBlevelComparator());

            //そして，データ依存辺を仮想的に加える．
            int len = oa.length;
            for (int i = 0; i < len; i++) {
                if (i == len - 1) {
                    break;
                }
                Long predID = ((AbstractTask) oa[i]).getIDVector().get(1);
                Long sucID = ((AbstractTask) oa[i + 1]).getIDVector().get(1);

                AbstractTask predTask = this.retApl.findTaskByLastID(predID);
                AbstractTask sucTask = this.retApl.findTaskByLastID(sucID);

                this.addVirtualEdge(predTask, sucTask);

            }
        }
        this.calcMakeSpan();
        return this.retApl;
    }
}
