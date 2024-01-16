package net.gripps.clustering.algorithms.triplet;

import net.gripps.clustering.algorithms.rac.RAC_Algorithm;
import net.gripps.clustering.common.aplmodel.*;
import net.gripps.clustering.tool.Calc;
import net.gripps.environment.CPU;
import net.gripps.environment.Environment;

import java.util.*;

/**
 * Author: H. Kanemitsu
 * Date: 14/12/15
 */
public class Triplet_Algorithm extends RAC_Algorithm {

    /**
     * Tripletのリスト．
     * まずはdegreeの多い順とし，同じdegreeであればデータサイズの大きい方を優先させる．
     */
    protected PriorityQueue<TripletInfo> tripletQueue;


    /**
     * 帯域幅の大きい順とし，同じ帯域幅であれば処理能力の高い方を優先させる．
     */
    protected PriorityQueue<CPU> cpuQueue;


    public Triplet_Algorithm(BBTask apl, String file, Environment env) {
        super(apl, file, env);
        this.tripletQueue = new PriorityQueue<TripletInfo>(100, new TripletComparator());
        this.cpuQueue = new PriorityQueue<CPU>(100, new CPUComparator());
    }

    public Triplet_Algorithm(BBTask apl, String file, Environment env, Hashtable<Long, CPU> in_cpuList) {
        super(apl, file, env, in_cpuList);
        this.tripletQueue = new PriorityQueue<TripletInfo>(100, new TripletComparator());
        this.cpuQueue = new PriorityQueue<CPU>(100, new CPUComparator());

    }

    public boolean isTripletExists(CustomIDSet set) {
        boolean ret = false;
        Iterator<TripletInfo> tripletIte = this.tripletQueue.iterator();
        while (tripletIte.hasNext()) {
            TripletInfo info = tripletIte.next();
            if (info.containsAll(set)) {
                ret = true;
                break;
            }
        }
        return ret;
    }

    public TripletInfo createTriplet(CustomIDSet set) {
        return null;
    }

    /**
     *
     */
    public void initialize() {
        //RACの初期化（プロセッサ能力＝割り当てるべきクラスタサイズ）を算出する．
        super.initialize();

        //Tripletとしての初期化処理
        //中間としてのタスクを選択する．
        Iterator<AbstractTask> taskIte = this.retApl.getTaskList().values().iterator();
        while (taskIte.hasNext()) {
            AbstractTask task = taskIte.next();

            if (task.getDpredList().isEmpty()) {
                //もしSTARTタスクが空ならば，無視する．
                continue;
            }
            if (task.getDsucList().isEmpty()) {
                //もしタスクがENDタスクならば，無視する．
                continue;
            }
            //taskの先行タスクに対して，後続タスクを得る．
            Iterator<DataDependence> dpredIte = task.getDpredList().iterator();


            while (dpredIte.hasNext()) {
                DataDependence dpred = dpredIte.next();
                Long predID = dpred.getFromID().get(1);
                //set.add(predID);
                Iterator<DataDependence> dsucIte = task.getDsucList().iterator();

                while (dsucIte.hasNext()) {
                    CustomIDSet set = new CustomIDSet();
                    //自身を追加
                    set.add(task.getIDVector().get(1));
                    //先行タスクを追加
                    set.add(predID);
                    DataDependence dsuc = dsucIte.next();
                    Long sucID = dsuc.getToID().get(1);
                    set.add(sucID);
                    if (this.isTripletExists(set)) {
                        set.remove(sucID);
                    } else {
                        //Tripletを生成する．
                        TripletInfo info = new TripletInfo(set, this.retApl);
                        if (this.isTripletExists(set)) {

                        } else {
                            this.tripletQueue.offer(info);

                        }
                    }
                }

            }
        }

        //次に，各CPUに対するループ
        Iterator<CPU> cpuIte = this.env.getCpuList().values().iterator();
        while (cpuIte.hasNext()) {
            CPU cpu = cpuIte.next();
            cpu.setBw(this.env.getBWFromCPU(cpu));
            this.cpuQueue.offer(cpu);
        }

    }

