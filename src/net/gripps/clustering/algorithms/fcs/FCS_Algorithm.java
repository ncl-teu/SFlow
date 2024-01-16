package net.gripps.clustering.algorithms.fcs;

import net.gripps.clustering.common.aplmodel.*;
import net.gripps.clustering.tool.Calc;
import net.gripps.environment.CPU;
import net.gripps.environment.Environment;
import net.gripps.environment.Machine;
import net.gripps.scheduling.algorithms.heterogeneous.heft.HEFT_Algorithm;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.PriorityQueue;

/**
 * Author: H. Kanemitsu
 * Date: 14/09/30
 */
public class FCS_Algorithm extends HEFT_Algorithm {

    /**
     * 実際に用いられるCPUコアのリスト
     */
    protected Hashtable<Long, CPU> cpuTable;

    protected PriorityQueue<FCS_CPUInfo> tau_CPUQueue;

    protected PriorityQueue<Long> uexTaskList;

    protected double tau;


    public FCS_Algorithm(BBTask task, String file, Environment env_tmp) {
        super(task, file, env_tmp);
        this.cpuTable = env_tmp.getCpuList();
        this.tau_CPUQueue = new PriorityQueue<FCS_CPUInfo>(5, new TauComparator());
        this.uexTaskList = new PriorityQueue<Long>(5, new FCS_TaskComparator());
        this.tau = 0.0;

    }


    /**
     * 指定のCPU集合で，アルゴリズムを実装する場合．
     * 比較実験の場合は，こちらを使う．
     *
     * @param apl
     * @param file
     * @param env
     * @param in_cpuList
     */
    public FCS_Algorithm(BBTask apl, String file, Environment env, Hashtable<Long, CPU> in_cpuList) {
        this(apl, file, env);
        this.cpuTable = in_cpuList;
    }

    public void initialize() {

        double cpuSum = 0.0;

        Iterator<CPU> cpuIte = this.cpuTable.values().iterator();
        while (cpuIte.hasNext()) {
            CPU cpu = cpuIte.next();
            cpuSum += Calc.getRoundedValue(cpu.getSpeed());
        }
        long taskSum = 0;
        long cpuNum = this.cpuTable.size();
        double ave_cpuSpeed = Calc.getRoundedValue((double) cpuSum / (double) cpuNum);

        //全体の平均帯域幅を算出する．
        long[][] linkMX = this.env.getLinkMatrix();
        long len = linkMX[0].length;
        int cnt = 0;
        long totalBW = 0;
        long count = 0;

        for (int i = 0; i < len; i++) {

            for (int j = i + 1; j < len; j++) {
                if (linkMX[i][j] == -1) {
                    continue;
                } else if (!this.cpuTable.containsKey(new Long(i)) || (!this.cpuTable.containsKey(new Long(j)))) {
                    // }else if(!this.cpuTable.contains(i) || (!this.cpuTable.contains(j))){
                    continue;
                } else {
                    totalBW += linkMX[i][j];
                    cnt++;
                }
            }
        }
        double aveLink = Calc.getRoundedValue((double) totalBW / (double) cnt);

        this.tau = Calc.getRoundedValue(ave_cpuSpeed / aveLink);

    }

