package net.gripps.scheduling.algorithms.heterogeneous.msl;

import net.gripps.clustering.common.aplmodel.AbstractTask;
import net.gripps.clustering.common.aplmodel.BBTask;
import net.gripps.clustering.common.aplmodel.DataDependence;
import net.gripps.clustering.common.aplmodel.TaskCluster;
import net.gripps.environment.CPU;
import net.gripps.environment.Environment;
import net.gripps.scheduling.algorithms.heterogeneous.heft.BlevelComparator;
import net.gripps.scheduling.algorithms.heterogeneous.heft.HEFT_Algorithm;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.PriorityQueue;

/**
 * Created by kanemih on 2015/11/08.
 */
public class MSL_Algorithm extends HEFT_Algorithm {

    /**
     * ランクの大きい順に格納している優先度つきキュー
     */
    protected PriorityQueue<AbstractTask> readyList;

    public MSL_Algorithm(BBTask apl, String file, Environment env) {
        super(apl, file, env);

    }

    public MSL_Algorithm(BBTask apl, String file, Environment env, Hashtable<Long, CPU> in_cpuList) {
        super(apl, file, env, in_cpuList);
    }

    /**
     * MET(Mean Execution Time)とTCT(Total Communication Time)を用いて
     * 優先度を設定する．
     */
    public void mainProcess() {
        long EFT = 0;
        while (!this.readyList.isEmpty()) {
            Iterator<CPU> cpuIte = this.cpuTable.values().iterator();

            //当該タスクの先行タスクがスケジュールされているかわからないので，
            //とりあえずpeekする（削除はしない）
            // AbstractTask maxBlevelTask = this.blevelQueue.poll();
            Object[] oa = this.readyList.toArray();
            //一つのクラスタ内で，タスクのtlevel順にソートする．
            Arrays.sort(oa, new MSLComparator());
            int len = oa.length;
            //Iterator<AbstractTask> taskIte = this.blevelQueue.iterator();

            AbstractTask assignTask = null;
            //while(taskIte.hasNext()){
            for (int i = 0; i < len; i++) {
                boolean isAllScheduled = true;
                // AbstractTask maxBlevelTask = taskIte.next();
                AbstractTask maxRankTask = (AbstractTask) oa[i];
                Iterator<DataDependence> dpredIte = maxRankTask.getDpredList().iterator();
                if (maxRankTask.getDpredList().isEmpty()) {
                    isAllScheduled = true;
                    assignTask = maxRankTask;
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
                    assignTask = maxRankTask;
                    break;
                }
            }
            this.readyList.remove(assignTask);

            //各CPUについて，タスクtを実行した場合のEFTを計算する．
            //EFTが最も小さくなるようなCPUに割り当てる．
            EFT = 1000000000;
            long start_time = 0;
            CPU assignedCPU = null;
            while (cpuIte.hasNext()) {
                CPU cpu = cpuIte.next();
                long tmpStartTime = this.calcEST2(assignTask, cpu);
                long tmpEFT = tmpStartTime + assignTask.getMaxWeight() / cpu.getSpeed();
                if (EFT >= tmpEFT) {
                    EFT = tmpEFT;
                    assignedCPU = cpu;
                    start_time = tmpStartTime;
                }
            }
            assignTask.setStartTime(start_time);
            //System.out.println("Task: "+assignTask.getIDVector().get(1) + "-> CPU:"+assignedCPU.getCpuID());
            //taskをCPUへ割り当てる処理．
            this.assignProcess(assignTask, assignedCPU);
        }


    }

    public TaskCluster assignProcess(AbstractTask task, CPU cpu){
        //もしCPUに，タスククラスタが未割り当てならば，当該タスクのクラスタID
        //をセットする．



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

        }

            //CPUの実行終了時刻を更新する．
            //long cpu_finish_time = task.getStartTime() + task.getMaxWeight()/cpu.getSpeed();
            //cpu.setFinishTime(cpu_finish_time);
        cpu.getFtQueue().add(task);
        cpu.setFinishTime(cpu.getEndTime());

        //スケジュール済みセットにタスクを追加する．
        this.scheduledTaskSet.add(task.getIDVector().get(1));

        //readyListの更新処理
        Iterator<DataDependence> sucIte = task.getDsucList().iterator();
        while(sucIte.hasNext()){
            DataDependence dsuc = sucIte.next();
            AbstractTask t = this.retApl.findTaskByLastID(dsuc.getToID().get(1));
            //tの先行タスクを見る．
            Iterator<DataDependence> dpred = t.getDpredList().iterator();
            boolean isAllScheduled = true;
            while(dpred.hasNext()){
                DataDependence sucDpred = dpred.next();
                AbstractTask sucDpredTask = this.retApl.findTaskByLastID(sucDpred.getFromID().get(1));
                if(this.scheduledTaskSet.contains(sucDpredTask.getIDVector().get(1))){
                    continue;
                }else{
                    isAllScheduled = false;
                    break;
                }
            }
            if(isAllScheduled){
                this.readyList.add(t);
            }

        }
        return  this.retApl.findTaskCluster(cpu.getTaskClusterID());


    }

    public void mslPrepare() {
        //MET+TCTを行う．
        Iterator<AbstractTask> taskIte = this.retApl.getTaskList().values().iterator();

        //タスクに対するループ
        while (taskIte.hasNext()) {
            long incom = 0;
            long outcom = 0;
            long rank = 0;
            AbstractTask task = taskIte.next();
            //まずは入力辺をしらべる．
            Iterator<DataDependence> dpredIte = task.getDpredList().iterator();
            while (dpredIte.hasNext()) {
                DataDependence dpred = dpredIte.next();
                incom += dpred.getAve_comTime();
            }

            //次に，出辺を調べる．
            Iterator<DataDependence> dsucIte = task.getDsucList().iterator();
            while (dsucIte.hasNext()) {
                DataDependence dsuc = dsucIte.next();
                outcom += dsuc.getAve_comTime();
            }
            rank = task.getAve_procTime() + incom + outcom;
            task.setMsl_rank(rank);
            if(task.getDpredList().isEmpty()){
                this.readyList.add(task);
            }

        }
    //    System.out.println("test");



    }

    public BBTask process() {
        this.readyList = new PriorityQueue<AbstractTask>(5, new MSLComparator());

        try {
            //初期化処理
            this.initialize();
            this.prepare();
            //MSL用の初期化処理
            this.mslPrepare();


            //this.wcp = this.calcWCP();

            long start = System.currentTimeMillis();
            //メイン処理
            this.mainProcess();
            long end = System.currentTimeMillis();
            retApl.setProcessTime((end - start));

            //System.out.println("PEFT:"+retApl.getProcessTime());
            //System.out.println("カウント:"+this.count);

            //System.out.println("時間:"+this.totalTime/1000);

            //System.out.println("time1:"+this.time1/1000 + "/time2:"+this.time2/1000+"/time3:"+this.time3/1000);
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
}

