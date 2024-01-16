package net.gripps.scheduling.algorithms.heterogeneous.peft;

import net.gripps.clustering.common.Constants;
import net.gripps.clustering.common.aplmodel.*;
import net.gripps.clustering.tool.Calc;
import net.gripps.environment.CPU;
import net.gripps.environment.Environment;
import net.gripps.scheduling.algorithms.heterogeneous.heft.BlevelComparator;
import net.gripps.scheduling.algorithms.heterogeneous.heft.HEFT_Algorithm;

import java.util.*;

/**
 * Author: H. Kanemitsu
 * Date: 14/09/14
 */
public class PEFT_Algorithm extends HEFT_Algorithm {

    /**
     * Optimistic Cost Table
     * (タスクID, CPU ID）のフォーマットである．
     */
    // private long[][] octTable;
    //private OCTTable octTable;

    private long count = 0;

    private long totalTime = 0;

    private PriorityQueue<AbstractTask> readyList;

    private long time1 = 0;
    private long time2 = 0;

    private long time3 = 0;

    private long time4 = 0;

    private Hashtable<Long, CPU> actualCPUTable;

    private long averageLink = 0;

    private long averageSpeed = 0;

    private CustomIDSet set;



    public PEFT_Algorithm(BBTask apl, String file, Environment env) {
        super(apl, file, env);
        this.actualCPUTable = this.env.getCpuList();
        this.set = new CustomIDSet();


    }

    public PEFT_Algorithm(BBTask apl, String file, Environment env, Hashtable<Long, CPU> in_cpuList) {
        super(apl, file, env, in_cpuList);
        this.actualCPUTable = this.env.getCpuList();
        this.set = new CustomIDSet();

    }



    /**
     *
     * @param sucTask 後続タスク
     * @param cpu 外側のCPU
     * @param aveComTime 平均通信時間
     * @return
     */
    public long calcOCT(AbstractTask sucTask, CPU cpu, long aveComTime, CustomIDSet set) {
        if(set.contains(sucTask.getIDVector().get(1))){
            return 0;
        }

        //既に計算済みであれば，何もせず終了
        if(sucTask.getAve_oct() != Constants.INFINITY) {
            set.add(sucTask.getIDVector().get(1));
            return 0;
        }
        long totalOCT = 0;

        if(sucTask.getDsucList().isEmpty()){
            set.add(sucTask.getIDVector().get(1));

            sucTask.setAve_oct(0);
            if(sucTask.getOctMap().size() == this.actualCPUTable.size()){
                return 0;
            }else{
                Iterator<CPU> cIte = this.cpuTable.values().iterator();
                while(cIte.hasNext()){
                    CPU proc = cIte.next();
                    sucTask.getOctMap().put(proc.getCpuID(), new Long(0));
                }
            }
            set.add(sucTask.getIDVector().get(1));



            return 0;
        }

        //sucTaskの外側のCPUの値を計算する．
        Iterator<CPU> cpuIte0 = this.actualCPUTable.values().iterator();
        while (cpuIte0.hasNext()) {

            CPU outerCPU = cpuIte0.next();
            /*if(sucTask.getDsucList().isEmpty()){
                totalOCT += 0;
                sucTask.getOctMap().put(outerCPU.getCpuID(), new Long(0));
                continue;
            }*/
          /*  if(sucTask.getOctMap().containsKey(cpu.getCpuID())) {
                totalOCT += sucTask.getOctMap().get(cpu.getCpuID()).longValue();
                continue;
            }*/
            Iterator<DataDependence> dsucIte = sucTask.getDsucList().iterator();
            long maxValue = 0;
            while (dsucIte.hasNext()) {
                DataDependence dsuc = dsucIte.next();
                AbstractTask sucSucTask = this.retApl.findTaskByLastID(dsuc.getToID().get(1));
                //後続タスクのOCTを計算する．
      //  System.out.println("From:"+sucTask.getIDVector().get(1).longValue()+"/"+sucTask.getOctMap().size() + "To:"+sucSucTask.getIDVector().get(1).longValue()+"/"+sucSucTask.getOctMap().size());
                this.calcOCT(sucSucTask, cpu, dsuc.getAve_comTime(), this.set);
                long minValue = this.getMinSucOCT(sucSucTask, outerCPU, dsuc.getAve_comTime());
//System.out.println("Test");
                if (minValue >= maxValue) {
                    maxValue = minValue;
                }

            }

            sucTask.getOctMap().put(outerCPU.getCpuID(), maxValue);
            totalOCT += maxValue;
        }
        double aveOCT = Calc.getRoundedValue(totalOCT/(double)this.cpuTable.size());
        sucTask.setAve_oct((long)aveOCT);
        set.add(sucTask.getIDVector().get(1));

        return 0;
    }


