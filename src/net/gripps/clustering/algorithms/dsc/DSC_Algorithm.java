package net.gripps.clustering.algorithms.dsc;

import net.gripps.clustering.algorithms.AbstractClusteringAlgorithm;
import net.gripps.clustering.common.aplmodel.*;

import java.util.*;

/**
 * DSC(Dominant Sequence Clustering: T.Yang's Algorithm)アルゴリズム実装です．
 * <p/>
 * Author: H. Kanemitsu
 * Date: 2008/10/28
 */
public class DSC_Algorithm extends AbstractClusteringAlgorithm {

    /**
     * FREEリスト
     */
    CustomIDSet freeTaskIDSet;

    /**
     * PARTIAL FREEリスト
     */
    CustomIDSet pFreeTaskIDSet;

    /**
     * 未チェックリスト
     */
    CustomIDSet uEXTaskSet;

    public int count = 0;


    /**
     * @param apl
     */
    public DSC_Algorithm(BBTask apl) {
        super(apl);
        this.freeTaskIDSet = new CustomIDSet();
        this.pFreeTaskIDSet = new CustomIDSet();
        this.uEXTaskSet = new CustomIDSet();
        this.count = 0;
    }

    /**
     * @param apl
     * @param fileName
     */
    public DSC_Algorithm(BBTask apl, String fileName) {
        super(apl, fileName, 3);
        this.freeTaskIDSet = new CustomIDSet();
        this.pFreeTaskIDSet = new CustomIDSet();
        this.uEXTaskSet = new CustomIDSet();
        this.count = 0;

    }

    /**
     * @return
     */
    public BBTask process() {
        //初期化
        this.prepare();
        int size = this.retApl.getTaskList().size();
        AbstractTask endTask = this.retApl.findTaskByLastID(new Long(size));
        this.retApl.setCpLen(endTask.getTlevel() + endTask.getBlevel());

        Iterator<AbstractTask> taskIte = this.retApl.taskIerator();
        long value = 0;
        long comm = 0;
        long edgeNum = 0;
        /*      while (taskIte.hasNext()) {
            AbstractTask task = taskIte.next();
            value += task.getMaxWeight();
            LinkedList<DataDependence> dsucList = task.getDsucList();
            Iterator<DataDependence> dsucIte = dsucList.iterator();
            edgeNum += dsucList.size();
            while (dsucIte.hasNext()) {
                DataDependence dd = dsucIte.next();
                comm += dd.getMaxDataSize();
            }
        }
        */
        /* System.out.println("****DSC Algorithm****");
        System.out.println(" **初期状態**");
        System.out.println(" - タスク総数: " + this.retApl.getTaskList().size());
        System.out.println(" - タスクの重み総和: " + value);
        System.out.println(" - 一台のPCでの実行時間: " + (this.retApl.getMaxWeight() / this.minSpeed));
        System.out.println(" - 辺の総数: " + edgeNum);
        System.out.println(" - 辺の重み総和: " + comm);
        int size = this.retApl.getTaskList().size();
        AbstractTask endTask = this.retApl.findTaskByLastID(new Long(size));
        System.out.println(" - 初期APLのCP長: " + (endTask.getTlevel() + endTask.getBlevel()));
        System.out.println(" **クラスタリング実行後**");
        */

        long start = System.currentTimeMillis();
        this.mainProcess();
        long end = System.currentTimeMillis();
        retApl.setProcessTime((end-start));
       //    System.out.println(" DSC TIME: " + (end - start) + "msec");

        //this.calcLevel();
        //this.calcMakeSpan();
        //System.out.println("****前****");
        // this.postProcess();
        //System.out.println("LB前のMAKESPAN: "+ this.makeSpan);
        //this.mainProcessLB();
        // this.printDAG(this.retApl);
        // System.out.println("TIME: "+(end-start));
        //System.out.println("****後****");
        this.postProcess();
        // System.out.println("でタッチ数: "+this.count);

        return this.retApl;
    }