    @Override
    public BBTask process() {
        //HEFTと同じ準備を行う．
        this.prepare();
        //RACと同じ初期化を行う．
        this.initialize();
        this.mainProcess();

        //System.out.println("総数は"+nm);
        this.clustering();

        /*Iterator<TaskCluster> clsite = this.retApl.getTaskClusterList().values().iterator();
        int nm = 0;
        while (clsite.hasNext()) {
            TaskCluster c = clsite.next();
            System.out.println("CLSID:" + c.getClusterID() + "num:" + c.getTaskSet().getList().size());
            nm += c.getTaskSet().getList().size();
        }
        */
        this.tripletAssign();

        this.postProcess();

        return this.retApl;

    }

    public void tripletAssign() {
        Iterator<TaskCluster> clusterIte = this.retApl.getTaskClusterList().values().iterator();
        PriorityQueue<ClusterInfo> cQueue = new PriorityQueue<ClusterInfo>(10, new ClusterAssignComparator());

        //クラスタを格納する．
        while (clusterIte.hasNext()) {
            TaskCluster c = clusterIte.next();
            ClusterInfo info = this.createClusterInfo(c);
            info.setTotalTaskSize(this.calculateSumValue(c.getTaskSet()));
            cQueue.offer(info);
        }


        //実際の割り当て処理
        while (!cQueue.isEmpty()) {
            //クラスタを取得
            ClusterInfo info = cQueue.poll();
            //cpuQueueの先頭に割り当てる．
            CPU cpu = this.cpuQueue.poll();
            this.tripletAssignProcess(info.getCluster(), cpu);


        }
    }

    public void tripletAssignProcess(TaskCluster pivot, CPU cpu) {
        // TaskCluster pivot = this.retApl.findTaskCluster(cpu.getTaskClusterID());
        //pivot.addTask(task.getIDVector().get(1));
        //TaskCluster target = this.retApl.findTaskCluster(task.getClusterID());
        //pivot内の各
        pivot.setCPU(cpu);
        cpu.setTaskClusterID(pivot.getClusterID());
        this.updateOutSet(pivot, null);
        //InSetを更新する（後のレベル値の更新のため）
        this.updateInSet(pivot, null);
        this.updateTopSet(pivot, null);
        //this.retApl.removeTaskCluster(null.getClusterID());
        //task.setClusterID(pivot.getClusterID());


    }

    public void removeTripletByTask(Long id) {
        Iterator<TripletInfo> infoIte = this.tripletQueue.iterator();
        while (infoIte.hasNext()) {
            TripletInfo info = infoIte.next();
            if (info.getTaskSet().contains(id)) {
                infoIte.remove();
                //break;
            }
        }
    }

    public ClusterInfo createClusterInfo(TaskCluster cluster) {

        int totalDegree = 0;
        long totalDataSize = 0;
        Iterator<Long> outIte = cluster.getOut_Set().iterator();
        while (outIte.hasNext()) {
            Long oID = outIte.next();
            AbstractTask t = this.retApl.findTaskByLastID(oID);
            //tの後続タスクを見る．
            Iterator<DataDependence> dsucIte = t.getDsucList().iterator();
            while (dsucIte.hasNext()) {
                DataDependence dsuc = dsucIte.next();
                if (cluster.getTaskSet().contains(dsuc.getToID().get(1))) {
                    continue;
                } else {
                    totalDegree++;
                    totalDataSize += dsuc.getMaxDataSize();
                }
            }
        }

        Iterator<Long> inIte = cluster.getIn_Set().iterator();
        while (inIte.hasNext()) {
            Long inID = inIte.next();
            AbstractTask inTask = this.retApl.findTaskByLastID(inID);
            //タスクの先行タスクを見る．
            Iterator<DataDependence> dpredIte = inTask.getDpredList().iterator();
            while (dpredIte.hasNext()) {
                DataDependence dpred = dpredIte.next();
                if (cluster.getTaskSet().contains(dpred.getFromID().get(1))) {
                    continue;
                } else {
                    totalDegree++;
                    totalDataSize += dpred.getMaxDataSize();
                }
            }
        }
        ClusterInfo info = new ClusterInfo(cluster, totalDegree, totalDataSize);

        return info;
    }