    public void mainProcess() {

        long O_EFT = 0;
        /**
         * スケジュールリストが空にならない間のループ
         */
        while (!this.readyList.isEmpty()) {
            Iterator<CPU> cpuIte = this.actualCPUTable.values().iterator();

            //当該タスクの先行タスクがスケジュールされているかわからないので，
            //とりあえずpeekする（削除はしない）
            Object[] oa = this.readyList.toArray();
            //一つのクラスタ内で，タスクのtlevel順にソートする．
            Arrays.sort(oa, new OCTComparator());
            int len = oa.length;


            AbstractTask assignTask = null;

            // while(taskIte.hasNext()){
            for (int i = 0; i < len; i++) {
                boolean isAllScheduled = true;

                // AbstractTask maxBlevelTask = taskIte.next();
                AbstractTask maxOCTTask = (AbstractTask) oa[i];
                Iterator<DataDependence> dpredIte = maxOCTTask.getDpredList().iterator();
                if (maxOCTTask.getDpredList().isEmpty()) {
                    isAllScheduled = true;
                    assignTask = maxOCTTask;
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
                    assignTask = maxOCTTask;
                    break;
                }
            }
            this.readyList.remove(assignTask);

            //各CPUについて，タスクtを実行した場合のEFTを計算する．
            //EFTが最も小さくなるようなCPUに割り当てる．
            O_EFT = Constants.MAXValue;
            long start_time = 0;
            CPU assignedCPU = null;
            while (cpuIte.hasNext()) {
                CPU cpu = cpuIte.next();
                long tmpStartTime = this.calcEST2(assignTask, cpu);
//                long tmpO_EFT = tmpStartTime + assignTask.getMaxWeight() / cpu.getSpeed() + this.octTable.get(assignTask.getIDVector().get(1), cpu.getCpuID());
                 long tmpO_EFT = tmpStartTime + assignTask.getMaxWeight() / cpu.getSpeed() +assignTask.getOctMap().get(cpu.getCpuID());


                if (O_EFT >= tmpO_EFT) {
                    O_EFT = tmpO_EFT;
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

    public TaskCluster assignProcess(AbstractTask task, CPU cpu) {
        //もしCPUに，タスククラスタが未割り当てならば，当該タスクのクラスタID
        //をセットする．


        if (cpu.getTaskClusterID() < 0) {
            cpu.setTaskClusterID(task.getClusterID());
            TaskCluster cls = this.retApl.findTaskCluster(task.getClusterID());
            cls.setCPU(cpu);

        } else {

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
        while (sucIte.hasNext()) {
            DataDependence dsuc = sucIte.next();
            AbstractTask t = this.retApl.findTaskByLastID(dsuc.getToID().get(1));
            //tの先行タスクを見る．
            Iterator<DataDependence> dpred = t.getDpredList().iterator();
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
                this.readyList.add(t);
            }

        }
        return this.retApl.findTaskCluster(cpu.getTaskClusterID());


    }


    public BBTask process() {
        try {
            //PEFT用の初期化処理
            long start = System.currentTimeMillis();

            this.initialize();
            long end = System.currentTimeMillis();
            retApl.setProcessTime((end - start));
            //System.out.println("時間:"+this.totalTime/1000);

            this.prepare();

            //this.wcp = this.calcWCP();

            this.wcp = this.calcWCP();
            this.retApl.setMinCriticalPath(this.wcp / this.maxSpeed);

            //メイン処理
            this.mainProcess();


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


    public void initialize() {

        double cpuSum = 0.0;

        this.readyList = new PriorityQueue<AbstractTask>(5, new OCTComparator());

        Iterator<CPU> cpuIte = this.actualCPUTable.values().iterator();
        //Iterator<CPU> cpuIte = this.env.getCpuList().values().iterator();
        while (cpuIte.hasNext()) {
            CPU cpu = cpuIte.next();
            cpuSum += Calc.getRoundedValue((double) 1 / cpu.getSpeed());
        }


        long taskSum = 0;
        long cpuNum = this.actualCPUTable.size();
        //long cpuNum = this.env.getCpuList().size();
        int taskNum = this.retApl.getTaskList().size();
        // this.octTable = new long[taskNum][(int) cpuNum];
       /* for (int i = 0; i < taskNum; i++) {
            for (int j = 0; j < cpuNum; j++) {
                //this.octTable[i][j] = -1;
            }
        }*/

        //全体の平均帯域幅を算出する．
        long[][] linkMX = this.env.getLinkMatrix();
        long len = linkMX[0].length;
        int cnt = 0;
        long totalBW = 0;
        for (int i = 0; i < len; i++) {
            for (int j = i + 1; j < len; j++) {
                if (linkMX[i][j] == -1) {
                    continue;
                } else if (!this.actualCPUTable.containsKey(new Long(i)) || (!this.actualCPUTable.containsKey(new Long(j)))) {
                    // }else if(!this.cpuTable.contains(i) || (!this.cpuTable.contains(j))){
                    continue;
                } else {
                    totalBW += linkMX[i][j];
                    cnt++;
                }
            }
        }
        long aveLink = totalBW / cnt;


        Iterator<AbstractTask> taskIte = this.retApl.getTaskList().values().iterator();
        //各タスクについて，平均実行時間をセットする．
        while (taskIte.hasNext()) {
            AbstractTask task = taskIte.next();
            double ave_time_double = task.getMaxWeight() * cpuSum / cpuNum;
            long ave_time = (long) ave_time_double;
            task.setAve_procTime(ave_time);

            Iterator<DataDependence> dpredIte = task.getDpredList().iterator();
            while (dpredIte.hasNext()) {
                DataDependence dpred = dpredIte.next();
                dpred.setAve_comTime(dpred.getMaxDataSize() / aveLink);
            }

            Iterator<DataDependence> dsucIte = task.getDsucList().iterator();
            while (dsucIte.hasNext()) {
                DataDependence dsuc = dsucIte.next();
                dsuc.setAve_comTime(dsuc.getMaxDataSize() / aveLink);
            }
            if (task.getDpredList().isEmpty()) {
                this.retApl.getStartTaskSet().add(task.getIDVector().get(1));
            }
        }

        Iterator<Long> startIte = this.retApl.getStartTaskSet().iterator();

        while (startIte.hasNext()) {
            AbstractTask startTask = this.retApl.findTaskByLastID(startIte.next());
            long totalOCT = 0;
            Iterator<CPU> cpuIte0 = this.actualCPUTable.values().iterator();
            while (cpuIte0.hasNext()) {
                CPU cpu = cpuIte0.next();
                Iterator<DataDependence> dsucIte = startTask.getDsucList().iterator();
                long maxValue = 0;
                while (dsucIte.hasNext()) {
                    DataDependence dsuc = dsucIte.next();
                    AbstractTask sucTask = this.retApl.findTaskByLastID(dsuc.getToID().get(1));
                    //後続タスクのOCTを計算する．
                    this.calcOCT(sucTask, cpu, dsuc.getAve_comTime(), this.set);
                    long minValue = this.getMinSucOCT(sucTask, cpu, dsuc.getAve_comTime());
                    if (minValue >= maxValue) {
                        maxValue = minValue;
                    }
                }
                startTask.getOctMap().put(cpu.getCpuID(), maxValue);
                totalOCT += maxValue;
            }
            double aveOCT = Calc.getRoundedValue(totalOCT/(double)this.actualCPUTable.size());
            startTask.setAve_oct((long)aveOCT);
            this.readyList.add(startTask);
        }

    }

    public long getMinSucOCT(AbstractTask task, CPU cpu, long aveCommTime) {
        Iterator<CPU> cpuIte = this.actualCPUTable.values().iterator();
        long minValue = Constants.MAXValue;
//System.out.println("ID:"+task.getIDVector().get(1).longValue()+":size:"+task.getOctMap().size());

        while (cpuIte.hasNext()) {
            CPU sucCPU = cpuIte.next();
            long comTime = 0;
            if (cpu.getCpuID() != sucCPU.getCpuID()) {
                comTime = aveCommTime;
            }
            long value = 0;
            if(task.getDsucList().isEmpty()){
                value =   task.getMaxWeight() / sucCPU.getSpeed() + comTime;
            }else{
                value = task.getOctMap().get(sucCPU.getCpuID()).longValue() + task.getMaxWeight() / sucCPU.getSpeed() + comTime;

            }
            if (value <= minValue) {
                minValue = value;
            }
        }

        return minValue;
    }


}

