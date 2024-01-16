package net.gripps.clustering.algorithms;

import net.gripps.clustering.common.aplmodel.BBTask;
import net.gripps.clustering.common.aplmodel.TaskCluster;
import net.gripps.clustering.common.aplmodel.CustomIDSet;
import net.gripps.clustering.common.aplmodel.AbstractTask;
import net.gripps.mapping.LB.TaskClusterComparator;

import java.util.Iterator;
import java.util.PriorityQueue;

/**
 * Author: H. Kanemitsu
 * Date: 2008/11/04
 */
public class PostClusteringManager extends AbstractClusteringAlgorithm{

    /**
     *
     */
    private PriorityQueue<TaskCluster> cQueue;

    private int clusterNum;

    /**
     *
     * @param task
     */
    public PostClusteringManager(BBTask task) {
        super(task);
        this.cQueue = new PriorityQueue<TaskCluster>(5, new TaskClusterComparator());
        this.clusterNum = 0;
    }

    public PostClusteringManager(BBTask task, String file, int num){
        super(task, file, 3);
        this.clusterNum = num;

    }




    public BBTask  process() {
        this.prepare();
        int size = this.retApl.getTaskList().size();
        AbstractTask endTask = this.retApl.findTaskByLastID(new Long(size));
        this.retApl.setCpLen(endTask.getTlevel() + endTask.getBlevel());
  /*      Iterator<AbstractTask> taskIte = this.retApl.taskIerator();
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
        System.out.println("****LB Algorithm****");
        System.out.println(" **初期状態**");
        System.out.println(" - タスク総数: "+ this.retApl.getTaskList().size());
        System.out.println(" - タスクの重み総和: "+ value);
        System.out.println(" - 一台のPCでの実行時間: "+(this.retApl.getMaxWeight() / this.minSpeed));
        System.out.println(" - 辺の総数: "+ edgeNum);
        System.out.println(" - 辺の重み総和: "+ comm);
        int size = this.retApl.getTaskList().size();
        AbstractTask endTask = this.retApl.findTaskByLastID(new Long(size));
        System.out.println(" - 初期APLのCP長: "+ (endTask.getTlevel() + endTask.getBlevel()));
        System.out.println(" **クラスタリング実行後**");

        long start = System.currentTimeMillis();
        */
       return  this.postClusteringLB(this.clusterNum);
       // long end = System.currentTimeMillis();
       // System.out.println(" - クラスタリング処理時間: "+ (end-start) + "msec");
        //this.calcLevel();

       // this.printDAG(this.retApl);
       // System.out.println("TIME: "+(end-start));
        // this.postProcess();

       // return this.retApl;

    }




    /**
     *
     * @param task
     * @param file
     * @param algorithm
     */
    public PostClusteringManager(BBTask task, String file, int algorithm, int num) {
        super(task, file, algorithm);
        this.cQueue = new PriorityQueue<TaskCluster>(5, new TaskClusterComparator());
        this.clusterNum = num;
        Iterator<TaskCluster> clusterIte = this.retApl.clusterIterator();
        while(clusterIte.hasNext()){
            TaskCluster cluster = clusterIte.next();
            this.uexClusterList.add(cluster.getClusterID());
        }
    }


    /**
     * 後処理としてのクラスタリングです．
     * 指定の数のクラスタとなるまで，クラスタリング処理をします．
     * @param clusterNum
     * @return
     */
    public BBTask postClusteringLB(int clusterNum){
        int len = this.retApl.getTaskClusterList().size();


        Iterator<TaskCluster> clusterIte = this.retApl.clusterIterator();
        while(clusterIte.hasNext()){
            TaskCluster cluster = clusterIte.next();
            this.uexClusterList.add(cluster.getClusterID());
        }

        long start = System.currentTimeMillis();
        //クラスタの数が合うまでのクラスタリングループ
        while(clusterNum < len){
            //まず，UEXから最小のクラスタを取得する．
            TaskCluster checkCluster = this.getMinSizeCluster(this.uexClusterList);
            if(checkCluster == null){
                break;
            }
            //後続クラスタたちを取得する．
            //実際にはクラスタIDのリストが入っている
            CustomIDSet sucClusterIDSet = this.getClusterSuc(checkCluster);
            //もしcheckClusterがENDクラスタであれば，先行クラスタをクラスタリングする．
            if(sucClusterIDSet.isEmpty()){
                //this.uexClusterList.remove(checkCluster.getClusterID());
                TaskCluster pivotCluster = this.getClusterPred(checkCluster);
                if(pivotCluster == null){
                    long clusterSize = 1000000000;
                    //TaskCluster newPivot = null;
                    //pivotが取得できなければ，他のクラスタの中から最小のものをpivotとする．
                    Iterator<TaskCluster> remainClusterIte = this.retApl.clusterIterator();
                    while(remainClusterIte.hasNext()){
                        TaskCluster rCluster = remainClusterIte.next();
                        if(rCluster.getClusterID().longValue() == checkCluster.getClusterID().longValue()){
                            continue;
                        }else{
                            long size = this.getClusterInstruction(rCluster);
                            if(size <= clusterSize){
                                clusterSize = size;
                                pivotCluster = rCluster;
                            }
                        }
                    }

                }
                this.clusteringClusterLB(pivotCluster, checkCluster, this.uexClusterList);

                continue;
            }

            //後続クラスタたちのIDを取得する．
            Iterator<Long> sucClsIte =  sucClusterIDSet.iterator();
            long size = 1000000000;
            TaskCluster toCluster = null;
            //サイズが最小の後続クラスタを決定するためのループ
            while(sucClsIte.hasNext()){
                TaskCluster sucCluster = this.retApl.findTaskCluster(sucClsIte.next());
                long value =  this.getClusterInstruction(sucCluster);
                if(value<= size){
                    size =  value;
                    toCluster = sucCluster;
                }
            }
            //そしてクラスタリング処理
            this.clusteringClusterLB(checkCluster,toCluster, this.uexClusterList);
            len = this.retApl.getTaskClusterList().size();

        }
        long end = System.currentTimeMillis();
        long oldtime = retApl.getProcessTime();
        long newtime = oldtime + (end-start);
        retApl.setProcessTime(newtime);
        this.postProcess();
        
        return this.retApl;


    }