    /**
     * 要素数が2以下であるクラスタ集合を取得し，
     * - degree数の多いものから選択して，pivotとする．
     * - pivotのin/outタスクのうちで，最もデータサイズの大きなクラスタを
     * targetとしてクラスタリングする．
     *
     * @return
     */
    public void clustering() {
        PriorityQueue<ClusterInfo> cQueue = new PriorityQueue<ClusterInfo>(10, new ClusterComparator());
        //要素数が2以下であるクラスタ集合をキューに入れる．
        Iterator<TaskCluster> clusterIte = this.retApl.getTaskClusterList().values().iterator();
        while (clusterIte.hasNext()) {
            TaskCluster cluster = clusterIte.next();
            if (cluster.getTaskSet().getList().size() <= 2) {
                ClusterInfo info = this.createClusterInfo(cluster);
                cQueue.offer(info);
            } else {
                continue;
            }
        }

        while (!cQueue.isEmpty()) {
            //pivotの選択
            ClusterInfo info = cQueue.poll();
            TaskCluster pivot = info.getCluster();
            long maxSize = 0;
            TaskCluster target = null;

            Iterator<Long> outIte = pivot.getOut_Set().iterator();
            while (outIte.hasNext()) {
                Long oID = outIte.next();
                AbstractTask t = this.retApl.findTaskByLastID(oID);
                //tの後続タスクを見る．
                Iterator<DataDependence> dsucIte = t.getDsucList().iterator();
                while (dsucIte.hasNext()) {
                    DataDependence dsuc = dsucIte.next();
                    if (pivot.getTaskSet().contains(dsuc.getToID().get(1))) {
                        continue;
                    } else {
                        long tmpSize = dsuc.getMaxDataSize();
                        if (maxSize <= tmpSize) {
                            maxSize = tmpSize;
                            AbstractTask targetTask = this.retApl.findTaskByLastID(dsuc.getToID().get(1));
                            target = this.retApl.findTaskCluster(targetTask.getClusterID());
                        }
                    }
                }
            }

            Iterator<Long> inIte = pivot.getIn_Set().iterator();
            while (inIte.hasNext()) {
                Long inID = inIte.next();
                AbstractTask inTask = this.retApl.findTaskByLastID(inID);
                //タスクの先行タスクを見る．
                Iterator<DataDependence> dpredIte = inTask.getDpredList().iterator();
                while (dpredIte.hasNext()) {
                    DataDependence dpred = dpredIte.next();
                    if (pivot.getTaskSet().contains(dpred.getFromID().get(1))) {
                        continue;
                    } else {
                        long tmpSize = dpred.getMaxDataSize();
                        if (maxSize <= tmpSize) {
                            maxSize = tmpSize;
                            AbstractTask targetTask = this.retApl.findTaskByLastID(dpred.getFromID().get(1));
                            target = this.retApl.findTaskCluster(targetTask.getClusterID());
                        }
                    }
                }
            }

            pivot = this.tripletClustering(pivot, target);
            //targetをcQueueから削除する．
            Iterator<ClusterInfo> cIte = cQueue.iterator();
            while (cIte.hasNext()) {
                ClusterInfo in = cIte.next();
                if (in.getCluster().getClusterID() == target.getClusterID()) {
                    cIte.remove();
                    break;
                }
            }
            //pivotを消すかどうか
            if (pivot.getTaskSet().getList().size() <= 2) {
                ClusterInfo newInfo = this.createClusterInfo(pivot);
                cQueue.offer(newInfo);
            }


        }
        //  return null;

    }

    public TaskCluster tripletClustering(TaskCluster pivot, TaskCluster target) {
        Long topTaskID = pivot.getTopTaskID();


        if (pivot.getClusterID().longValue() > target.getClusterID().longValue()) {
            return this.tripletClustering(target, pivot);
        }

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

        pivot = this.updateOutSet(pivot, target);
        //InSetを更新する（後のレベル値の更新のため）
        pivot = this.updateInSet(pivot, target);

        pivot = this.updateTopSet(pivot, target);

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
        //targetクラスタをFreeから削除する．
        this.freeClusterList.remove(target.getClusterID());

        return pivot;


    }

