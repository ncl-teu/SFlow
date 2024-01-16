package net.gripps.clustering.algorithms.cassII;

import net.gripps.clustering.algorithms.AbstractClusteringAlgorithm;
import net.gripps.clustering.common.aplmodel.TaskCluster;
import net.gripps.clustering.common.aplmodel.*;

import java.util.*;

/**
 * Author: H. Kanemitsu
 * Date: 2008/02/09
 */
public class CASSII_Algorithm extends AbstractClusteringAlgorithm {

    private long longestPath;

    /**
     * Lontest Pathに含まれるタスククラスタのリスト
     */
    private CustomIDSet lpIDList;

    private String filename;

    private int taskNum;


    public CASSII_Algorithm(BBTask task) {
        super(task);
        this.freeClusterList = new CustomIDSet();
       // this.uexClusterList = new CustomIDSet();
    }


    /**
     * @param task
     * @param file
     */
    public CASSII_Algorithm(BBTask task, String file) {
        super(task, file, 2);

    }


    public long getLongestPath() {
        return longestPath;
    }

    public void setLongestPath(long longestPath) {
        this.longestPath = longestPath;
    }


    public CustomIDSet getUexClusterList() {
        return uexClusterList;
    }

    public void setUexClusterList(CustomIDSet uexClusterList) {
        this.uexClusterList = uexClusterList;
    }

    public CustomIDSet getLpTaskList() {
        return lpIDList;
    }

