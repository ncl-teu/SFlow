package net.gripps.scheduling.algorithms.heterogeneous.mwsl;

import net.gripps.clustering.common.aplmodel.*;
import net.gripps.clustering.tool.Calc;
import net.gripps.environment.CPU;
import net.gripps.environment.Environment;
import net.gripps.scheduling.algorithms.heterogeneous.heft.HEFT_Algorithm;
import net.gripps.scheduling.algorithms.heterogeneous.heft.StartTimeComparator;
import net.gripps.scheduling.common.TaskPriorityLargeTlevelComparator;
import net.gripps.scheduling.common.TaskPriorityTlevelComparator;

import java.util.*;

/**
 * Author: H. Kanemitsu
 * Date: 14/11/30
 */
public class MWSL_RCPScheduling extends HEFT_Algorithm {

    protected CustomIDSet underThresholdList;

    /**
     * FREEとなるタスクのリストです．
     */
    protected CustomIDSet freeIDList;

    protected long dwtime;

    /**
     * 未スケジュールのタスクリスト
     */
    protected CustomIDSet unScheduledIDList;

    public MWSL_RCPScheduling(BBTask apl, String filename, Environment env) {
        super(apl, filename, env);
        this.underThresholdList = new CustomIDSet();
        this.freeIDList = new CustomIDSet();
        this.unScheduledIDList = new CustomIDSet();
        this.dwtime = 0;

    }

    public MWSL_RCPScheduling(BBTask apl, String file, Environment env,
                              Hashtable<Long, CPU> in_cpuList) {
        super(apl, file, env, in_cpuList);
        this.underThresholdList = underThresholdList;
        this.freeIDList = new CustomIDSet();
        this.unScheduledIDList = new CustomIDSet();
        this.dwtime = 0;

    }