    /**
     * pivotとtargetのうち，最初のタスクIDが小さい方のクラスタをpivotとする．
     * 各タスクのIDをセットし，top/in/outを更新する．
     * target/pivot内のタスクが含まれているtriplet自体を削除する．
     * target/pivotのtripletを削除する．そして，pivotのtripletを再登録する．
     *
     * @param pivot
     * @param target
     * @return
     */
    public TaskCluster tripletClustering(TripletInfo pivot, TripletInfo target) {
        //pivotは既に削除されているが，targetはまだキューから削除されていない．
        // this.tripletQueue.remove(target);
        //this.tripletQueue.remove(pivot);

        Long pID = pivot.getTaskSet().getList().get(0);
        Long tID = target.getTaskSet().getList().get(0);
        if (tID.longValue() < pID.longValue()) {
            return this.tripletClustering(target, pivot);
        }

        //pivotのクラスタに対して，どんどんタスクを入れる．
        TaskCluster cluster = this.retApl.findTaskCluster(pID);
        Iterator<Long> pIDIte = pivot.getTaskSet().iterator();
        Iterator<Long> tIDIte = target.getTaskSet().iterator();

        //タスク反映処理@pivot
        while (pIDIte.hasNext()) {
            Long id = pIDIte.next();
            //キューから削除
            this.removeTripletByTask(id);
            //APLからのクラスタ削除処理
            AbstractTask t = this.retApl.findTaskByLastID(id);
            this.retApl.removeTaskCluster(t.getClusterID());

            //クラスタにタスクを追加
            cluster.addTask(id);
            //タスクの属するクラスタIDのセット
            t.setClusterID(pID);

        }
        //タスク反映処理＠target
        while (tIDIte.hasNext()) {
            Long id = tIDIte.next();
            //キューから削除
            this.removeTripletByTask(id);
            //APLからのクラスタ削除処理
            AbstractTask t = this.retApl.findTaskByLastID(id);
            this.retApl.removeTaskCluster(t.getClusterID());

            //クラスタにタスクを追加
            cluster.addTask(id);
            //タスクの属するクラスタIDのセット
            t.setClusterID(pID);
        }
        //APLにpivotを追加
        this.retApl.addTaskCluster(cluster);

        //各集合の更新
        cluster = this.updateOutSet(cluster, null);
        //InSetを更新する（後のレベル値の更新のため）
        cluster = this.updateInSet(cluster, null);
        cluster = this.updateTopSet(cluster, null);
        Iterator<Long> topIte2 = cluster.getTop_Set().iterator();
        while (topIte2.hasNext()) {
            Long id = topIte2.next();
            AbstractTask startTask2 = this.retApl.findTaskByLastID(id);
            this.updateDestTaskList2(new CustomIDSet(), startTask2, cluster.getClusterID());
        }


        //pivotとしてのtripletを生成する．
        this.createTriplet(cluster.getTaskSet());
        TripletInfo pivot_info = new TripletInfo(cluster.getTaskSet(), this.retApl);
        //そしてキューに新規追加する．
        this.tripletQueue.offer(pivot_info);


        return cluster;

    }

    /**
     * 指定のセットから，タスククラスタを生成する．
     *
     * @param set
     * @return
     */
    public TaskCluster createTaskCluster(CustomIDSet set) {
        Iterator<Long> setIte = set.iterator();
        AbstractTask firstTask = this.retApl.findTaskByLastID(set.getList().get(0));
        TaskCluster firstCluster = this.retApl.findTaskCluster(firstTask.getClusterID());

        while (setIte.hasNext()) {
            Long id = setIte.next();
            this.removeTripletByTask(id);

            if (id == firstTask.getIDVector().get(1)) {
                continue;
            } else {

                firstCluster.addTask(id);
                AbstractTask t = this.retApl.findTaskByLastID(id);
                //まずは，retAPLからクラスタを削除する．
                this.retApl.removeTaskCluster(t.getClusterID());
                t.setClusterID(firstCluster.getClusterID());

            }
        }
        // this.retApl.addTaskCluster(firstCluster);

        this.updateOutSet(firstCluster, null);
        //InSetを更新する（後のレベル値の更新のため）
        this.updateInSet(firstCluster, null);
        this.updateTopSet(firstCluster, null);
        //this.retApl.removeTaskCluster(firstCluster.getClusterID());
        //task.setClusterID(firstCluster.getClusterID());
        return firstCluster;

    }