    public BBTask process() {
        try {
            this.initialize();
            this.prepare();
            long start = System.currentTimeMillis();
            //メイン処理
            this.mainProcess();
            long end = System.currentTimeMillis();
            retApl.setProcessTime((end - start));

            System.out.println("FCSの処理時間:" + retApl.getProcessTime());

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

    public CustomIDSet getCPTaskSet(AbstractTask task, CustomIDSet set) {

        set.add(task.getIDVector().get(1));
        if (task.getDpredList().isEmpty()) {
            return set;
        } else {
            AbstractTask predTask = this.retApl.findTaskByLastID(task.getTpred().get(1));
            set = this.getCPTaskSet(predTask, set);
        }


        return set;

    }

    /**
     * freeClusterリストから，β値が最小のものを返す．
     *
     * @return
     */
    public TaskCluster getMinBetaCluster(TaskCluster pivot) {
        //Iterator<Long> freeIte = this.freeClusterList.iterator();
        Iterator<TaskCluster> clusterIte = this.retApl.getTaskClusterList().values().iterator();
        double retBeta = 99999999.9;
        TaskCluster retCluster = null;
        while (clusterIte.hasNext()) {
            //Long id = freeIte.next();
            TaskCluster cls = clusterIte.next();
            // if (cls.getTaskSet().getList().size() > 1) {
            if (cls.getClusterID() == pivot.getClusterID()) {

            } else {
                double beta = cls.getBeta();
                if (beta <= retBeta) {
                    retBeta = beta;
                    retCluster = cls;
                }
            }

            //  } else {
            continue;
            //  }

        }
        return retCluster;

    }


    /**
     * 指定のpivot（CPU割り当て済み)に対してtargetをクラスタリングした後の
     * Beta値を見積もる．
     *
     * @param pivot
     * @param target
     * @return
     */
    public double estimateBeta(TaskCluster pivot, TaskCluster target) {
        //targetのInタスクを取得する．
        Iterator<Long> outIte = target.getOut_Set().iterator();
        double r_value = pivot.getFCS_R();
        double c_value = pivot.getFCS_C();

        while (outIte.hasNext()) {
            Long outID = outIte.next();
            AbstractTask t = this.retApl.findTaskByLastID(outID);
            Iterator<DataDependence> dsucIte = t.getDsucList().iterator();
            //後続タスクを取得する．そして，後続タスクがpivotに含まれていれば，pivotのCを減らす．
            while (dsucIte.hasNext()) {
                DataDependence dsuc = dsucIte.next();
                if (pivot.getTaskSet().contains(dsuc.getToID().get(1))) {
                    c_value -= dsuc.getMaxDataSize();
                }
            }
        }
        r_value += this.calculateSumValue(target.getTaskSet());
        //System.out.println("R_value;"+r_value+ "/C_Value;"+c_value);
        if (c_value == 0.0) {
            c_value = 1.0;
        }
        return Calc.getRoundedValue(r_value / c_value);

    }

    public double getTau(CPU cpu) {
        //this.unAssignedCPUs.add(cpu.getCpuID());
        Machine m = this.env.getMachineList().get(cpu.getMachineID());
        double tau = Calc.getRoundedValue((double) cpu.getSpeed() / (double) m.getBw());

        return tau;
    }

    public TaskCluster updateRC(TaskCluster cluster) {
        //Inタスクの入力辺全て取得
        Iterator<Long> inIte = cluster.getIn_Set().iterator();
        double c_value = 0.0;
        double r_value = 0.0;

        while (inIte.hasNext()) {
            Long ID = inIte.next();
            AbstractTask inTask = this.retApl.findTaskByLastID(ID);
            Iterator<DataDependence> inDDIte = inTask.getDpredList().iterator();
            while (inDDIte.hasNext()) {
                DataDependence dpred = inDDIte.next();
                //もしfromタスクが同一クラスタならば，無視
                if (cluster.getTaskSet().contains(dpred.getFromID().get(1))) {

                } else {
                    c_value += dpred.getMaxDataSize();

                }
            }
        }
        cluster.setFCS_C(c_value);
        r_value = this.calculateSumValue(cluster.getTaskSet());
        cluster.setFCS_R(r_value);

        return cluster;


    }

    /**
     * @param cluster
     * @return
     */
    public boolean isAboveTau(TaskCluster cluster, CPU cpu) {
        /*double r_value = cluster.getFCS_R();
        double c_value = cluster.getFCS_C();
        if(c_value == 0){
            c_value = 1.0;
        }*/
        //System.out.println("R:"+r_value + "/C:"+c_value);
        //double beta = Calc.getRoundedValue(r_value / c_value);
        if (cluster.getFCS_C() == 0.0) {
            return false;
        }
        double beta = cluster.getBeta();
        double tau = this.tau;

        if (beta <= tau) {
            return false;
        } else {
            return true;
        }
    }

    public boolean isAboveTau(TaskCluster cluster) {
        double beta = cluster.getBeta();
        //double beta = Calc.getRoundedValue(cluster.getFCS_R()/cluster.getFCS_C());
        if (beta < this.tau) {
            return false;
        } else {
            return true;
        }

    }


    /**
     *
     */
    public void mainProcess() {

        //Endタスクを取得する．
        AbstractTask endTask = this.retApl.findTaskByLastID(this.retApl.getEndTask().get(1));
        CustomIDSet CPTaskSet = this.getCPTaskSet(endTask, new CustomIDSet());
        //最高性能のプロセッサを取得する．
        Iterator<CPU> cpuIte = this.cpuTable.values().iterator();
        CPU highCPU = null;
        while (cpuIte.hasNext()) {
            CPU cpu = cpuIte.next();
            Machine m = this.env.getMachineList().get(cpu.getMachineID());

            if (cpu.getSpeed() == this.maxSpeed) {
                if (m.getBw() == this.maxLink) {
                    highCPU = cpu;
                    break;
                }
            }
        }
        //CPUに対して，CPタスクを割り当てる．
        Iterator<Long> cpIte = CPTaskSet.iterator();
        while (cpIte.hasNext()) {
            Long id = cpIte.next();
            AbstractTask task = this.retApl.findTaskByLastID(id);
            this.assignProcess(task, highCPU);
            this.uexTaskList.remove(task.getIDVector().get(1));

        }
        this.unAssignedCPUs.remove(highCPU.getCpuID());
        Iterator<Long> umIte = this.unAssignedCPUs.iterator();

        CPU assignedCPU = highCPU;
        TaskCluster pivot = this.retApl.findTaskCluster(assignedCPU.getTaskClusterID());
        this.updateRC(pivot);
        boolean isInitial = true;
        //マシンに対するループ
        //各マシンを未割り当てCPUリストへ追加させて，CPUリストを作成する．
        //優先度リストに，CPコア以外を追加する．
        while (umIte.hasNext()) {
            CPU cpu = this.cpuTable.get(umIte.next());
            FCS_CPUInfo info = new FCS_CPUInfo(cpu, this.getTau(cpu));
            this.tau_CPUQueue.offer(info);
        }

        //CPであるタスク集合を最高性能（τ）
        //未クラスタリングなクラスタが存在する間のループ
        while (!this.uexTaskList.isEmpty()) {
            //uexClusterListから，辞書式にタスクを取得する．
            Long taskID = this.uexTaskList.poll();
            AbstractTask t = this.retApl.findTaskByLastID(taskID);

            //  if (isInitial) {
            //      isInitial = false;
            //   } else {
            //pivot = this.retApl.findTaskCluster(t.getClusterID());
            FCS_CPUInfo nextInfo = this.tau_CPUQueue.poll();
            assignedCPU = nextInfo.getCpu();
            //    }
            pivot = this.assignProcess(t, assignedCPU);
            int size = pivot.getTaskSet().getList().size();

            //TaskCluster target = this.retApl.findTaskCluster(t.getClusterID());
            //もしτ以下であれば，クラスタリング続行．
            //double est_beta = this.estimateBeta(pivot, target);

            //if (est_beta <= this.getTau(assignedCPU)) {
            while (!this.isAboveTau(pivot, assignedCPU) || size <= 1) {
                Long org_id = t.getIDVector().get(1);
                //System.out.println("残りタスク数："+this.uexTaskList.size() + "クラスタ数:"+this.retApl.getTaskClusterList().size());
                System.out.println("tのID;" + t.getIDVector().get(1));
                if (pivot == null) {
                    System.out.println("");
                }
                pivot = this.updateRC(pivot);
                this.uexTaskList.remove(t.getIDVector().get(1));
                //h(x), s(x), p(y), n(x)のいずれかをまとめるためのループ
                //まずは，h(x)の処理
                FCSInfo info = this.clustering_hx(pivot, t);
                AbstractTask retTask = info.getTask();
                //クラスタリングできる限りのループ
                while (retTask != null) {
                    t = retTask;
                    info = this.clustering_hx(pivot, t);
                    retTask = info.getTask();
                    pivot = info.getCluster();
                    if (this.isAboveTau(pivot, assignedCPU)) {
                        break;
                    }
                }
                if (this.isAboveTau(pivot, assignedCPU)) {
                    continue;
                }
                CustomIDSet pList = new CustomIDSet();
                //System.out.println("beta:"+pivot.getBeta());

                //次は，兄妹タスクとまとめる．
                Iterator<Long> topIte = pivot.getTop_Set().iterator();
                while (topIte.hasNext()) {
                    Long id = topIte.next();
                    AbstractTask topTask = this.retApl.findTaskByLastID(id);
                    Iterator<DataDependence> dpredIte = topTask.getDpredList().iterator();
                    while (dpredIte.hasNext()) {
                        DataDependence dpred = dpredIte.next();
                        pList.add(dpred.getFromID().get(1));
                    }

                }

                Iterator<Long> pIte = pList.iterator();
                //共通の親を持つものから，未集約タスクリストを取得．
                //そして，pivotとのクラスタリングによってβが小さくなるものを
                //兄妹としてまとめる．
                double retBeta = 99999999.0;
                AbstractTask candidateTask = null;
                while (pIte.hasNext()) {
                    Long pID = pIte.next();
                    AbstractTask pTask = this.retApl.findTaskByLastID(pID);
                    Iterator<DataDependence> dsucIte = pTask.getDsucList().iterator();
                    while (dsucIte.hasNext()) {
                        DataDependence dsuc = dsucIte.next();
                        //もしpivotに含まれていれば，無視
                        if (pivot.getTaskSet().contains(dsuc.getToID().get(1))) {
                            continue;
                        } else {
                            if (this.uexTaskList.contains(dsuc.getToID().get(1))) {
                                AbstractTask tTask = this.retApl.findTaskByLastID(dsuc.getToID().get(1));
                                TaskCluster target = this.retApl.findTaskCluster(tTask.getClusterID());
                                double tmpBeta = this.estimateBeta(pivot, target);
                                if (tmpBeta <= retBeta) {
                                    retBeta = tmpBeta;
                                    candidateTask = tTask;
                                }
                            }
                        }
                    }
                }
                if (candidateTask != null) {
                    pivot = this.assignProcess(candidateTask, assignedCPU);
                    this.updateRC(pivot);
                    this.uexTaskList.remove(candidateTask.getIDVector().get(1));
                    t = candidateTask;
                } else {

                }
                if (this.isAboveTau(pivot, assignedCPU)) {
                    continue;
                }
                //次は，pivotのinタスクの親タスクを含める
                Iterator<Long> inIte = pivot.getIn_Set().iterator();
                double inRetBeta = 99999999.0;
                AbstractTask inRetTask = null;
                while (inIte.hasNext()) {
                    Long id = inIte.next();
                    AbstractTask inTask = this.retApl.findTaskByLastID(id);
                    //inタスクの先行タスクを取得
                    Iterator<DataDependence> dpredIte = inTask.getDpredList().iterator();
                    while (dpredIte.hasNext()) {
                        DataDependence dpred = dpredIte.next();
                        AbstractTask predTask = this.retApl.findTaskByLastID(dpred.getFromID().get(1));
                        TaskCluster target = this.retApl.findTaskCluster(predTask.getClusterID());
                        if (!pivot.getTaskSet().contains(dpred.getFromID().get(1))) {
                            if (this.uexTaskList.contains(dpred.getFromID().get(1))) {
                                double inTempBeta = this.estimateBeta(pivot, target);
                                if (inTempBeta < inRetBeta) {
                                    inRetBeta = inTempBeta;
                                    inRetTask = predTask;
                                }
                            }

                        } else {
                            continue;
                        }

                    }
                }
                if (inRetTask != null) {
                    pivot = this.assignProcess(inRetTask, assignedCPU);
                    if (pivot == null) {
                        System.out.println("null");
                    }
                    this.updateRC(pivot);
                    this.uexTaskList.remove(inRetTask.getIDVector().get(1));
                    t = inRetTask;
                }
                if (this.isAboveTau(pivot, assignedCPU)) {
                    continue;
                }
                //次は，辞書式である次のノードを含める．
                //つまり，tを決める．
                //Outタスクのうちで，βが最も小さくなりそうな後続タスクを取得する．
                Iterator<Long> outIte = pivot.getOut_Set().iterator();
                double outbeta = 9999999.9;
                while (outIte.hasNext()) {
                    Long outID = outIte.next();
                    AbstractTask outTask = this.retApl.findTaskByLastID(outID);
                    Iterator<DataDependence> dsucIte = outTask.getDsucList().iterator();
                    while (dsucIte.hasNext()) {
                        DataDependence dsuc = dsucIte.next();
                        if (pivot.getTaskSet().contains(dsuc.getToID().get(1))) {

                        } else {

                            AbstractTask dsucTask = this.retApl.findTaskByLastID(dsuc.getToID().get(1));
                            if (!this.uexTaskList.contains(dsucTask.getIDVector().get(1))) {
                                continue;
                            }
                            TaskCluster target = this.retApl.findTaskCluster(dsucTask.getClusterID());
                            if (target == null) {
                                System.out.println(403);
                            }
                            double tmpOUTbeta = this.estimateBeta(pivot, target);
                            if (tmpOUTbeta <= outbeta) {
                                outbeta = tmpOUTbeta;
                                t = dsucTask;
                            }
                        }
                    }

                }
                ///tが不変の場合（この時点で未だtauを超えていない）
                if (org_id.longValue() == t.getIDVector().get(1)) {
                    //新たなtが得られなかった場合である．
                    //既存のクラスタへと割り当てる．
                    TaskCluster target = pivot;
                    pivot = this.getMinBetaCluster(pivot);
                    System.out.println("****510***");
                    pivot = this.fcs_clustering(pivot, target);
                    t = this.retApl.findTaskByLastID(pivot.getBsucTaskID());
                    assignedCPU = pivot.getCPU();

                } else {
                    //新たなtが得られた場合は何もしない．

                }

                /*if (this.uexTaskList.contains(t.getIDVector().get(1))) {

                } else {
                    //以降は，tがuexに含まれない場合の処理
                    if (this.uexTaskList.isEmpty()) {
                        if (this.isAboveTau(pivot, assignedCPU)) {
                            continue;
                        } else {
                            //まだτ未満でもうタスクがなければ，既存のクラスタでβが小さいものを選択する．
                            pivot = this.getMinBetaCluster(pivot);

                            if (org_id.longValue() == t.getIDVector().get(1)) {
                                //Long newID = this.uexTaskList.poll();
                                //t = this.retApl.findTaskByLastID(newID);


                            } else {

                            }

                        }
                    } else {
                        if (org_id.longValue() == t.getIDVector().get(1)) {
                            Long newID = this.uexTaskList.poll();
                            t = this.retApl.findTaskByLastID(newID);

                        } else {

                        }
                    }

                }*/

            }
            System.out.println("越えた");
        }
    }


    /**
     * 起点となるタスクtaskの後続タスクを見て，pivotに含めるかどうかを行う処理．
     *
     * @param pivot
     * @param task
     * @return
     */
    public FCSInfo clustering_hx(TaskCluster pivot, AbstractTask task) {

        //taskの後続タスクたちに対するループ（in-degreeが1となる後続タスクが複数あれば，
        //まとめてみてβが最小となるものを選択する．
        double retBeta = 100000000.0;
        AbstractTask retTask = null;
        AbstractTask pivotTask = task;
        //まずは，in-degreeeが1である後続タスクが存在する限り，まとめる．
        Iterator<DataDependence> dsucIte = pivotTask.getDsucList().iterator();

        while (dsucIte.hasNext()) {
            DataDependence dsuc = dsucIte.next();
            //後続タスクを取得
            AbstractTask dsucTask = this.retApl.findTaskByLastID(dsuc.getToID().get(1));
            TaskCluster target = this.retApl.findTaskCluster(dsucTask.getClusterID());
            if (!this.uexTaskList.contains(dsucTask.getIDVector().get(1))) {
                continue;
            }
            //もしin-degreeが1ならば，処理を行う．
            if ((dsucTask.getDpredList().size() == 1) && (target.getTaskSet().getList().size() == 1)) {

                double tmpBeta = this.estimateBeta(pivot, target);
                if (tmpBeta < retBeta) {
                    retBeta = tmpBeta;
                    retTask = dsucTask;
                }
            } else {
                continue;
            }
        }
        if (retTask == null) {

        } else {
            pivot = this.assignProcess(retTask, pivot.getCPU());
            pivot = this.updateRC(pivot);
            this.uexTaskList.remove(retTask.getIDVector().get(1));
        }

        FCSInfo info = new FCSInfo(retTask, pivot);

        return info;

    }


    public void prepare() {
        //アプリから，タスクリストを取得
        Hashtable<Long, AbstractTask> tasklist = this.retApl.getTaskList();
        Collection<AbstractTask> col = tasklist.values();
        Iterator<AbstractTask> ite = col.iterator();

        long start = System.currentTimeMillis();
        CustomIDSet startSet = new CustomIDSet();


        //CPUを全て未割り当て状態とする．
        //Iterator<CPU> umIte = this.env.getCpuList().values().iterator();
        Iterator<CPU> umIte = this.cpuTable.values().iterator();

        //マシンに対するループ
        //各マシンを未割り当てCPUリストへ追加させる．
        while (umIte.hasNext()) {
            CPU cpu = umIte.next();
            this.unAssignedCPUs.add(cpu.getCpuID());
            // Machine m = this.env.getMachineList().get(cpu.getMachineID());
            //double tau = Calc.getRoundedValue((double)cpu.getSpeed()/(double)m.getBw());
            //FCS_CPUInfo info = new FCS_CPUInfo(cpu, tau);
            //this.tau_CPUQueue.offer(info);
        }

        //タスククラスタの生成
        //各タスクに対するループ
        while (ite.hasNext()) {
            AbstractTask task = ite.next();
            //まずは自分自身をDestへ追加する．
            task.addDestTask(task.getIDVector().get(1));

            //タスクをクラスタへ入れる．
            TaskCluster cluster = new TaskCluster(task.getIDVector().get(1));
            //一つのタスクしか入らないので，当然Linearである．
            cluster.setLinear(true);
            //クラスタに，CPUをセットする．

            task.setClusterID(cluster.getClusterID());
            cluster.addTask(task.getIDVector().get(1));
            //クラスタのR, C値をセットする．
            cluster.setFCS_R(task.getMaxWeight());
            double C_value = 1.0;
            Iterator<DataDependence> dpredIte = task.getDpredList().iterator();
            while (dpredIte.hasNext()) {
                DataDependence dpred = dpredIte.next();
                C_value += dpred.getMaxDataSize();

            }
            cluster.setFCS_C(C_value);

            // タスククラスタに対して，各種情報をセットする．
            /**このときは，各クラスタには一つのみのタスクが入るため，
             * 以下のような処理が明示的に可能である．
             */
            //ここで，top/outタスクは，自分自身のみをセットしておく．
            cluster.setBsucTaskID(task.getIDVector().get(1));
            cluster.getBottomSet().add(task.getIDVector().get(1));
            cluster.addIn_Set(task.getIDVector().get(1));
            cluster.setTopTaskID(task.getIDVector().get(1));
            CustomIDSet topSet = new CustomIDSet();
            topSet.add(task.getIDVector().get(1));
            cluster.setTop_Set(topSet);

            //もし後続タスクがあれば，自身のタスクをOutセットへ入れる
            if (!task.getDsucList().isEmpty()) {
                cluster.addOut_Set(task.getIDVector().get(1));
            }


            //先行タスクがなけｒば，スタートセットに入れる．
            if (task.getDpredList().isEmpty()) {
                startSet.add(task.getIDVector().get(1));
                task.setStartTime(0);
            }
            //タスク自身に，所属するクラスタIDをセットする．
            //このとき，クラスタIDをタスクIDとしてセットしておく．
            task.setClusterID(task.getIDVector().get(1));
            //クラスタ自体をDAGへ反映
            Long clusterID = this.retApl.addTaskCluster(cluster);

            //この時点で，UEXを格納しておく．
            this.uexTaskList.add(task.getIDVector().get(1));
        }


        long endID = this.retApl.getTaskList().size();
        AbstractTask endTask = this.retApl.findTaskByLastID(new Long(endID));
        //Tlevelのセットをする．
        this.getMaxTlevel(endTask, new CustomIDSet());
        //Blevelのセットをする．
        Iterator<Long> startIDIte = startSet.iterator();
        CustomIDSet idSet = new CustomIDSet();

        while (startIDIte.hasNext()) {
            Long sID = startIDIte.next();
            AbstractTask startTask = this.retApl.findTaskByLastID(sID);
            this.getMaxBlevel(startTask, idSet);
        }

        //   this.calculateInitialTlevel(endTask, initialCPU, false);
        this.retApl.setStartTaskSet(startSet);

        Hashtable<Long, AbstractTask> taskTable = this.retApl.getTaskList();
        Collection<AbstractTask> taskCollection = taskTable.values();
        Iterator<AbstractTask> ite2 = taskCollection.iterator();

        //各タスククラスタへのレベル反映処理
        long start1 = System.currentTimeMillis();
        long cLevel = 0;
        //各タスクに対するループ処理
        while (ite2.hasNext()) {
            AbstractTask task = ite2.next();
            //もしタスククラスタがSTARTノードであれば，この時点でFreeリストへ入れる．
            TaskCluster cls = this.retApl.findTaskCluster(task.getClusterID());
            if (task.getDpredList().isEmpty()) {
                this.freeClusterList.add(cls.getClusterID());
            }
            //初期状態では，クラスタのレベルはタスクのレベルと同じである．
            cls.setTlevel(task.getTlevel());
            cls.setBlevel(task.getBlevel());
            long value = cls.getTlevel() + cls.getBlevel();
            if (cLevel <= value) {
                cLevel = value;
            }

        }
        this.level = cLevel;
        long end1 = System.currentTimeMillis();
        //System.out.println("レベル反映時間: "+(end1-start));

    }

    public TaskCluster assignProcess(AbstractTask task, CPU cpu) {
        //もしCPUに，タスククラスタが未割り当てならば，当該タスクのクラスタID
        //をセットする．

        TaskCluster target = this.retApl.findTaskCluster(task.getClusterID());
        TaskCluster pivot = null;
        if (cpu != null) {
            this.unAssignedCPUs.remove(cpu.getCpuID());
            System.out.print("CPUID:" + cpu.getCpuID());
            System.out.println("pivot ID" + cpu.getTaskClusterID());
        } else {

        }


        if (cpu == null ||cpu.getTaskClusterID() < 0) {
            cpu.setTaskClusterID(target.getClusterID());
            //TaskCluster cls = this.retApl.findTaskCluster(target.getClusterID());
            //cls.setCPU(cpu);
            target.setCPU(cpu);
            pivot = target;
            // this.retApl.removeTaskCluster(target.getClusterID());


        } else {

             pivot = this.retApl.findTaskCluster(cpu.getTaskClusterID());
            Iterator<Long> taskIte = target.getTaskSet().iterator();
            while (taskIte.hasNext()) {
                Long id = taskIte.next();
                AbstractTask t = this.retApl.findTaskByLastID(id);
                if (t == null || pivot == null) {
                    System.out.println("null");
                    System.out.println("みつからなかったpivotID:" + cpu.getTaskClusterID());
                }
                t.setClusterID(pivot.getClusterID());
                pivot.addTask(id);
                //pivot.getTaskSet().add(id);
            }
            CPU tcpu = target.getCPU();
            if (tcpu != null) {
                tcpu.clear();
                this.unAssignedCPUs.add(tcpu.getCpuID());
            }

            //

            this.updateOutSet(pivot, target);
            //InSetを更新する（後のレベル値の更新のため）
            this.updateInSet(pivot, target);
            this.updateTopSet(pivot, target);
            this.retApl.removeTaskCluster(target.getClusterID());
            task.setClusterID(pivot.getClusterID());


            pivot.setCPU(cpu);
            cpu.setTaskClusterID(pivot.getClusterID());

        }

        //CPUの実行終了時刻を更新する．
        long cpu_finish_time = task.getStartTime() + task.getMaxWeight() / cpu.getSpeed();
        // cpu.setFinishTime(cpu_finish_time);
        cpu.getFtQueue().add(task);
        cpu.setFinishTime(cpu.getEndTime());


        //スケジュール済みセットにタスクを追加する．
        this.scheduledTaskSet.add(task.getIDVector().get(1));

      // return this.retApl.findTaskCluster(cpu.getTaskClusterID());
        return pivot;


    }

    public TaskCluster fcs_clustering(TaskCluster pivot, TaskCluster target) {
        //もしCPUに，タスククラスタが未割り当てならば，当該タスクのクラスタID
        //をセットする．
        this.retApl.removeTaskCluster(target.getClusterID());

        CPU cpu = pivot.getCPU();
        CPU tCPU = target.getCPU();
        if (pivot.getCPU() == null) {
            pivot.setCPU(target.getCPU());


        } else if (pivot.getCPU().getCpuID() == -1) {
            pivot.setCPU(target.getCPU());
        } else {
            tCPU.clear();
            this.unAssignedCPUs.add(tCPU.getCpuID());
        }
        /*
        if(tCPU != null){
            tCPU.clear();
            this.unAssignedCPUs.add(tCPU.getCpuID());
        }*/
        // TaskCluster target = this.retApl.findTaskCluster(task.getClusterID());


        //TaskCluster pivot = this.retApl.findTaskCluster(cpu.getTaskClusterID());
        Iterator<Long> taskIte = target.getTaskSet().iterator();
        while (taskIte.hasNext()) {
            Long id = taskIte.next();
            AbstractTask t = this.retApl.findTaskByLastID(id);
            t.setClusterID(pivot.getClusterID());
            pivot.addTask(id);
        }

        //

        this.updateOutSet(pivot, target);
        //InSetを更新する（後のレベル値の更新のため）
        this.updateInSet(pivot, target);
        this.updateTopSet(pivot, target);
        //task.setClusterID(pivot.getClusterID());


        pivot.setCPU(cpu);


        //CPUの実行終了時刻を更新する．

        //cpu.getFtQueue().add(task);
        // cpu.setFinishTime(cpu.getEndTime());


        //スケジュール済みセットにタスクを追加する．
        //his.scheduledTaskSet.add(task.getIDVector().get(1));

        return pivot;


    }


}
