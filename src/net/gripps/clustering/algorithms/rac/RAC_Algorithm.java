package net.gripps.clustering.algorithms.rac;

import net.gripps.clustering.common.aplmodel.*;
import net.gripps.clustering.tool.Calc;
import net.gripps.environment.CPU;
import net.gripps.environment.Environment;
import net.gripps.scheduling.algorithms.heterogeneous.heft.HEFT_Algorithm;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.PriorityQueue;

/**
 * Author: H. Kanemitsu
 * Date: 14/12/10
 */
public class RAC_Algorithm extends HEFT_Algorithm {
    /**
     * CPU速度の合計値
     */
    protected long totalCPUSpeed;

    protected CustomIDSet clusteredSet;

    /**
         * 帯域幅の大きい順とし，同じ帯域幅であれば処理能力の高い方を優先させる．
         */
        protected PriorityQueue<CPU> cpuQueue;

    public RAC_Algorithm(BBTask task, String file, Environment env_tmp) {
        super(task, file, env_tmp);
        this.cpuQueue = new PriorityQueue<CPU>(100, new RAC_CPUComparator());

    }

    public RAC_Algorithm(BBTask apl, String file, Environment env, Hashtable<Long, CPU> in_cpuList) {
        super(apl, file, env, in_cpuList);
        this.cpuQueue = new PriorityQueue<CPU>(100, new RAC_CPUComparator());

    }

    /**
     * 各プロセッサ能力（power)をセットするためのループ
     */
    public void initialize() {
        this.clusteredSet = new CustomIDSet();
        Iterator<CPU> cpuIte = this.env.getCpuList().values().iterator();
        Iterator<CPU> cpuIte2 = this.env.getCpuList().values().iterator();

        //速度合計値のためのループ
        while (cpuIte.hasNext()) {
            CPU cpu = cpuIte.next();
            this.totalCPUSpeed += cpu.getSpeed();
            this.unAssignedCPUs.add(cpu.getCpuID());

        }

        //値を割り当てるため，もう一度ループ
        while (cpuIte2.hasNext()) {
            CPU cpu = cpuIte2.next();
            cpu.setPowerRate(Calc.getRoundedValue((double) cpu.getSpeed() / (double) this.totalCPUSpeed));
            cpu.setProcessingCapacity((long) (this.getApl().getMaxWeight() * cpu.getPowerRate()));

        }

         //次に，各CPUに対するループ
        Iterator<CPU> cpuIte3 = this.env.getCpuList().values().iterator();
        while(cpuIte3.hasNext()){
            CPU cpu = cpuIte3.next();
            cpu.setBw( this.env.getBWFromCPU(cpu));
            this.cpuQueue.offer(cpu);
        }


    }