    public TaskCluster createClusterProcess(TripletInfo info) {
        TaskCluster pivot = this.createTaskCluster(info.getTaskSet());
        pivot.getDestCheckedSet().getObjSet().clear();
        Iterator<Long> topIte2 = pivot.getTop_Set().iterator();
        while (topIte2.hasNext()) {
            Long id = topIte2.next();
            AbstractTask startTask2 = this.retApl.findTaskByLastID(id);
            this.updateDestTaskList2(new CustomIDSet(), startTask2, pivot.getClusterID());

        }

        return pivot;

    }

    public CustomIDSet getDestTaskSet(CustomIDSet taskSet,
                                      AbstractTask task, CustomIDSet retSet, CustomIDSet checkedSet) {

        if (checkedSet.contains(task.getIDVector().get(1))) {
            return retSet;
        }
        //もし後続タスク自身が異なるクラスタであれば，ここで終了．
        //補題: なぜなら，それ以降はかならず異なるクラスタとなるはずだから．
        if (!taskSet.contains(task.getIDVector().get(1))) {
            return retSet;
        } else {
            //タスク自身が，当該クラスタ内に属しているときの処理
            retSet.add(task.getIDVector().get(1));
            checkedSet.add(task.getIDVector().get(1));

            LinkedList<DataDependence> dsuclist = task.getDsucList();
            Iterator<DataDependence> ite = dsuclist.iterator();
            if (dsuclist.isEmpty()) {

                //後続タスクがなければ,そのままリターン
                return retSet;
            } else {
                while (ite.hasNext()) {
                    DataDependence dd = ite.next();
                    AbstractTask dsuc = this.retApl.findTaskByLastID(dd.getToID().get(1));
                    //再帰呼び出しにより,後続処理結果のセットをマージする．
                    CustomIDSet retSet2 = this.getDestTaskSet(taskSet, dsuc, retSet, checkedSet);
                    retSet.getObjSet().addAll(retSet2.getObjSet());

                }
                //タスク自身にセットする．
                //task.setDestTaskSet(set);
                //task.getDestTaskSet().add(task.getIDVector().get(1));

                //pivot.getDestCheckedSet().add(task.getIDVector().get(1));
                //set.add(task.getIDVector().get(1));
                return retSet;
            }
        }
    }