    public TaskCluster clusteringClusterLB(TaskCluster fromCluster, TaskCluster toCluster, CustomIDSet targetList) {
        //toClusterの全タスク集合を取得する．
        CustomIDSet IDSet = toCluster.getTaskSet();
        Iterator<Long> taskIte = IDSet.iterator();
        //とりあえずはunderリストから削除する．
        TaskCluster retCluster = null;

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
        targetList.remove(toCluster.getClusterID());
        
        retCluster = fromCluster;

        return retCluster;

    }


    /**
     *
     * @param clusterNum
     * @return
     */
    public BBTask postClusteringWP(int clusterNum){
        int cSize = this.retApl.getTaskClusterList().size();
        Iterator<TaskCluster> allClusterIte = this.retApl.clusterIterator();
        while(allClusterIte.hasNext()){
            TaskCluster cluster = allClusterIte.next();
            this.cQueue.add(cluster);
            this.underThresholdClusterList.add(cluster.getClusterID());

            cluster.setClusterSize(this.getClusterInstruction(cluster));

        }

        while(clusterNum < cSize){
            Object[] oa = this.cQueue.toArray();
            int len = oa.length;

            if(len == 1){
                break;
            }
            //先頭を取得
            TaskCluster c1 = (TaskCluster)oa[0];
            //最後を取得
            TaskCluster c2 = (TaskCluster)oa[len-1];
            this.cQueue.remove(c1);
            this.cQueue.remove(c2);

            if((this.underThresholdClusterList.contains(c1.getClusterID())) || (this.underThresholdClusterList.contains(c2.getClusterID()))){
                c1 = this.clusteringClusterLB(c1, c2,this.underThresholdClusterList);
                c1.setClusterSize(this.getClusterInstruction(c1));
                if(this.underThresholdClusterList.contains(c1.getClusterID())){
                    this.cQueue.add(c1);
                }

            }else{
                continue;
            }
            cSize = this.retApl.getTaskClusterList().size();

        }

        this.postProcess();
        return this.retApl;

    }

    public void updateQueue(){
        int cSize = this.retApl.getTaskClusterList().size();
        Iterator<TaskCluster> allClusterIte = this.retApl.clusterIterator();
        while(allClusterIte.hasNext()){
            TaskCluster cluster = allClusterIte.next();
            this.cQueue.add(cluster);
            this.underThresholdClusterList.add(cluster.getClusterID());

            cluster.setClusterSize(this.getClusterInstruction(cluster));

        }
    }
    public BBTask postClusteringWP2(int clusterNum){

        int cSize = this.retApl.getTaskClusterList().size();
        this.updateQueue();
        while(clusterNum < cSize){
            if(this.cQueue.size() <= 1){
                this.updateQueue();
                cSize = this.retApl.getTaskClusterList().size();
                continue;
            }
            Object[] oa = this.cQueue.toArray();
            int len = oa.length;
            if(len == 1){
                break;
            }


            //先頭を取得
           // TaskCluster c1 = this.retApl.findTaskCluster(((TaskCluster)oa[0]).getClusterID());
            //最後を取得
         //   TaskCluster c2 = this.retApl.findTaskCluster(((TaskCluster)oa[len-1]).getClusterID());
                        //先頭を取得
            TaskCluster c1 = (TaskCluster)oa[0];
            //最後を取得
            TaskCluster c2 = (TaskCluster)oa[len-1];
            this.cQueue.remove(c1);
            this.cQueue.remove(c2);

         //   if((this.underThresholdClusterList.contains(c1.getClusterID())) || (this.underThresholdClusterList.contains(c2.getClusterID()))){
                c1 = this.clusteringClusterLB(c1, c2,this.underThresholdClusterList);
                c1.setClusterSize(this.getClusterInstruction(c1));

           // }else{
               // continue;
          //  }
            cSize = this.retApl.getTaskClusterList().size();

        }

        this.postProcess();
        return this.retApl;

    }



}
