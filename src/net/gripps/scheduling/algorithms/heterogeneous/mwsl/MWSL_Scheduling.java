package net.gripps.scheduling.algorithms.heterogeneous.mwsl;

import net.gripps.clustering.common.aplmodel.*;
import net.gripps.environment.CPU;
import net.gripps.environment.Environment;
import net.gripps.scheduling.algorithms.heterogeneous.heft.HEFT_Algorithm;

import java.util.*;

/**
 * Author: H. Kanemitsu
 * Date: 14/09/13
 */
public class MWSL_Scheduling extends HEFT_Algorithm {

    protected PriorityQueue<AbstractTask> taskLevelQueue;


    public MWSL_Scheduling(BBTask apl, String file, Environment env) {
        super(apl, file, env);

    }

    public MWSL_Scheduling(BBTask apl, String file, Environment env, Hashtable<Long, CPU> in_cpuList) {
        super(apl, file, env, in_cpuList);

    }


    public BBTask process() {
        try {
            this.initialize();
            //this.prepare();

            //this.wcp = this.calcWCP();

            long start = System.currentTimeMillis();
            //メイン処理
            this.mainProcess();
            long end = System.currentTimeMillis();
            retApl.setProcessTime((end - start));

           // System.out.println("MWSL:" + retApl.getProcessTime());

            //後処理を行う．
            this.postProcess();
            AbstractTask endTask = this.retApl.findTaskByLastID(this.retApl.getEndTask().get(1));
            TaskCluster endCluster = this.retApl.findTaskCluster(endTask.getClusterID());
            CPU cpu = endCluster.getCPU();

            long makeSpan = endTask.getStartTime() + endTask.getMaxWeight() / cpu.getSpeed();
            this.retApl.setMakeSpan(makeSpan);


            return this.retApl;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public void mainProcess() {

        long O_EFT = 0;

        /**
         * スケジュールリストが空にならない間のループ
         */
        while (!this.taskLevelQueue.isEmpty()) {
            //当該タスクの先行タスクがスケジュールされているかわからないので，
            //とりあえずpeekする（削除はしない）
            Object[] oa = this.taskLevelQueue.toArray();
            //一つのクラスタ内で，タスクのtlevel順にソートする．
            //Arrays.sort(oa, new MWSL_TlevelComparator());
          Arrays.sort(oa, new WSLComparator());
           // Arrays.sort(oa, new MWSL_BlevelComparator());

            int len = oa.length;
            AbstractTask assignTask = null;

            //キュー内のタスクに対するループ
            for (int i = 0; i < len; i++) {
                boolean isAllScheduled = true;

                AbstractTask maxLevelTask = (AbstractTask) oa[i];
                for (int j = 1; j < len; j++) {
                    AbstractTask tmpTask = (AbstractTask) oa[j];
                    if (tmpTask.getTlevel() == maxLevelTask.getTlevel()) {
                        if (tmpTask.getBlevel() > maxLevelTask.getBlevel()) {
                            maxLevelTask = tmpTask;
                        }

                    } else {
                        break;
                    }
                }
                Iterator<DataDependence> dpredIte = maxLevelTask.getDpredList().iterator();
                if (maxLevelTask.getDpredList().isEmpty()) {
                    isAllScheduled = true;
                    assignTask = maxLevelTask;
                    break;
                }

                while (dpredIte.hasNext()) {
                    DataDependence dpred = dpredIte.next();
                    AbstractTask dpredTask = this.retApl.findTaskByLastID(dpred.getFromID().get(1));
                    if (this.scheduledTaskSet.contains(dpredTask.getIDVector().get(1))) {

                    } else {
                        isAllScheduled = false;
                        break;
                    }
                }
                if (!isAllScheduled) {
                    continue;
                } else {
                    assignTask = maxLevelTask;
                    break;
                }
            }
            this.taskLevelQueue.remove(assignTask);

            //各CPUについて，タスクtを実行した場合のEFTを計算する．
            //EFTが最も小さくなるようなCPUに割り当てる．
            CPU assignedCPU = this.retApl.findTaskCluster(assignTask.getClusterID()).getCPU();
            long start_time = this.calcEST2(assignTask, assignedCPU);
            assignTask.setStartTime(start_time);
            //assignTask.setTlevel(start_time);
            //System.out.println("Task: "+assignTask.getIDVector().get(1) + "-> CPU:"+assignedCPU.getCpuID());
            //taskをCPUへ割り当てる処理．
            this.assignProcess(assignTask, assignedCPU);
        }


    }

    public TaskCluster assignProcess(AbstractTask task, CPU cpu) {
        /*
        if(cpu.getTaskClusterID() < 0){
            cpu.setTaskClusterID(task.getClusterID());
            TaskCluster cls = this.retApl.findTaskCluster(task.getClusterID());
            cls.setCPU(cpu);

        }else{

            TaskCluster pivot = this.retApl.findTaskCluster(cpu.getTaskClusterID());
            pivot.addTask(task.getIDVector().get(1));
            TaskCluster target = this.retApl.findTaskCluster(task.getClusterID());

            this.updateOutSet(pivot, target);
            //InSetを更新する（後のレベル値の更新のため）
            this.updateInSet(pivot, target);
            this.retApl.removeTaskCluster(task.getClusterID());
            task.setClusterID(pivot.getClusterID());


            pivot.setCPU(cpu);

        } */

        //CPUの実行終了時刻を更新する．
        long cpu_finish_time = task.getStartTime() + task.getMaxWeight() / cpu.getSpeed();
        cpu.setFinishTime(cpu_finish_time);
        TaskCluster cluster = this.retApl.findTaskCluster(task.getClusterID());

        //スケジュール済みセットにタスクを追加する．
        this.scheduledTaskSet.add(task.getIDVector().get(1));

        //タスクの後続タスクのtlevelを更新する．
        /*Iterator<DataDependence> dsucIte = task.getDsucList().iterator();
        while(dsucIte.hasNext()){
            DataDependence dsuc = dsucIte.next();
            AbstractTask sucTask = this.retApl.findTaskByLastID(dsuc.getToID().get(1));
            TaskCluster sucCluster = this.retApl.findTaskCluster(sucTask.getClusterID());
            long nw_time = 0;
           if(sucCluster.getCPU().getMachineID() == cluster.getCPU().getMachineID()){
                //同一クラスタの場合の計算
                if(sucTask.getClusterID() == task.getClusterID()){

                }

            }else{
                nw_time = this.getNWTime(task.getIDVector().get(1), sucTask.getIDVector().get(1), dsuc.getMaxDataSize(),
                        this.env.getNWLink(cluster.getCPU().getCpuID(), sucCluster.getCPU().getCpuID()));
            }

        }*/

        //キューの更新処理
        Iterator<DataDependence> sucIte = task.getDsucList().iterator();
        while (sucIte.hasNext()) {
            DataDependence dsuc = sucIte.next();
            AbstractTask t = this.retApl.findTaskByLastID(dsuc.getToID().get(1));
            //tの先行タスクを見る．
            Iterator<DataDependence> dpred = t.getDpredList().iterator();
            Iterator<DataDependence> dpred2 = t.getDpredList().iterator();
            boolean isAllScheduled = true;
            while (dpred.hasNext()) {
                DataDependence sucDpred = dpred.next();
                AbstractTask sucDpredTask = this.retApl.findTaskByLastID(sucDpred.getFromID().get(1));
                if (this.scheduledTaskSet.contains(sucDpredTask.getIDVector().get(1))) {
                    continue;
                } else {
                    isAllScheduled = false;
                    break;
                }
            }
            if (isAllScheduled) {
                this.taskLevelQueue.add(t);
                TaskCluster cls = this.retApl.findTaskCluster(t.getClusterID());
                //tのtlevelを更新する．
                long task_finishTime = task.getStartTime() + task.getMaxWeight() / cpu.getSpeed();

                long in_tlevel = 0;
                long global_in_tlevel = 0;
                long out_tlevel = 0;
                long sumValue = 0;
                long clsSize = this.calculateSumValue(cls.getTaskSet());
                //tの先行タスクたちに対するループ
                boolean isCalc = false;
                while (dpred2.hasNext()) {
                    DataDependence dd = dpred2.next();
                    //tの先行タスクを取得
                    AbstractTask tPredTask = this.retApl.findTaskByLastID(dd.getFromID().get(1));
                    TaskCluster tpredCluster = this.retApl.findTaskCluster(tPredTask.getClusterID());


                    if (cls.getCPU().getMachineID() == tpredCluster.getCPU().getMachineID()) {
                        if (cls.getClusterID() == tpredCluster.getClusterID()) {
                            //同一クラスタであれば，（全要素数 - tのdestタスク - スケジュール済タスク）分の処理時間
                            // + taskの開始時刻を，tの新たなtlevelとする．
                            Iterator<Long> taskIte = cls.getTaskSet().iterator();
                            if(!isCalc){
                                while (taskIte.hasNext()) {
                                    Long id = taskIte.next();
                                    AbstractTask tt = this.retApl.findTaskByLastID(id);
                                    //もしtのdestであれば，無視
                                    if (t.getDestTaskSet().contains(id)) {
                                        continue;
                                    } else {
                                        //dのdestでなく，かつスケジュール済みタスクであれば，
                                        if (this.scheduledTaskSet.contains(id)) {
                                            long tmpTlevel = tt.getStartTime() + tt.getMaxWeight() / cls.getCPU().getSpeed();
                                            if (in_tlevel <= tmpTlevel) {
                                                in_tlevel = tmpTlevel;
                                            }
                                        } else {
                                            //未スケジュール，かつtのdestではない場合，＋する．
                                            sumValue += tt.getMaxWeight() / cls.getCPU().getSpeed();
                                        }
                                    }

                                }
                                in_tlevel += sumValue;
                                isCalc = true;
                            }
                        } else {
                            //異なるクラスタである（が，マシンは同じ）
                            if (cls.getTop_Set().contains(t.getIDVector().get(1))) {
                                //t自身がtopタスクであれば，外側からの到着時刻を取得する．
                                long tmpOutTlevel = tPredTask.getStartTime() + tPredTask.getMaxWeight() / tpredCluster.getCPU().getSpeed();
                                if (out_tlevel <= tmpOutTlevel) {
                                    out_tlevel = tmpOutTlevel;
                                }

                            } else {
                                //tはtopではない場合はスキップ
                                continue;
                            }

                        }
                    } else {
                        if (cls.getTop_Set().contains(t.getIDVector().get(1))) {
                            //t自身がtopタスクであれば，外側からの到着時刻を取得する．
                            long tmpOutTlevel = tPredTask.getStartTime() + tPredTask.getMaxWeight() / tpredCluster.getCPU().getSpeed() +
                                    this.getNWTime(tPredTask.getIDVector().get(1), t.getIDVector().get(1), dd.getMaxDataSize(),
                                            this.env.getNWLink(tpredCluster.getCPU().getCpuID(), cls.getCPU().getCpuID()));
                            if (out_tlevel <= tmpOutTlevel) {
                                out_tlevel = tmpOutTlevel;
                            }

                        } else {
                            //tはtopではない場合はスキップ
                            continue;
                        }

                    }

                }
                t.setTlevel(Math.max(in_tlevel, out_tlevel));
            }

        }

        cpu.getFtQueue().add(task);
        cpu.setFinishTime(cpu.getEndTime());

        return this.retApl.findTaskCluster(cpu.getTaskClusterID());
    }

    /**
     * 初期化処理を行います．
     * 各タスクについて，全CPUで実行した場合の平均時間をセットする．
     */
    public void initialize() {
        //タスクキューの初期化
        this.taskLevelQueue = new PriorityQueue<AbstractTask>(5,new WSLComparator() );
        //this.taskLevelQueue = new PriorityQueue<AbstractTask>(5, new MWSL_TlevelComparator());
       // this.taskLevelQueue = new PriorityQueue<AbstractTask>(5, new MWSL_BlevelComparator());

        //STARTタスクをキューにいれる．
        Iterator<Long> startSet = this.retApl.getStartTaskSet().iterator();
        while (startSet.hasNext()) {
            Long startID = startSet.next();
            AbstractTask startTask = this.retApl.findTaskByLastID(startID);
            this.taskLevelQueue.add(startTask);
        }

    }


}