    @Override
    public void mainProcess() {

        // super.mainProcess();
        //Triplet同士をクラスタリングする．
        while (!this.tripletQueue.isEmpty()) {

            //先頭の要素を取得する．
            TripletInfo info = this.tripletQueue.poll();
            if (this.tripletQueue.isEmpty()) {
                break;
            }

            //8割以上オーバーラップしていない かつ20%以上大きくなる場合はマージする．
            Object[] infoArray = this.tripletQueue.toArray();
            Arrays.sort(infoArray, new TripletComparator());
            int len = infoArray.length;
            boolean isClustered = false;

            //マージ対象を決めるためのループ
            for (int i = 0; i < len; i++) {
                TripletInfo in = (TripletInfo) infoArray[i];
                CustomIDSet tmpSet = new CustomIDSet();
                tmpSet.addAll(info.getTaskSet());
                tmpSet.addAll(in.getTaskSet());


                PriorityQueue<DataDependence> ddList = this.getDDList(info.getTaskSet(), in.getTaskSet());
                if (!ddList.isEmpty()) {
                    //キューが空でないならば，キューのループを行う．
                    while (!ddList.isEmpty()) {
                        DataDependence dd = ddList.poll();
                        AbstractTask fromTask = this.retApl.findTaskByLastID(dd.getFromID().get(1));
                        AbstractTask toTask = this.retApl.findTaskByLastID(dd.getToID().get(1));

                        //fromTaskのdestを取得する．
                        CustomIDSet fromDestSet = this.getDestTaskSet(info.getTaskSet(), fromTask, new CustomIDSet(), new CustomIDSet());
                        double t_rest = Calc.getRoundedValue((double) this.calculateSumValue(fromDestSet) / (double) this.maxSpeed);
                        CustomIDSet toDestSet = this.getDestTaskSet(in.getTaskSet(), toTask, new CustomIDSet(), new CustomIDSet());
                        double t_worst = Calc.getRoundedValue((double) this.calculateSumValue(toDestSet) / (double) this.minSpeed);
                        double t_best = Calc.getRoundedValue((double) this.calculateSumValue(in.getTaskSet()) / (double) this.maxSpeed);
                        double t_comm = Calc.getRoundedValue((double) dd.getMaxDataSize() / (double) this.minLink);

                        //条件を満たせば，
                        if ((t_comm + t_worst < t_rest + t_best)
                               /* ||
                                (this.calculateSumValue(tmpSet) >= 1.2 * this.calculateSumValue(info.getTaskSet())*//* ||
                                        this.calculateSumValue(info.getTaskSet()) + this.calculateSumValue(in.getTaskSet()) >= 1.2*this.calculateSumValue(in.getTaskSet())*/) {
                            TaskCluster cluster = this.tripletClustering(info, in);
                            isClustered = true;
                            break;
                        } else {
                            continue;
                        }
                    }

                } else {

                    PriorityQueue<DataDependence> ddpredList = this.getDDpredList(in.getTaskSet(), info.getTaskSet());
                    if (!ddpredList.isEmpty()) {
                        //キューが空でないならば，キューのループを行う．
                        while (!ddpredList.isEmpty()) {
                            DataDependence dd = ddpredList.poll();
                            AbstractTask fromTask = this.retApl.findTaskByLastID(dd.getFromID().get(1));
                            AbstractTask toTask = this.retApl.findTaskByLastID(dd.getToID().get(1));

                            //fromTaskのdestを取得する．
                            CustomIDSet fromDestSet = this.getDestTaskSet(in.getTaskSet(), fromTask, new CustomIDSet(), new CustomIDSet());
                            double t_rest = Calc.getRoundedValue((double) this.calculateSumValue(fromDestSet) / (double) this.maxSpeed);
                            CustomIDSet toDestSet = this.getDestTaskSet(info.getTaskSet(), toTask, new CustomIDSet(), new CustomIDSet());
                            double t_worst = Calc.getRoundedValue((double) this.calculateSumValue(toDestSet) / (double) this.minSpeed);
                            double t_best = Calc.getRoundedValue((double) this.calculateSumValue(info.getTaskSet()) / (double) this.maxSpeed);
                            double t_comm = Calc.getRoundedValue((double) dd.getMaxDataSize() / (double) this.minLink);

                            //条件を満たせば，
                            if ((t_comm + t_worst < t_rest + t_best)/* ||
                                    (this.calculateSumValue(tmpSet) >= 1.2 * this.calculateSumValue(info.getTaskSet())*/
                                    /* ||
                                           this.calculateSumValue(info.getTaskSet()) + this.calculateSumValue(in.getTaskSet()) >= 1.2*this.calculateSumValue(in.getTaskSet())*/) {
                                TaskCluster cluster = this.tripletClustering(in, info);
                                isClustered = true;
                                break;
                            } else {
                                continue;
                            }
                        }
                    } else {
                        //continue;
                        if (this.calculateSumValue(tmpSet) >= 1.2 * this.calculateSumValue(info.getTaskSet())/*||
                                this.calculateSumValue(info.getTaskSet()) + this.calculateSumValue(in.getTaskSet()) >= 1.2*this.calculateSumValue(in.getTaskSet())*/) {
                            TaskCluster cluster = this.tripletClustering(info, in);
                            isClustered = true;
                        } else {
                            //System.out.println("ダメだった");
                        }
                    }


                }
                if (isClustered) {
                    break;
                }
            }

            if (!isClustered) {
                //クラスタされなければ，triplet自体をクラスタとする．
                // this.createClusterProcess(info);
                //System.out.println("セルフ");

            }

        }

    }