    public void setLpTaskList(CustomIDSet lpIDList) {
        this.lpIDList = lpIDList;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public int getTaskNum() {
        return taskNum;
    }

    public void setTaskNum(int taskNum) {
        this.taskNum = taskNum;
    }


    public BBTask process() {
        this.prepare();
                        int size = this.retApl.getTaskList().size();
        AbstractTask endTask = this.retApl.findTaskByLastID(new Long(size));
        this.retApl.setCpLen(endTask.getTlevel() + endTask.getBlevel());
       /* Iterator<AbstractTask> taskIte = this.retApl.taskIerator();
        long value = 0;
        long comm = 0;
        long edgeNum = 0;
        while (taskIte.hasNext()) {
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
        System.out.println("****CASS-II Algorithm Only****");
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

        long start = System.currentTimeMillis();
        */
        long start = System.currentTimeMillis();
        this.mainProcess();
       long end = System.currentTimeMillis();
        retApl.setProcessTime((end-start));
        //System.out.println("CASS TIME: " + (end - start) + "msec");

        //this.calcLevel();
       // this.calcMakeSpan();
        //System.out.println("****前****");
        // this.postProcess();
        //System.out.println("LB前のMAKESPAN: "+ this.makeSpan);
        //this.mainProcessLB();
        // this.printDAG(this.retApl);
        // System.out.println("TIME: "+(end-start));
        //System.out.println("****後****");
        this.postProcess();

        return this.retApl;


    }

    /**
     *
     */
    public void calcMakeSpan() {
        Iterator<AbstractTask> taskIte = this.retApl.taskIerator();
        long blevel = 0;

        while (taskIte.hasNext()) {
            AbstractTask task = taskIte.next();
            //もしstartタスクであれば，レベルの取得をする.
            if (task.getDpredList().isEmpty()) {
                if (blevel <= task.getBlevel()) {
                    blevel = task.getBlevel();
                }

            }

        }
        this.makeSpan = blevel;
    }


    /**
     * CASS-IIアルゴリズム
     *
     * @param pivot
     * @param target
     * @return
     */
    public TaskCluster clusteringClusterCASSII(TaskCluster pivot, TaskCluster target) {
        //System.out.println("PIVOTのタスク数" + pivot.getObjSet().getList().size());
        Long oneTaskID = pivot.getTaskSet().getList().get(0);
        AbstractTask pTask = this.retApl.findTaskByLastID(oneTaskID);

        //toClusterの全タスク集合を取得する．
        CustomIDSet IDSet = target.getTaskSet();
        Iterator<Long> taskIte = IDSet.iterator();

        AbstractTask targetTopTask = this.retApl.findTaskByLastID(target.getTopTaskID());
        long blevel = this.getClusterInstruction(pivot)/this.minSpeed + target.getBlevel();

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
        //そしてretAplへの，fromClusterの反映
        //this.retApl.updateTaskCluster(fromCluster);
        //toClusterの削除
        this.retApl.removeTaskCluster(target.getClusterID());
        CustomIDSet topSet = this.getTopList(pivot);
        pivot.setTop_Set(topSet);
        Iterator<Long> topIte = topSet.iterator();

        //pivot内のDestCheckSetを空にする
        pivot.getDestCheckedSet().getObjSet().clear();

        long start = System.currentTimeMillis();
        /*  while(topIte.hasNext()){
            Long topID = topIte.next();
            AbstractTask startTask = this.retApl.findTaskByLastID(topID);
            this.updateDestTaskList2(new CustomIDSet(),startTask, pivot.getClusterID());
        }*/
        long end = System.currentTimeMillis();
        //System.out.println("4th: "+ (end-start));
        //   this.updateLevelValue(pivot);
        //クラスタのBlevel値を更新する．

       //pivot内のタスクを取得する．
        Iterator<DataDependence> sucIte = pTask.getDsucList().iterator();

        while(sucIte.hasNext()){
            DataDependence dsuc = sucIte.next();
            AbstractTask sucTask = this.retApl.findTaskByLastID(dsuc.getToID().get(1));
            long value = this.getClusterInstruction(pivot)/this.minSpeed  +
                    this.getNWTime(pTask.getIDVector().get(1),sucTask.getIDVector().get(1),dsuc.getMaxDataSize(), this.minLink) + sucTask.getBlevel();
            if(value >= blevel){
                blevel = value;
            }

        }

        pivot.setBlevel(blevel);

        pTask.setBlevel(blevel);
        //pivot内の単一タスクのblevelを更新する．



        return pivot;

    }


    /**
     * CASS-IIアルゴリズムのメイン処理です．
     * それだけ．
     */
    public void mainProcess() {
        long start = System.currentTimeMillis();

        while (!this.uexClusterList.isEmpty()) {
            // System.out.println("FREE NUM: "+ this.freeClusterList.getList().size());
             //System.out.println("251");
            //Freeクラスタリストから，レベルMaxのクラスタを取得する．
            TaskCluster pivot = this.getMaxLevelCluster(this.freeClusterList);
            if(pivot == null){
                //System.out.println("ERROR");
                break;

            }
            // System.out.println("254");
            //Dominant Successorタスクを取得する．
            //そのために，当該クラスタのタスク(一つのみ）を取得する．
            AbstractTask task = this.retApl.findTaskByLastID(pivot.getTopTaskID());
            if (task.getDsucList().isEmpty()) {
                //ENDクラスタであれば
                //             //UEXクラスタリストからpivot削除する
                this.uexClusterList.remove(pivot.getClusterID());
                this.freeClusterList.remove(pivot.getClusterID());
                //Freeクラスタリストの更新をする．

                //pivotクラスタの先行タスクたちを見る．
                pivot = this.updateFreeClusterList(pivot);

                continue;

            }
            //taskのDominant Successor，つまりBsucタスクを取得する．
            AbstractTask bsTask = this.retApl.findTaskByLastID(task.getBsuc().get(1));

            //bsucタスクが所属するクラスタIDを取得する．
            Long bsClusterID = bsTask.getClusterID();

            //bsucが所属するタスククラスタを取得する．
            TaskCluster target = this.retApl.findTaskCluster(bsClusterID);

            //targetのblevel値を取得する．
            long blevel_target = target.getBlevel();

            //pivotのblevel値を取得する．
            long blevel_pivot = pivot.getBlevel();

            //新blevelをセットする．
            long new_blevel = blevel_target + (this.getClusterInstruction(pivot) / this.minSpeed);

            //pivotのblevelが減れば，受け入れられる．
            if (new_blevel <= blevel_pivot) {
                       //long start5 = System.currentTimeMillis();

                pivot = this.clusteringClusterCASSII(pivot, target);
                            //     long end5 = System.currentTimeMillis();
               //  System.out.println("3rd: "+ (end5-start5));

                AbstractTask topTask = this.retApl.findTaskByLastID(pivot.getTopTaskID());
                //topタスクの後続タスクたちを取得する．
                LinkedList<DataDependence> pDsucList = topTask.getDsucList();
                Iterator<DataDependence> pDsucIte = pDsucList.iterator();
                //topタスクの後続タスクたちに対するループ
                /*while (pDsucIte.hasNext()) {
                    DataDependence dd = pDsucIte.next();
                    //topタスクの後続タスクを取得する．
                    AbstractTask sucTask = this.retApl.findTaskByLastID(dd.getToID().get(1));
                    long value = this.getInstrunction(topTask) / this.minSpeed +
                            this.getNWTime(topTask.getIDVector().get(1), sucTask.getIDVector().get(1), dd.getMaxDataSize(), this.minLink) + sucTask.getBlevel();
                    if (value >= new_blevel) {
                        new_blevel = value;
                        topTask.setBsuc(sucTask.getIDVector());
                    }

                }

                //taskのblevelを更新する．
                task.setBlevel(new_blevel);
                //pivotのblevelを更新する．
                pivot.setBlevel(new_blevel);
                */

            } else {
                //pivotのblevelが増えれば，pivotは独立クラスタとなる．
                 //System.out.println("ダメ");
            }

            this.freeClusterList.remove(pivot.getClusterID());
            this.freeClusterList.remove(target.getClusterID());



            //UEXクラスタリストからのtargetを削除する
            this.uexClusterList.remove(target.getClusterID());

            //UEXクラスタリストからpivot削除する
            this.uexClusterList.remove(pivot.getClusterID());

            ///             long start = System.currentTimeMillis();
            //Freeクラスタリストの更新をする．
            //pivotクラスタの先行タスクたちを見る．
           // long start2 = System.currentTimeMillis();
            pivot = this.updateFreeClusterList(pivot);
            //            long end = System.currentTimeMillis();

           // long end2 = System.currentTimeMillis();
           // System.out.println("TIME: "+ (end2-start2));

            //freeクラスタリストのblevel, bsuc値を更新する．

            this.updateBlevelofFree(pivot);


            //System.out.println("PIVOTのタスク数: "+ pivot.getObjSet().getList().size());


        }
        long end = System.currentTimeMillis();
        //System.out.println("クラスタリング時間: "+ (end-start));
        //各クラスタ内タスクの，DestSetを更新する．

        Iterator<TaskCluster> cIte = this.retApl.clusterIterator();
        while (cIte.hasNext()) {
            TaskCluster cluster = cIte.next();
            CustomIDSet startSet = cluster.getTop_Set();
            Iterator<Long> topIte = startSet.iterator();
            while (topIte.hasNext()) {
                Long topID = topIte.next();
                AbstractTask startTask = this.retApl.findTaskByLastID(topID);
                this.updateDestTaskList2(new CustomIDSet(), startTask, cluster.getClusterID());
            }

        }


    }

    public void updateBlevelofFree(TaskCluster pivot) {
        //pivotのINタスク集合を取得する．
        CustomIDSet inSet = pivot.getIn_Set();
        Iterator<Long> inIte = inSet.iterator();
        CustomIDSet clusterSet = new CustomIDSet();

        //Inセットに対するループ
        while (inIte.hasNext()) {
            Long inID = inIte.next();
            //Inタスクを取得する．
            AbstractTask inTask = this.retApl.findTaskByLastID(inID);
            //INタスクの先行タスクたちを取得する．
            LinkedList<DataDependence> dpredList = inTask.getDpredList();
            Iterator<DataDependence> dpredIte = dpredList.iterator();
            //一つのInセットの，先行タスクたちに対するループ
            while (dpredIte.hasNext()) {
                DataDependence dd = dpredIte.next();
                //先行タスクを取得する．
                AbstractTask predTask = this.retApl.findTaskByLastID(dd.getFromID().get(1));
                //先行タスクの所属クラスタがfreeクラスタに含まれていれば，blevel, bsucの更新をする．
                if ((this.freeClusterList.contains(predTask.getClusterID())) && (!clusterSet.contains(predTask.getClusterID()))) {
                    LinkedList<DataDependence> dsucList = predTask.getDsucList();
                    Iterator<DataDependence> dsucIte = dsucList.iterator();
                    long blevel = 0;
                    Vector<Long> bsucID = null;

                    //freeクラスタ（freeタスク）の後続タスクに対するループ
                    while (dsucIte.hasNext()) {
                        DataDependence dsucDD = dsucIte.next();
                        //freeタスクの後続タスクを取得する．
                        AbstractTask sucTask = this.retApl.findTaskByLastID(dsucDD.getToID().get(1));
                        long tmpValue = this.getInstrunction(predTask) / this.minSpeed +
                                this.getNWTime(predTask.getIDVector().get(1), sucTask.getIDVector().get(1), dsucDD.getMaxDataSize(), this.minLink) + sucTask.getBlevel();
                        if (tmpValue >= blevel) {
                            blevel = tmpValue;
                            bsucID = sucTask.getIDVector();
                        }
                    }
                    predTask.setBlevel(blevel);
                    predTask.setBsuc(bsucID);
                    clusterSet.add(predTask.getClusterID());
                }
            }
        }
    }


    /**
     * @param pivot
     * @return
     */
    public TaskCluster updateFreeClusterList(TaskCluster pivot) {

        CustomIDSet clsSet = new CustomIDSet();

        CustomIDSet pInSet = pivot.getIn_Set();
        Iterator<Long> pInIte = pInSet.iterator();
        //Inタスクに対するループ
        while (pInIte.hasNext()) {
            Long oID = pInIte.next();
            AbstractTask iTask = this.retApl.findTaskByLastID(oID);
            //Inタスクの先行タスク集合を取得する．
            LinkedList<DataDependence> dpredList = iTask.getDpredList();
            Iterator<DataDependence> dpredIte = dpredList.iterator();
            //Inタスクの，先行タスクたちに対するループ
            while (dpredIte.hasNext()) {
                DataDependence dd = dpredIte.next();
                AbstractTask predTask = this.retApl.findTaskByLastID(dd.getFromID().get(1));
                if (predTask.getClusterID().longValue() != pivot.getClusterID().longValue()) {
                    //predTaskの出力辺に"Checked"フラグをつける．
                    predTask.setCheckedFlgToDsuc(predTask.getIDVector(), iTask.getIDVector(), true);
                    clsSet.add(predTask.getClusterID());
                }
            }
        }

        Iterator<Long> clsIte = clsSet.iterator();
        while (clsIte.hasNext()) {
            Long clusterid = clsIte.next();
            TaskCluster clster = this.retApl.findTaskCluster(clusterid);
            if (this.isAllOutEdgeChecked(this.retApl.findTaskCluster(clusterid))&&(this.uexClusterList.contains(clusterid))) {
                //もしクラスタのInタスクがすべてCheckeであれば，そのクラスタをFreeListへ入れる．
              //  if (this.uexClusterList.contains(clster.getClusterID())) {
              this.freeClusterList.add(clusterid);
              //  }

            }
        }

        return pivot;
    }


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
            //まずは自分自身をDestへ追加する．
            task.addDestTask(task.getIDVector().get(1));

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
            cluster.setTop_Set(topSet);
            if (!task.getDsucList().isEmpty()) {
                cluster.addOut_Set(task.getIDVector().get(1));

            }

            if (task.getDpredList().isEmpty()) {
                startSet.add(task.getIDVector().get(1));
            }

            task.setClusterID(task.getIDVector().get(1));
            //クラスタ自体をDAGへ反映
            Long clusterID = this.retApl.addTaskCluster(cluster);
            //タスクに対して，そのクラスタIDをセットする．
            //task.setClusterID(clusterID);
            if (!this.isClusterAboveThreshold(cluster)) {
                this.uexClusterList.add(clusterID);

            }

            //ENDタスクであれば，Freeリストへ入れる．
            if (task.getDsucList().isEmpty()) {
                this.freeClusterList.add(cluster.getClusterID());

            }
            // this.calculateInitialTlevel(task, false);
            // this.calculateInitialBlevel(task, false);

        }
        this.retApl.setStartTaskSet(startSet);


        long endID = this.retApl.getTaskList().size();
        AbstractTask endTask = this.retApl.findTaskByLastID(new Long(endID));
        this.calculateInitialTlevel(endTask, false);
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


}