    @Override
    public BBTask process() {
        try {
            //初期のクラスタ生成+uexクラスタ集合の生成を行う．
            this.initialize();
            this.prepare();
            long start = System.currentTimeMillis();
            //メイン処理
            this.mainProcess();
            long end = System.currentTimeMillis();
            retApl.setProcessTime((end - start));

            //後処理を行う．
            this.postProcess();
            return this.retApl;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public AbstractTask updateDirection(AbstractTask t) {


        Iterator<DataDependence> dpredIte = t.getDpredList().iterator();
        Iterator<DataDependence> dsucIte = t.getDsucList().iterator();
        long tmpPredScore = 0;
        long tmpSucScore = 0;
        int direction = -1;
        double maxScore = 0.0;

        while (dpredIte.hasNext()) {
            DataDependence dpred = dpredIte.next();
            AbstractTask dpredTask = this.retApl.findTaskByLastID(dpred.getFromID().get(1));
            if (!this.uexClusterList.contains(dpredTask.getClusterID())) {
                continue;
            }
            if (dpredTask.getClusterID() == t.getClusterID()) {
                continue;
            }
            tmpPredScore += dpred.getMaxDataSize();
        }

        while (dsucIte.hasNext()) {
            DataDependence dsuc = dsucIte.next();
            AbstractTask dsucTask = this.retApl.findTaskByLastID(dsuc.getToID().get(1));

            if (!this.uexClusterList.contains(dsucTask.getClusterID())) {
                continue;
            }
            if (dsucTask.getClusterID() == t.getClusterID()) {
                continue;
            }
            tmpSucScore += dsuc.getMaxDataSize();
        }

        if (tmpPredScore == 0 && tmpSucScore == 0) {
            direction = -1;
        } else {
            if (tmpPredScore >= tmpSucScore) {
                direction = 0;
            } else {
                direction = 1;
            }
            double value = Calc.getRoundedValue((double) (Math.max(tmpPredScore, tmpSucScore) / (double) t.getMaxWeight()));
            if (value >= maxScore) {
                maxScore = value;

            }
        }
        t.setDirection(direction);
        return t;

    }

    public AbstractTask getCoordinator() {
        Iterator<Long> uexIte = this.uexClusterList.iterator();
        double maxScore = 0.0;
        AbstractTask coordinator = null;
        //上方向: 0 / 下方向:1
        int direction = -1;

        while (uexIte.hasNext()) {
            direction = -1;
            Long id = uexIte.next();
            TaskCluster cls = this.retApl.findTaskCluster(id);
            if (cls.getTaskSet().getList().size() > 1) {
                uexIte.remove();
                continue;
            } else {
                AbstractTask t = this.retApl.findTaskByLastID(cls.getTopTaskID());
                Iterator<DataDependence> dpredIte = t.getDpredList().iterator();
                Iterator<DataDependence> dsucIte = t.getDsucList().iterator();
                long tmpPredScore = 0;
                long tmpSucScore = 0;

                while (dpredIte.hasNext()) {
                    DataDependence dpred = dpredIte.next();
                    AbstractTask dpredTask = this.retApl.findTaskByLastID(dpred.getFromID().get(1));
                    if (!this.uexClusterList.contains(dpredTask.getClusterID())) {
                        continue;
                    }
                    tmpPredScore += dpred.getMaxDataSize();
                }

                while (dsucIte.hasNext()) {
                    DataDependence dsuc = dsucIte.next();
                    AbstractTask dsucTask = this.retApl.findTaskByLastID(dsuc.getToID().get(1));

                    if (!this.uexClusterList.contains(dsucTask.getClusterID())) {
                        continue;
                    }
                    tmpSucScore += dsuc.getMaxDataSize();
                }

        //        if (tmpPredScore == 0 && tmpSucScore == 0) {
        //            continue;
        //        } else {
                    if (tmpPredScore >= tmpSucScore) {
                        direction = 0;
                    } else {
                        direction = 1;
                    }
                    double value = Calc.getRoundedValue((double) (Math.max(tmpPredScore, tmpSucScore) / (double) t.getMaxWeight()));
                    if (value >= maxScore) {
                        maxScore = value;
                        coordinator = t;
                    }
              //  }
            }
        }
        if (coordinator != null) {
            coordinator.setDirection(direction);
            return coordinator;
        } else {
            return null;
        }

    }


    public boolean isAboveCapacity(TaskCluster cluster) {
        CPU cpu = cluster.getCPU();
        long value = (long) cpu.getPowerRate() * this.calculateSumValue(cluster.getTaskSet());
        if (value >= cpu.getProcessingCapacity()) {
            return true;
        } else {
            return false;
        }
    }

    public double calcClusterCapacity(TaskCluster cluster, CPU cpu) {
        return Calc.getRoundedValue((double) cpu.getPowerRate() * (double) this.calculateSumValue(cluster.getTaskSet()));

    }

    public TaskCluster getSmallestCluster(TaskCluster pivot) {
        //IN/Outタスクの先行・後続タスクのうち，データサイズが最大のものを選択する．
        Iterator<Long> inIte = pivot.getIn_Set().iterator();
        Iterator<Long> outIte = pivot.getOut_Set().iterator();
        double maxDataSize = 0.0;
        TaskCluster target = null;

        while(inIte.hasNext()){
            Long id = inIte.next();
            AbstractTask inTask = this.retApl.findTaskByLastID(id);
            //inTaskの先行タスクを取得する．
            Iterator<DataDependence> dpredIte = inTask.getDpredList().iterator();
            while(dpredIte.hasNext()){
                DataDependence dpred = dpredIte.next();
                AbstractTask dpredTask = this.retApl.findTaskByLastID(dpred.getFromID().get(1));
                if(pivot.getClusterID() == dpredTask.getClusterID()) {
                    continue;
                }else{
                    double value =Calc.getRoundedValue((double)dpred.getMaxDataSize()/(double) dpredTask.getMaxWeight());
                    if(value >= maxDataSize){
                        maxDataSize = value;
                        target = this.retApl.findTaskCluster(dpredTask.getClusterID());
                    }
                }
            }
        }

        while(outIte.hasNext()){
                   Long id = outIte.next();
                   AbstractTask outTask = this.retApl.findTaskByLastID(id);
                   //inTaskの先行タスクを取得する．
                   Iterator<DataDependence> dsucIte = outTask.getDsucList().iterator();
                   while(dsucIte.hasNext()){
                       DataDependence dsuc = dsucIte.next();
                       AbstractTask dsucTask = this.retApl.findTaskByLastID(dsuc.getToID().get(1));
                       if(pivot.getClusterID() == dsucTask.getClusterID()) {
                           continue;
                       }else{
                           double value = Calc.getRoundedValue((double) dsuc.getMaxDataSize() / (double) dsucTask.getMaxWeight());
                           if(value >= maxDataSize){
                               maxDataSize = value;
                               target = this.retApl.findTaskCluster(dsucTask.getClusterID());
                           }
                       }
                   }
               }

        return target;
        //割り当て済みcpuから，クラスタを取得する．
        /*Iterator<Long> clsIte = this.clusteredSet.iterator();
        TaskCluster retCluster = null;
        long minSize = 999999;

        while (clsIte.hasNext()) {
            Long id = clsIte.next();
            if (id.longValue() == pivot.getClusterID().longValue()) {
                continue;
            }
            TaskCluster cls = this.retApl.findTaskCluster(id);
            long value = this.calculateSumValue(cls.getTaskSet());
            if (value <= minSize) {
                retCluster = cls;
                minSize = value;
            }


        }
        return retCluster;
        */
    }


    /**
     * RAC(Resource-Aware Clustering)アルゴリズムのメイン処理です．
     */
    public void mainProcess() {
        //未集約クラスタが存在する限りのループ
        while (!this.uexClusterList.isEmpty()) {
            AbstractTask coTask = null;

            TaskCluster pivot = null;

            boolean isTargetFound_initial = true;
            boolean isTargetFound = true;

            //未集約クラスタが一つのみの場合は，既存のクラスタに含める．
            //サイズが最小のクラスタとまとめる．
            if (this.uexClusterList.getList().size() == 1) {
                //他のクラスタに含める処理
                Long coID = this.uexClusterList.getList().getFirst();
                coTask = this.retApl.findTaskByLastID(coID);
                isTargetFound_initial = false;

            } else {
                //コーディネータを取得する．
                coTask = this.getCoordinator();
                if (coTask == null) {
                    //System.out.println("コーディネータがnull");
                } else {

                }
            }
            //次にCPUを決める．最小のパワーをもつCPUに割り当てる．
            Iterator<Long> cpuIte = this.unAssignedCPUs.iterator();
            long max_Speed = 0;
            CPU assignedCPU = null;

           /* while (cpuIte.hasNext()) {
                Long cpuID = cpuIte.next();
                CPU cpu = this.env.getCPU(cpuID);
                if (cpu.getSpeed() >= max_Speed) {
                    max_Speed = cpu.getSpeed();
                    assignedCPU = cpu;
                }
            }*/

        assignedCPU = this.cpuQueue.poll();

            pivot = this.retApl.findTaskCluster(coTask.getClusterID());
            pivot.setCPU(assignedCPU);
            int count = 0;
            //coTaskとcpuが決まったので，後はクラスタリングのループをする．
            //while (this.calcClusterCapacity(pivot, assignedCPU) < assignedCPU.getSpeed()/*assignedCPU.getProcessingCapacity()*/) {
             //   System.out.println("count:" + count + "/value:" + this.calcClusterCapacity(pivot, assignedCPU));
            while(this.calculateSumValue(pivot.getTaskSet())<= assignedCPU.getProcessingCapacity()){

                double maxScore = 0.0;
                AbstractTask target = null;
                TaskCluster targetCluster = null;

                if (isTargetFound_initial) {
                    //double maxScore = 0.0;

                    //コーディネータのdirectionを見て，targetを取得する．
                    if (coTask.getDirection() == 0) {
                        //先行タスクを見る．
                        Iterator<DataDependence> dpredIte = coTask.getDpredList().iterator();
                        while (dpredIte.hasNext()) {
                            DataDependence dpred = dpredIte.next();
                            AbstractTask dpredTask = this.retApl.findTaskByLastID(dpred.getFromID().get(1));

                            if (!this.uexClusterList.contains(dpredTask.getClusterID())) {
                                continue;
                            } else if (dpredTask.getClusterID() == pivot.getClusterID()) {
                                continue;
                            } else {
                                AbstractTask predTask = this.retApl.findTaskByLastID(dpred.getFromID().get(1));
                                double value = Calc.getRoundedValue((double) dpred.getMaxDataSize() / (double) predTask.getMaxWeight());
                                if (maxScore <= value) {
                                    maxScore = value;
                                    target = predTask;
                                }
                            }
                        }

                    } else if (coTask.getDirection() == 1) {
                        Iterator<DataDependence> dsucIte = coTask.getDsucList().iterator();
                        while (dsucIte.hasNext()) {
                            DataDependence dsuc = dsucIte.next();
                            AbstractTask dsucTask = this.retApl.findTaskByLastID(dsuc.getToID().get(1));
                            if (!this.uexClusterList.contains(dsucTask.getClusterID())) {
                                continue;
                            } else if (pivot.getClusterID() == dsucTask.getClusterID()) {
                                continue;
                            } else {
                                AbstractTask sucTask = this.retApl.findTaskByLastID(dsuc.getToID().get(1));
                                double value = Calc.getRoundedValue((double) dsuc.getMaxDataSize() / (double) sucTask.getMaxWeight());
                                if (maxScore <= value) {
                                    maxScore = value;
                                    target = sucTask;
                                }
                            }
                        }
                        isTargetFound = true;
                    } else {
                        //先行／後続タスクからtargetを得られない場合は，他の最小サイズのクラスタとまとめる．
                        isTargetFound = false;
                 //       System.out.println("ERROR:" + 275);

                    }
                } else {
                    isTargetFound = false;
                  //  System.out.println("ERROR:" + 276);
                }

                // if (isTargetFound) {
                if (target != null) {
                    //targetが見つかれば，普通の処理
                    targetCluster = this.retApl.findTaskCluster(target.getClusterID());
                    coTask = this.retApl.findTaskByLastID(targetCluster.getClusterID());
                    coTask = this.updateDirection(coTask);
                    //クラスタリング処理
                    pivot = this.rac_clustering(pivot, targetCluster);
                } else {
                    //targetが見つからなければ，最小サイズのクラスタとする．
                    targetCluster = this.getSmallestCluster(pivot);
                    //クラスタリング処理
                    pivot = this.rac_clustering(pivot, targetCluster);

                    Iterator<Long> inIte = pivot.getIn_Set().iterator();
                    double tmpScore = 0.0;
                    AbstractTask nextCoTask = null;
                    while (inIte.hasNext()) {
                        Long id = inIte.next();
                        AbstractTask inTask = this.retApl.findTaskByLastID(id);
                        //inTaskの先行タスクを見る．
                        Iterator<DataDependence> dpredIte = inTask.getDpredList().iterator();
                        while (dpredIte.hasNext()) {
                            DataDependence dpred = dpredIte.next();
                            AbstractTask predTask = this.retApl.findTaskByLastID(dpred.getFromID().get(1));
                            if (!this.uexClusterList.contains(dpred.getFromID().get(1))) {
                                continue;
                            }
                            if (predTask.getClusterID() == pivot.getClusterID()) {
                                continue;
                            }
                            double value = Calc.getRoundedValue((double) dpred.getMaxDataSize() / (double) predTask.getMaxWeight());
                            if (value >= tmpScore) {
                                tmpScore = value;
                                nextCoTask = predTask;
                            }
                        }


                    }

                    Iterator<Long> outIte = pivot.getOut_Set().iterator();
                    while (outIte.hasNext()) {
                        Long id = outIte.next();
                        AbstractTask outTask = this.retApl.findTaskByLastID(id);
                        //inTaskの先行タスクを見る．
                        Iterator<DataDependence> dsucIte = outTask.getDsucList().iterator();
                        while (dsucIte.hasNext()) {
                            DataDependence dsuc = dsucIte.next();
                            AbstractTask sucTask = this.retApl.findTaskByLastID(dsuc.getToID().get(1));
                            if (!this.uexClusterList.contains(dsuc.getToID().get(1))) {
                                continue;
                            }
                            if (sucTask.getClusterID() == pivot.getClusterID()) {
                                continue;
                            }
                            double value = Calc.getRoundedValue((double) dsuc.getMaxDataSize() / (double) sucTask.getMaxWeight());
                            if (value >= tmpScore) {
                                tmpScore = value;
                                nextCoTask = sucTask;
                            }
                        }
                    }
                    if(nextCoTask == null){
                        //おそらく，八方ふさがりになっている．
                        //次のcotaskは，サイズの小さな他クラスタとする
                        //System.out.println("coTaskがnull");
                        TaskCluster nextCluster = this.getSmallestCluster(pivot);
                        pivot = this.rac_clustering(pivot, nextCluster);
                        break;
                    }else{
                        coTask = nextCoTask;
                       this.updateDirection(coTask);

                     //  System.out.println("**286**");
                    }

                }
                isTargetFound_initial = true;
//isFoudがnullの場合に，coTaskの決め方がおかしい．
                count++;
            }
            //CPU及びpivotを削除する．
            this.uexClusterList.remove(pivot.getClusterID());
            this.unAssignedCPUs.remove(assignedCPU.getCpuID());
            this.clusteredSet.add(pivot.getClusterID());

        }


    }

    public TaskCluster rac_clustering(TaskCluster pivot, TaskCluster target) {
        CPU pivotCPU = pivot.getCPU();
        CPU targetCPU = target.getCPU();
        if(pivotCPU == null){
            pivot.setCPU(targetCPU);
            targetCPU.setTaskClusterID(pivot.getClusterID());
        }else{
        }

        if (pivot.getClusterID().longValue() > target.getClusterID().longValue()) {
            return this.rac_clustering(target, pivot);
        }
        this.clusteredSet.remove(pivot.getClusterID());
        this.clusteredSet.remove(target.getClusterID());

        //targetの全タスク集合を取得する．
        CustomIDSet IDSet = target.getTaskSet();
        Iterator<Long> taskIte = IDSet.iterator();

        //target内の全タスクの所属クラスタの変更処理
        while (taskIte.hasNext()) {
            Long taskID = taskIte.next();
            //fromClusterへタスクを追加する．
            pivot.addTask(taskID);
            //toClusterにあったタスクの所属クラスタの変更
            AbstractTask task = this.retApl.findTaskByLastID(taskID);
            task.setClusterID(pivot.getClusterID());

        }

        if (target.getCPU() == null) {

        } else if (!target.getCPU().isVirtual()) {
            this.unAssignedCPUs.add(target.getCPU().getCpuID());
        } else {

        }

        pivot = this.updateOutSet(pivot, target);
        pivot = this.updateInSet(pivot, target);
        pivot = this.updateTopSet(pivot, target);

        Iterator<Long> topIte = pivot.getTop_Set().iterator();
        Long topID = null;
        long topTlevel = 0;
        while (topIte.hasNext()) {
            Long id = topIte.next();
            AbstractTask topTask = this.retApl.findTaskByLastID(id);
            if (topTlevel <= topTask.getTlevel()) {
                topTlevel = topTask.getTlevel();
                topID = topTask.getIDVector().get(1);
            }

        }
        pivot.setTopTaskID(topID);
        //各タスクの，destSetの更新をする．

        CustomIDSet allSet = pivot.getTaskSet();
        Iterator<Long> idIte = allSet.iterator();
        CustomIDSet startIDs = new CustomIDSet();
        //まずはTopタスクのIDを追加しておく．

        AbstractTask startTask = this.retApl.findTaskByLastID(pivot.getTopTaskID());
        pivot.getDestCheckedSet().getObjSet().clear();
        Iterator<Long> topIte2 = pivot.getTop_Set().iterator();
        while (topIte2.hasNext()) {
            Long id = topIte2.next();
            AbstractTask startTask2 = this.retApl.findTaskByLastID(id);
            this.updateDestTaskList2(new CustomIDSet(), startTask2, pivot.getClusterID());

        }

        //targetクラスタをDAGから削除する．
        this.retApl.removeTaskCluster(target.getClusterID());
        //targetクラスタをUEXから削除する．
        this.uexClusterList.remove(target.getClusterID());
        return pivot;

    }
}