    public BBTask process() {
        //priorityTlevle, priorityBlevelの設定，
        //freeリスト，未スケジュールリストの初期化を行う．
        this.preProcess();
        this.initializedUnderThresholdList();
        //スケジューリング処理
        this.mainProcess();

        this.postProcess();
        AbstractTask endTask = this.retApl.findTaskByLastID(this.retApl.getEndTask().get(1));
        TaskCluster endCluster = this.retApl.findTaskCluster(endTask.getClusterID());
        CPU cpu = endCluster.getCPU();

        long makeSpan = endTask.getStartTime() + endTask.getMaxWeight() / cpu.getSpeed();
        this.retApl.setMakeSpan(makeSpan);


        return this.retApl;


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
     * @param task
     * @param recalculate
     * @return
     */
    public long calcPriorityBlevel(AbstractTask task, boolean recalculate) {
        LinkedList<DataDependence> DsucList = task.getDsucList();
        Iterator<DataDependence> dsucIte = DsucList.iterator();
        int size = DsucList.size();
        TaskCluster fromCluster = this.retApl.findTaskCluster(task.getClusterID());

        //もしすでにBlevelの値が入っていれば，そのまま返す．
        if (task.getPriorityBlevel() != -1) {
            if (!recalculate) {
                return task.getPriorityBlevel();
            }
        }

        //もし後続タスクがない場合，blevel=自分の命令数となる．
        long instruction = this.getInstrunction(task);
        if (DsucList.size() == 0) {
            long execTime = instruction / fromCluster.getCPU().getSpeed();
            task.setPriorityBlevel(execTime);
            return execTime;
        }

        long maxValue = 0;
        while (dsucIte.hasNext()) {
            DataDependence dd = dsucIte.next();
            //DataDependence dd = DsucList.get(i);
            Vector<Long> toid = dd.getToID();

            AbstractTask toTask = this.retApl.findTaskByLastID(toid.get(1));
            //toTaskの属するクラスタを取得する．
            TaskCluster toCluster = this.retApl.findTaskCluster(toTask.getClusterID());

            long nw_time = 0;
            if (fromCluster.getCPU().getMachineID() == toCluster.getCPU().getMachineID()) {

            } else {
                nw_time = this.getNWTime(dd.getFromID().get(1), dd.getToID().get(1), dd.getMaxDataSize(),
                        this.env.getNWLink(fromCluster.getCPU().getCpuID(), toCluster.getCPU().getCpuID()));
            }
            long toBlevel = (instruction / toCluster.getCPU().getSpeed()) + nw_time + this.calcPriorityBlevel(toTask, recalculate);

            if (maxValue <= toBlevel) {
                maxValue = toBlevel;
            }
        }
        task.setPriorityBlevel(maxValue);
        return maxValue;
    }


    /**
     * @param task
     * @param recalculate
     * @return
     */
    public long calcPriorityTlevel(AbstractTask task, boolean recalculate) {
        LinkedList<DataDependence> DpredList = task.getDpredList();
        Iterator<DataDependence> dpredIte = DpredList.iterator();
        int size = DpredList.size();
        TaskCluster fromCluster = this.retApl.findTaskCluster(task.getClusterID());

        //もしすでにTlevelの値が入っていれば，そのまま返す．
        if (task.getPriorityTlevel() != -1) {
            if (!recalculate) {
                return task.getPriorityTlevel();
            }
        }

        //もし後続タスクがない場合，blevel=自分の命令数となる．
        long instruction = this.getInstrunction(task);
        if (DpredList.size() == 0) {
            //long execTime = instruction / fromCluster.getCPU().getSpeed();
            task.setPriorityTlevel(0);
            this.retApl.getStartTaskSet().add(task.getIDVector().get(1));
            return 0;
        }

        long maxValue = 0;
        while (dpredIte.hasNext()) {
            DataDependence dd = dpredIte.next();
            //DataDependence dd = DsucList.get(i);
            Vector<Long> fromid = dd.getFromID();

            AbstractTask fromTask = this.retApl.findTaskByLastID(fromid.get(1));
            //fromTaskの属するクラスタを取得する．
            TaskCluster toCluster = this.retApl.findTaskCluster(fromTask.getClusterID());

            long nw_time = 0;
            if (fromCluster.getCPU().getMachineID() == toCluster.getCPU().getMachineID()) {

            } else {
                nw_time = this.getNWTime(dd.getFromID().get(1), dd.getToID().get(1), dd.getMaxDataSize(),
                        this.env.getNWLink(fromCluster.getCPU().getCpuID(), toCluster.getCPU().getCpuID()));
            }
            TaskCluster clster = this.retApl.findTaskCluster(fromTask.getClusterID());
            CPU cpu = clster.getCPU();
            long fromTlevel = this.calcPriorityTlevel(fromTask, recalculate) + fromTask.getMaxWeight() / cpu.getSpeed() + nw_time;

            if (maxValue <= fromTlevel) {
                maxValue = fromTlevel;
            }
        }
        task.setPriorityTlevel(maxValue);
        return maxValue;
    }

    public void preProcess() {
        //priorityTlevel, priorityBlevelをそれぞれセットする．
        this.initialize();
        Iterator<Long> startIDIte = this.retApl.getStartTaskSet().iterator();
        //STARTタスクたちに対するループ

        //まずは，STARTタスクをFREEタスクとする．
        while (startIDIte.hasNext()) {
            Long sID = startIDIte.next();
            this.freeIDList.add(sID);
        }

        //まずは全タスクを未スケジュールとする．
        Iterator<AbstractTask> taskIte = this.retApl.getTaskList().values().iterator();
        while (taskIte.hasNext()) {
            AbstractTask task = taskIte.next();
            this.unScheduledIDList.add(task.getIDVector().get(1));
        }


    }


    /**
     * @param cluster
     * @return
     */
    public boolean isAboveThreshold(TaskCluster cluster) {
        CustomIDSet IDSet = cluster.getTaskSet();
        Iterator<Long> ite = IDSet.iterator();
        long value = 0;

        while (ite.hasNext()) {
            Long id = ite.next();
            AbstractTask task = this.retApl.findTaskByLastID(id);
            value += this.getInstrunction(task);
        }
        CPU cpu = cluster.getCPU();
        double execTime = Calc.getRoundedValue((double) value / cpu.getSpeed());
        //割り当てられているCPUが，仮想CPUでない，かつクラスタ実行時間がr値以上である場合はtrue
        if ((execTime >= cpu.getThresholdTime()) && (!cpu.isVirtual())) {
            return true;

        } else {
            return false;
        }

    }

    /**
     * しきい値未満であるクラスタのリストを初期化する．
     */
    public void initializedUnderThresholdList() {
        Iterator<TaskCluster> clsIte = this.retApl.getTaskClusterList().values().iterator();
        while (clsIte.hasNext()) {
            TaskCluster cls = clsIte.next();
            if (!this.isAboveThreshold(cls)) {
                this.underThresholdList.add(cls.getClusterID());
            }

        }

    }

    public void mainProcess() {


        while (!this.unScheduledIDList.isEmpty()) {
            //Freeリストから，Readyタスクを取得する．
            AbstractTask assignTask = this.getReadyTask();
            TaskCluster cluster = this.retApl.findTaskCluster(assignTask.getClusterID());
            CPU assignedCPU = null;
            long start_time = 0;

            if (this.sched_mode == 0) {
                //既にしきい値を超えていれば，当該CPUにおける挿入ベースの開始時刻決めの処理にうつる．
                if (this.isAboveThreshold(cluster)) {
                    assignedCPU = this.retApl.findTaskCluster(assignTask.getClusterID()).getCPU();
                    start_time = this.calcEST2(assignTask, assignedCPU);
                } else {
                    //しきい値未満であれば，挿入ベースで割当先CPUを決めて，開始時刻を決める．
                    Iterator<TaskCluster> clsIte = this.retApl.getTaskClusterList().values().iterator();
                    long EFT = (long)Double.POSITIVE_INFINITY;
                    while (clsIte.hasNext()) {
                        TaskCluster cls = clsIte.next();
                        CPU c = cls.getCPU();

                   /* if(cls.getCPU().getCpuID().longValue() == cluster.getCPU().getCpuID().longValue()){
                        continue;
                    }else{
                    */
                        long tmpStartTime = this.calcEST2(assignTask, c);
                        long tmpEFT = tmpStartTime + assignTask.getMaxWeight() / c.getSpeed();
                        if (tmpEFT <= EFT) {
                            EFT = tmpEFT;
                            assignedCPU = c;
                            start_time = tmpStartTime;
                            // }
                        }
                    }
                }
            } else {
                Iterator<TaskCluster> clsIte = this.retApl.getTaskClusterList().values().iterator();
                long EFT = (long)Double.POSITIVE_INFINITY;
                while (clsIte.hasNext()) {
                    TaskCluster cls = clsIte.next();
                    CPU c = cls.getCPU();

                   /* if(cls.getCPU().getCpuID().longValue() == cluster.getCPU().getCpuID().longValue()){
                        continue;
                    }else{
                    */
                    long tmpStartTime = this.calcEST2(assignTask, c);
                    long tmpEFT = tmpStartTime + assignTask.getMaxWeight() / c.getSpeed();
                    if (tmpEFT <= EFT) {
                        EFT = tmpEFT;
                        assignedCPU = c;
                        start_time = tmpStartTime;
                        // }
                    }
                }
            }


            //もし，当該タスクが属しているクラスタがしきい値を満たしていなければ，
            //挿入ベースの方法の処理へと移行する．

            assignTask.setStartTime(start_time);
            if(start_time>=assignTask.getPriorityTlevel()){

            }else{
                this.dwtime+=assignTask.getPriorityTlevel() - start_time;
            }
            //assignTask.setTlevel(start_time);
            //System.out.println("Task: "+assignTask.getIDVector().get(1) + "-> CPU:"+assignedCPU.getCpuID());
            //taskをCPUへ割り当てる処理．
            this.assignProcess(assignTask, assignedCPU);

            //readyタスクの後続タスクたちのTleveを更新する．
            this.updateSucTasks(assignTask);


        }
        //this.calcMakeSpan();
        // return this.retApl;

    }

    public long getDwtime() {
        return dwtime;
    }

    public void setDwtime(long dwtime) {
        this.dwtime = dwtime;
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

    /**
     * @param task
     */
    public void updatePriorityTlevel(AbstractTask task) {
        Iterator<DataDependence> dpredIte = task.getDpredList().iterator();
        TaskCluster sucCluster = this.retApl.findTaskCluster(task.getClusterID());

        long tlevel = 0;
        TaskCluster cls = this.retApl.findTaskCluster(task.getClusterID());

        while (dpredIte.hasNext()) {
            DataDependence dpred = dpredIte.next();
            //先行タスクを取得する．
            AbstractTask predTask = this.retApl.findTaskByLastID(dpred.getFromID().get(1));
            //先行タスクが属するタスククラスタを取得する．
            TaskCluster predCluster = this.retApl.findTaskCluster(predTask.getClusterID());
            long value = 0;
            if (this.isHetero()) {
                long nw_time = 0;
                if (cls.getCPU().getMachineID() == predCluster.getCPU().getMachineID()) {

                } else {
                    nw_time = this.env.getSetupTime()+dpred.getMaxDataSize() / this.env.getNWLink(predCluster.getCPU().getCpuID(), cls.getCPU().getCpuID());
                }
                value = predTask.getPriorityTlevel() + (this.getInstrunction(predTask) / predCluster.getCPU().getSpeed()) + nw_time;

                //this.getNWTime(predTask.getIDVector().get(1), task.getIDVector().get(1), dpred.getMaxDataSize());

            } else {
                value = predTask.getPriorityTlevel() + (this.getInstrunction(predTask) / predCluster.getCPU().getSpeed()) +
                        this.getNWTime(predTask.getIDVector().get(1), task.getIDVector().get(1), dpred.getMaxDataSize(),
                                this.env.getNWLink(predCluster.getCPU().getCpuID(), cls.getCPU().getCpuID()));
            }

            if (value >= tlevel) {
                tlevel = value;

            }
        }
        task.setPriorityTlevel(tlevel);

    }

    /**
     * @param task
     * @return
     */
    public boolean isFreeTask(AbstractTask task) {
        //入力辺を取得
        Iterator<DataDependence> dpredIte = task.getDpredList().iterator();

        boolean ret = true;
        while (dpredIte.hasNext()) {
            DataDependence dpred = dpredIte.next();
            if (!dpred.isReady()) {
                ret = false;
                break;
            }
        }

        return ret;
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
        //Tlevelの小さい順→blevelの大きい順にソートする．
        Arrays.sort(oa, new TaskPriorityTlevelComparator());
        //Arrays.sort(oa, new TaskPriorityLargeTlevelComparator());
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


    public TaskCluster assignProcess(AbstractTask task, CPU cpu) {
        //もしCPUに，タスククラスタが未割り当てならば，当該タスクのクラスタID
        //をセットする．


        if (this.underThresholdList.contains(task.getClusterID())) {
            TaskCluster org_cluster = this.retApl.findTaskCluster(task.getClusterID());
            if (cpu.getCpuID().longValue() == org_cluster.getCPU().getCpuID().longValue()) {
                //移動が不要の場合は何もしない
            } else {
                TaskCluster cluster = this.retApl.findTaskCluster(cpu.getTaskClusterID());
                task.setClusterID(cluster.getClusterID());
                org_cluster.removeTask(task.getIDVector().get(1));
                cluster.addTask(task.getIDVector().get(1));

                this.updateOutSet(cluster, null);
                //InSetを更新する（後のレベル値の更新のため）
                this.updateInSet(cluster, null);
                this.updateTopSet(cluster, null);

                if (org_cluster.getTaskSet().isEmpty()) {
                    this.retApl.removeTaskCluster(org_cluster.getClusterID());
                    this.underThresholdList.remove(org_cluster.getClusterID());
                }
            }


        }
        //CPUの実行終了時刻を更新する．
//        long cpu_finish_time = task.getStartTime() + task.getMaxWeight() / cpu.getSpeed();
        // cpu.setFinishTime(cpu_finish_time);

        cpu.getFtQueue().add(task);
        cpu.setFinishTime(cpu.getEndTime());


        //スケジュール済みセットにタスクを追加する．
        this.scheduledTaskSet.add(task.getIDVector().get(1));

        return this.retApl.findTaskCluster(cpu.getTaskClusterID());


    }


}