    /**
     * DAGに対して必要な情報をセットする．
     */
    protected void prepare() {
        Hashtable<Long, AbstractTask> tasklist = this.retApl.getTaskList();
        Collection<AbstractTask> col = tasklist.values();
        Iterator<AbstractTask> ite = col.iterator();

        long start = System.currentTimeMillis();

        CustomIDSet startSet = new CustomIDSet();

        //タスククラスタの生成
        //各タスクに対するループ
        while (ite.hasNext()) {
            AbstractTask task = ite.next();
            //未チェックリストへ追加
            this.uEXTaskSet.add(task.getIDVector().get(1));

            //まずは自分自身をDestへ追加する．
            task.addDestTask(task.getIDVector().get(1));
            // task.addParentTask(task.getIDVector().get(1));
            //タスクをクラスタへ入れる．
            TaskCluster cluster = new TaskCluster(task.getIDVector().get(1));
            //一つのタスクしか入らないので，当然Linearである．
            cluster.setLinear(true);
            cluster.addTask(task.getIDVector().get(1));

            // タスククラスタに対して，各種情報をセットする．
            /**このときは，各クラスタには一つのみのタスクが入るため，
             * 以下のような処理が明示的に可能である．
             */
            //ここで，top/outタスクは，自分自身のみをセットしておく．
            cluster.setBsucTaskID(task.getIDVector().get(1));


            cluster.addIn_Set(task.getIDVector().get(1));
            cluster.setTopTaskID(task.getIDVector().get(1));
            CustomIDSet topSet = new CustomIDSet();
            topSet.add(task.getIDVector().get(1));
           // cluster.addOut_Set(task.getIDVector().get(1));
            cluster.setTop_Set(topSet);

            if (!task.getDsucList().isEmpty()) {
                cluster.addOut_Set(task.getIDVector().get(1));
                //FREEリストへ入れる．
                this.freeTaskIDSet.add(task.getIDVector().get(1));
            }

            if (task.getDpredList().isEmpty()) {
                startSet.add(task.getIDVector().get(1));
            }

            task.setClusterID(task.getIDVector().get(1));
            //クラスタ自体をDAGへ反映
            Long clusterID = this.retApl.addTaskCluster(cluster);
            //タスクに対して，そのクラスタIDをセットする．
            //task.setClusterID(clusterID);

            //この時点で，UEXを格納しておく．
            if (!this.isClusterAboveThreshold(cluster)) {
                this.uexClusterList.add(clusterID);
            }

        }
        long endID = this.retApl.getTaskList().size();
        AbstractTask endTask = this.retApl.findTaskByLastID(new Long(endID));
        this.calculateInitialTlevel(endTask, false);
        this.retApl.setStartTaskSet(startSet);
        Iterator<Long> startIte = startSet.iterator();
        while (startIte.hasNext()) {
            Long startID = startIte.next();
            AbstractTask startTask = this.retApl.findTaskByLastID(startID);
            this.calculateInitialBlevel(startTask, false);
        }


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


    /**
     *
     */
    public void mainProcess() {
        // int i = 0;
        //UEXリストが空でない間のループ
        while (!this.uEXTaskSet.isEmpty()) {
            //System.out.println("COUNT: "+ i);
            //Freeから優先度Maxのタスクを取得
            AbstractTask freeTask = this.getHeadTaskFromFreeList();

            //pFreeから優先度Maxのタスクを取得
            AbstractTask pFreeTask = this.getHeadTaskFromPfreeList();
            long pFreeLevel = 0;
            if (pFreeTask != null) {
                pFreeLevel = pFreeTask.getTlevel() + pFreeTask.getBlevel();
            }
            long freeLevel = freeTask.getTlevel() + freeTask.getBlevel();

            //FREEタスクの優先度が大きいときは，Minimizationi Procedureをする．
            //そして，まとめられるだけ一つのクラスタにまとめる．
            if (freeLevel >= pFreeLevel) {
                //freeTaskのtlevelを削減するための処理をCALLする．
                TaskCluster retCluster = this.minimizationProcedure(freeTask, null);

            } else {
                if (pFreeTask.getTpred() == null) {
                    // System.out.println("先行の数:" + pFreeTask.getDpredList().size());
                    minimizationProcedure(freeTask, null);
                } else {
                    //pFreeTaskのDominant Predecessorタスクを取得
                    AbstractTask dpredTask = this.retApl.findTaskByLastID(pFreeTask.getTpred().get(1));
                    //Dominant Predecessorタスクが属するタスククラスタを取得する.
                    TaskCluster dpredCluster = this.retApl.findTaskCluster(dpredTask.getClusterID());
                    /*if(dpredCluster == null){
                        continue;
                    }*/
                    //実際に減るのであれば，DSRWとなる.
                    //DSCでは，この先行クラスタのみをDSRW対象としている（本当は，クラスタの「リスト」自体が対象だと思うけど・・・）
                    if (this.checkClustering(dpredCluster, dpredTask, pFreeTask, pFreeTask.getTlevel(), true)) {

                        TaskCluster DSRW_Cluster = dpredCluster;
                        TaskCluster retCluster = this.minimizationProcedure(freeTask, DSRW_Cluster);
                    } else {
                        minimizationProcedure(freeTask, null);
                    }
                }
            }
            //Freeリスト，UEXリストから，freeタスクを削除する．
            this.freeTaskIDSet.remove(freeTask.getIDVector().get(1));
            this.uEXTaskSet.remove(freeTask.getIDVector().get(1));

            //freeTaskの後続タスクたちに対し，レベル更新＋FREEへの格納を行う．
            this.updateSucs(freeTask);
        }
    }

    /**
     * @param task
     */
    public void updateTaskTlevel(AbstractTask task) {
        Iterator<DataDependence> dpredIte = task.getDpredList().iterator();

        long tlevel = 0;
        Vector<Long> tpredID = task.getTpred();

        while (dpredIte.hasNext()) {
            DataDependence dpred = dpredIte.next();
            AbstractTask predTask = this.retApl.findTaskByLastID(dpred.getFromID().get(1));
            long value = predTask.getTlevel() +
                    this.getNWTime(dpred.getFromID().get(1), dpred.getToID().get(1), dpred.getMaxDataSize(), this.minLink);
            if (tlevel <= value) {
                tlevel = value;
                tpredID = predTask.getIDVector();
            }
        }
        task.setTlevel(tlevel);
        task.setTpred(tpredID);

    }

    /**
     * @param freeTask
     */
    public void updateSucs(AbstractTask freeTask) {
        Iterator<DataDependence> dsucIte = freeTask.getDsucList().iterator();
        //freeTaskの後続タスクたちに対するループ
        while (dsucIte.hasNext()) {
            DataDependence dsuc = dsucIte.next();
            AbstractTask sucTask = this.retApl.findTaskByLastID(dsuc.getToID().get(1));
            //入力辺にチェック済みフラグをつける．
            sucTask.setCheckedFlgToDpred(freeTask.getIDVector(), sucTask.getIDVector(), true);
            //すべての入力辺がチェック済みであればFREEリストへ追加する．
            if ((sucTask.allDpredIsChecked()) && (this.uEXTaskSet.contains(sucTask.getIDVector().get(1)))) {
                this.freeTaskIDSet.add(sucTask.getIDVector().get(1));

            } else {
                this.pFreeTaskIDSet.add(sucTask.getIDVector().get(1));

            }


        }

    }

    /**
     * FREEリストから，優先度が最も大きなタスクを取り出す．
     *
     * @return
     */
    public AbstractTask getHeadTaskFromFreeList() {
        Iterator<Long> freeIte = this.freeTaskIDSet.iterator();
        AbstractTask retTask = null;

        long level = 0;
        while (freeIte.hasNext()) {
            Long freeID = freeIte.next();
            AbstractTask freeTask = this.retApl.findTaskByLastID(freeID);
            long value = freeTask.getTlevel() + freeTask.getBlevel();
            if (value >= level) {
                level = value;
                retTask = freeTask;
            }
        }

        return retTask;
    }

    /**
     * @return
     */
    public AbstractTask getHeadTaskFromPfreeList() {
        if (this.pFreeTaskIDSet.isEmpty()) {
            return null;
        }
        long level = 0;
        AbstractTask retTask = null;

        //以降はpFreeリストが空でない場合の処理
        Iterator<Long> pFreeIte = this.pFreeTaskIDSet.iterator();
        while (pFreeIte.hasNext()) {
            Long pFreeID = pFreeIte.next();
            AbstractTask pFreeTask = this.retApl.findTaskByLastID(pFreeID);
            long value = pFreeTask.getTlevel() + pFreeTask.getBlevel();
            if (level <= value) {
                level = value;
                retTask = pFreeTask;
            }
        }
        return retTask;
    }

    /**
     * @param freeTask
     * @return
     */
    public TaskCluster minimizationProcedure(AbstractTask freeTask, TaskCluster DSRW_Cluster) {
        //tlevel+タスクサイズ+通信の降順
        PriorityQueue<DSC_MP> predQueue = new PriorityQueue<DSC_MP>(5, new DSC_LevelComparator());
        //tlevel値の昇順
        PriorityQueue<AbstractTask> tlevelQueue = new PriorityQueue<AbstractTask>(5, new DSC_TlevelComparator());

        //taskの先行タスクたちをまとめる．
        Iterator<DataDependence> predIte = freeTask.getDpredList().iterator();

        //先行タスクたちにに対するループ
        //先行タスクのTlevel + 先行タスク実行時間 + NW時間の値の大きい順に格納する．
        //さらに，tlevelの昇順となるようなリストも生成しておく．
        while (predIte.hasNext()) {
            DataDependence dd = predIte.next();
            AbstractTask predTask = this.retApl.findTaskByLastID(dd.getFromID().get(1));
            DSC_MP mp = new DSC_MP(predTask, dd.getMaxDataSize() / this.minLink, this.getInstrunction(predTask) / this.minSpeed);
            predQueue.add(mp);
            tlevelQueue.add(predTask);
        }

        //もともとのtlevel値

        int idx = 0;
        TaskCluster tmpCluster = this.retApl.findTaskCluster(freeTask.getClusterID());
        TaskCluster initialCluster = tmpCluster;

        while (!predQueue.isEmpty()) {
            DSC_MP mp = predQueue.poll();
            // AbstractTask predTask = mp.getTask();
            AbstractTask predTask = this.retApl.findTaskByLastID(mp.getTask().getIDVector().get(1));
            long preTlevel = freeTask.getTlevel();
            //最初の先行タスクからは，「子供タスク」を考慮する必要はない．
            if (idx == 0) {
                //クラスタリングによって，本当にtaskのtlevelが減るのかどうか
                TaskCluster pivot = this.retApl.findTaskCluster(predTask.getClusterID());
                if(pivot == null){
                    continue;
                }
                TaskCluster target = this.retApl.findTaskCluster(freeTask.getClusterID());
                //pivotへfreeTaskをふくめてみて，freeTaskのtlevelを調べる．
                //そのためには，pivot内にてtlevelの昇順でスケジュールしてみる必要がある．
//                Iterator<Long> pTaskIte = pivot.getObjSet().iterator();
                //freeTask.setClusterID(pivot.getClusterID());
                if (DSRW_Cluster != null) {
                    if (pivot.getClusterID().longValue() == DSRW_Cluster.getClusterID().longValue()) {
                        //DSRWであれば，次のループへ行く
                        return target;
                        // continue;
                    }
                }
                if (this.checkClustering(pivot, predTask, freeTask, preTlevel, true)) {

                    initialCluster = this.clusteringDSC(pivot, target);
                } else {
                    return target;
                }
            } else {
                TaskCluster preCluster = this.retApl.findTaskCluster(predTask.getClusterID());
                if(preCluster == null){
                    break;
                }

                if (DSRW_Cluster != null) {
                    if (preCluster.getClusterID().longValue() == DSRW_Cluster.getClusterID().longValue()) {
                        //DSWRであれば，次のループへ行く
                        return initialCluster;

                        // continue;
                    }
                } else {
                    if (initialCluster.getTaskSet().contains(predTask.getIDVector().get(1))) {

                        //continue;
                    } else {
                        //先行タスクがinitialClusterに入っていなければ，その後続タスク数が1かどうかのチェック
                        if (predTask.getDsucList().size() == 1) {
                            if (this.checkClustering(initialCluster, predTask, freeTask, preTlevel, false)) {
                                //predTaskを，もともとのクラスタから切り離す．
                                preCluster = this.detachCluster(preCluster, predTask);
                                this.count++;

                                //TaskCluster preCluster = this.retApl.findTaskCluster(predTask.getClusterID());
                                //tlevelが減れば，クラスタリング
                                initialCluster = this.clusteringDSC(preCluster, initialCluster);

                            } else {
                                //この時点でtlevelが増えれば，クラスタリングはしない．
                                return initialCluster;
                            }

                        } else {

                            //後続タスク数が2以上の場合は次へ
                            //continue;
                        }
                    }

                }
                //もし先行タスクが，initialClusterに含まれていれば，クラスタチェックをする．

            }
            idx++;

        }

        return initialCluster;


    }

    /**
     * @param cluster
     * @param task
     * @return
     */
    public TaskCluster detachCluster(TaskCluster cluster, AbstractTask task) {
        if (cluster.getTaskSet().getList().size() >= 2) {
            //新規にタスククラスタを形成する．
            TaskCluster newCluster = new TaskCluster(task.getIDVector().get(1));
            task.setClusterID(task.getIDVector().get(1));
            this.retApl.removeTaskCluster(task.getIDVector().get(1));
            this.retApl.addTaskCluster(newCluster);

            //元のクラスタからタスクを削除する．
            cluster.removeTask(task.getIDVector().get(1));

            //元タスククラスタの情報更新
            this.updateInOut(cluster, null);
            CustomIDSet topSet = this.getTopList(cluster);
            cluster.setTop_Set(topSet);
            Iterator<Long> topIte = topSet.iterator();
            cluster.getDestCheckedSet().getObjSet().clear();
            while (topIte.hasNext()) {
                Long topID = topIte.next();
                AbstractTask startTask = this.retApl.findTaskByLastID(topID);
                this.updateDestTaskList2(new CustomIDSet(), startTask, cluster.getClusterID());
            }

            //新規クラスタの情報更新

            newCluster.setLinear(true);
            newCluster.addTask(task.getIDVector().get(1));

            // タスククラスタに対して，各種情報をセットする．
            /**このときは，各クラスタには一つのみのタスクが入るため，
             * 以下のような処理が明示的に可能である．
             */
            //ここで，top/outタスクは，自分自身のみをセットしておく．
            newCluster.setBsucTaskID(task.getIDVector().get(1));


            newCluster.addIn_Set(task.getIDVector().get(1));
            newCluster.setTopTaskID(task.getIDVector().get(1));
            CustomIDSet topSet2 = new CustomIDSet();
            topSet2.add(task.getIDVector().get(1));
            newCluster.setTop_Set(topSet2);

            if (!task.getDsucList().isEmpty()) {
                newCluster.addOut_Set(task.getIDVector().get(1));
            }

            task.setClusterID(task.getIDVector().get(1));
            //クラスタ自体をDAGへ反映
            Long clusterID = this.retApl.addTaskCluster(cluster);
            //タスクに対して，そのクラスタIDをセットする．
            //task.setClusterID(clusterID);

            //この時点で，UEXを格納しておく．
            if (!this.isClusterAboveThreshold(newCluster)) {
                this.uexClusterList.add(newCluster.getClusterID());
            }
            this.retApl.addTaskCluster(newCluster);

            return newCluster;


        } else {

            //単一クラスタであれば，そのまま返す．
            return cluster;
        }

    }

    /**
     * @param initialCluster
     * @param predTask
     * @param freeTask
     * @param preTlevel
     * @return
     */
    public boolean checkClustering(TaskCluster initialCluster, AbstractTask predTask, AbstractTask freeTask, long preTlevel, boolean isInitial) {
        boolean ret = false;
        PriorityQueue<AbstractTask> queue = new PriorityQueue<AbstractTask>(5, new DSC_TlevelComparator());
        Iterator<Long> pTaskIte = initialCluster.getTaskSet().iterator();
        //pivot内のタスクをtlevelの昇順にするためのループ
        while (pTaskIte.hasNext()) {
            Long pTaskID = pTaskIte.next();
            AbstractTask pTask = this.retApl.findTaskByLastID(pTaskID);
            queue.add(pTask);
        }
        //最初のクラスタリングの場合は，freeTaskを優先度リストへ入れる．
        if (isInitial) {
            queue.add(freeTask);
            // freeTask.setClusterID(predTask.getClusterID());
        } else {
            //2番目以降のクラスタリングの場合，freeタスクは既に入っているのでpredTaskを新たに優先度リストへ入れる．
            queue.add(predTask);
        }

        //2番目以降の，クラスタリングチェック処理
        //先行タスクがC_1に含まれていれば，レベルチェック対象．
        //predTask側のクラスタがpivot, 当該クラスタがtargetとなることに注意．
        //まずはこれらをクラスタリングして，本当にtlevelが抑えられるのか？
        //tlevelチェックをする．
        Object[] oa = queue.toArray();
        int len = oa.length;
        //pivot＋freeTaskのtlevelを決定するためのループ
        //この時点で，すでにtlevelの昇順に並び替えられている．
        for (int i = 0; i < len; i++) {
            if (i == 0) {
                continue;
            } else {
                //2番目以降のタスクのtlevel値セット処理
                AbstractTask currentTask = (AbstractTask) oa[i];
                AbstractTask previousTask = (AbstractTask) oa[i - 1];

                //当該タスクが，freeタスクであれば，チェックをする．
                if (currentTask.getIDVector().get(1).longValue() == freeTask.getIDVector().get(1).longValue()) {
                    //freeTaskの出番であれば，tlevelの計算をする．
                    //よって，クラスタリング処理では各タスクのtlevel値を更新する必要がある．
                    Iterator<DataDependence> pIte = currentTask.getDpredList().iterator();
                    //最初のtlevelは，自分よりTlevelが一つ前のタスクのものとする．
                    long sTlevel = previousTask.getTlevel() + this.getInstrunction(currentTask) / this.minSpeed;
                    while (pIte.hasNext()) {
                        DataDependence dpred = pIte.next();
                        AbstractTask ppredTask = this.retApl.findTaskByLastID(dpred.getFromID().get(1));
                        long value = ppredTask.getTlevel() + this.getInstrunction(ppredTask) / this.minSpeed +
                                this.getNWTime(ppredTask.getIDVector().get(1), currentTask.getIDVector().get(1), dpred.getMaxDataSize(), this.minLink);
                        if (value >= sTlevel) {
                            sTlevel = value;
                        }
                    }
                    if (preTlevel >= sTlevel) {
                        ret = true;
                        //freeTask.setTlevel(sTlevel);
                        break;
                    } else {
                        //このときにダメなら，終了する．
                        return false;
                    }

                } else {
                    continue;
                }

            }
        }

        return ret;
    }


    /**
     * @param pivot
     * @param target
     * @return
     */
    public TaskCluster clusteringDSC(TaskCluster pivot, TaskCluster target) {
        /*if(target == null){
           // System.out.println("NULL*");
            return pivot;
        }*/

        if(target == null){
            return pivot;
        }
        //toClusterの全タスク集合を取得する．
        CustomIDSet IDSet = target.getTaskSet();

        Iterator<Long> taskIte = IDSet.iterator();

        //toCluster内のタスクたちに対するループ
        while (taskIte.hasNext()) {
            Long taskID = taskIte.next();
            //fromClusterへタスクを追加する．
            pivot.addTask(taskID);
            //toClusterにあったタスクの所属クラスタの変更
            AbstractTask task = this.retApl.findTaskByLastID(taskID);
            task.setClusterID(pivot.getClusterID());
            //this.retApl.updateTask(task);
        }

        //fromClusterのIn/Outを更新
        this.updateInOut(pivot, target);
        CustomIDSet topSet = this.getTopList(pivot);
        pivot.setTop_Set(topSet);
        Iterator<Long> topIte = topSet.iterator();
        pivot.getDestCheckedSet().getObjSet().clear();
        while (topIte.hasNext()) {
            Long topID = topIte.next();
            AbstractTask startTask = this.retApl.findTaskByLastID(topID);
            this.updateDestTaskList2(new CustomIDSet(), startTask, pivot.getClusterID());
        }
        //そしてretAplへの，fromClusterの反映
        //this.retApl.updateTaskCluster(fromCluster);
        //toClusterの削除

        this.retApl.removeTaskCluster(target.getClusterID());

        //CustomIDSet topSet = this.getTopList(pivot);
        //pivot.setTop_Set(topSet);

        //pivot内のDestCheckSetを空にする
        // pivot.getDestCheckedSet().getObjSet().clear();

        //pivot内のタスクのtlevel値を更新する.
        this.updateTlevelOfPivot(pivot);

        return pivot;

    }

    /**
     * @param pivot
     */
    public void updateTlevelOfPivot(TaskCluster pivot) {
        //boolean ret = false;
        PriorityQueue<AbstractTask> queue = new PriorityQueue<AbstractTask>(5, new DSC_TlevelComparator());
        Iterator<Long> pTaskIte = pivot.getTaskSet().iterator();
        //pivot内のタスクをtlevelの昇順にするためのループ
        while (pTaskIte.hasNext()) {
            Long pTaskID = pTaskIte.next();
            AbstractTask pTask = this.retApl.findTaskByLastID(pTaskID);
            queue.add(pTask);
        }

        Object[] oa = queue.toArray();
        int len = oa.length;
        //pivot＋freeTaskのtlevelを決定するためのループ
        //この時点で，すでにtlevelの昇順に並び替えられている．
        for (int i = 0; i < len; i++) {
            if (i == 0) {
                continue;
            } else {
                //2番目以降のタスクのtlevel値セット処理
                AbstractTask currentTask = (AbstractTask) oa[i];
                AbstractTask previousTask = (AbstractTask) oa[i - 1];
                ///AbstractTask currentTask = this.retApl.findTaskByLastID(currentTaskTmp.getIDVector().get(1));

                //当該タスクが，freeタスクであれば，チェックをする．

                //freeTaskの出番であれば，tlevelの計算をする．
                //よって，クラスタリング処理では各タスクのtlevel値を更新する必要がある．
                Iterator<DataDependence> pIte = currentTask.getDpredList().iterator();
                //最初のtlevelは，自分よりTlevelが一つ前のタスクのものとする．
                long sTlevel = previousTask.getTlevel() + this.getInstrunction(previousTask) / this.minSpeed;
                //Vector<Long> tpredID = null;
                Vector<Long> tpredID = currentTask.getTpred();
                while (pIte.hasNext()) {
                    DataDependence dpred = pIte.next();
                    AbstractTask ppredTask = this.retApl.findTaskByLastID(dpred.getFromID().get(1));
                    long value = ppredTask.getTlevel() + this.getInstrunction(ppredTask) / this.minSpeed +
                            this.getNWTime(ppredTask.getIDVector().get(1), currentTask.getIDVector().get(1), dpred.getMaxDataSize(), this.minLink);
                    if (value >= sTlevel) {
                        sTlevel = value;
                        tpredID = ppredTask.getIDVector();

                    }
                }
                currentTask.setTlevel(sTlevel);
                currentTask.setTpred(tpredID);
            }
        }

    }

}
