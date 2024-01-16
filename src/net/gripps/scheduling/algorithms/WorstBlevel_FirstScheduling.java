package net.gripps.scheduling.algorithms;

import net.gripps.clustering.common.aplmodel.BBTask;
import net.gripps.clustering.common.aplmodel.TaskCluster;
import net.gripps.clustering.common.aplmodel.AbstractTask;
import net.gripps.environment.Environment;
import net.gripps.scheduling.common.TaskWorstBlevelComparator;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by IntelliJ IDEA.
 * User: kanemih
 * Date: 2008/11/06
 * Time: 3:14:40
 * To change this template use File | Settings | File Templates.
 */
public class WorstBlevel_FirstScheduling extends Blevel_FirstScheduling{
    public WorstBlevel_FirstScheduling(String filename, BBTask apl, Environment env) {
        super(filename, apl, env);
    }

    public WorstBlevel_FirstScheduling() {
        super();
    }

    public void initialize() {
        super.initialize();    //To change body of overridden methods use File | Settings | File Templates.
    }

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
            Arrays.sort(oa, new TaskWorstBlevelComparator());

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