    public PriorityQueue<DataDependence> getDDpredList(CustomIDSet pivotSet, CustomIDSet targetSet) {
        PriorityQueue<DataDependence> dataQueue = new PriorityQueue<DataDependence>(10, new DataComparator());
        Iterator<Long> taskIte = targetSet.iterator();
        while (taskIte.hasNext()) {
            Long id = taskIte.next();
            AbstractTask t = this.retApl.findTaskByLastID(id);
            //後続タスクを見る．
            Iterator<DataDependence> dpredIte = t.getDpredList().iterator();
            while (dpredIte.hasNext()) {
                DataDependence dpred = dpredIte.next();
                if (targetSet.contains(dpred.getFromID().get(1))) {
                    continue;
                } else {
                    if (pivotSet.contains(dpred.getFromID().get(1))) {
                        //targetに含まれている∧pivotにはないならば，キューに入れる．
                        dataQueue.offer(dpred);
                    }
                }
            }
        }
        return dataQueue;
    }


    public PriorityQueue<DataDependence> getDDList(CustomIDSet pivotSet, CustomIDSet targetSet) {
        PriorityQueue<DataDependence> dataQueue = new PriorityQueue<DataDependence>(10, new DataComparator());
        Iterator<Long> taskIte = pivotSet.iterator();
        while (taskIte.hasNext()) {
            Long id = taskIte.next();
            AbstractTask t = this.retApl.findTaskByLastID(id);
            //後続タスクを見る．
            Iterator<DataDependence> dsucIte = t.getDsucList().iterator();
            while (dsucIte.hasNext()) {
                DataDependence dsuc = dsucIte.next();
                if (pivotSet.contains(dsuc.getToID().get(1))) {
                    continue;
                } else {
                    if (targetSet.contains(dsuc.getToID().get(1))) {
                        //targetに含まれている∧pivotにはないならば，キューに入れる．
                        dataQueue.offer(dsuc);
                    }
                }
            }
        }
        return dataQueue;
    }

    public void mainProcessLB() {

        Iterator<TaskCluster> clusterIte = this.retApl.getTaskClusterList().values().iterator();
        while (clusterIte.hasNext()) {
            //TaskCluster pivot =
        }


    }


    /**
     * @param fromCluster
     * @param toCluster
     * @return
     */
    public TaskCluster clusteringClusterLB(TaskCluster fromCluster, TaskCluster toCluster, CustomIDSet targetList) {
        //toClusterの全タスク集合を取得する．
        CustomIDSet IDSet = toCluster.getTaskSet();
        Iterator<Long> taskIte = IDSet.iterator();
        //とりあえずはunderリストから削除する．
        TaskCluster retCluster = null;

        this.unAssignedCPUs.add(toCluster.getCPU().getCpuID());
        toCluster.setCPU(null);
        this.retApl.removeTaskCluster(toCluster.getClusterID());


        //toCluster内のタスクたちに対するループ
        while (taskIte.hasNext()) {
            Long taskID = taskIte.next();
            //fromClusterへタスクを追加する．
            fromCluster.addTask(taskID);
            //toClusterにあったタスクの所属クラスタの変更
            AbstractTask task = this.retApl.findTaskByLastID(taskID);
            task.setClusterID(fromCluster.getClusterID());

            //this.retApl.updateTask(task);
        }

        //以降は，retAplへの更新処理
        //toTaskの反映
        // this.retApl.updateTask((AbstractTask) toTask.deepCopy());
        //fromClusterのIn/Outを更新
        this.updateInOut(fromCluster, toCluster);

        CustomIDSet topSet = this.getTopList(fromCluster);
        fromCluster.setTop_Set(topSet);
        Iterator<Long> topIte = topSet.iterator();
        fromCluster.getDestCheckedSet().getObjSet().clear();
        while (topIte.hasNext()) {
            Long topID = topIte.next();
            AbstractTask startTask = this.retApl.findTaskByLastID(topID);
            this.updateDestTaskList2(new CustomIDSet(), startTask, fromCluster.getClusterID());
        }

        //そしてretAplへの，fromClusterの反映
        //this.retApl.updateTaskCluster(fromCluster);
        //toClusterの削除
        this.retApl.removeTaskCluster(toCluster.getClusterID());
        //UEXからのtoClusterの削除
        targetList.remove(toCluster.getClusterID());


        retCluster = fromCluster;

        return retCluster;

    }


}
