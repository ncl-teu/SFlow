package net.gripps.clustering.algorithms.dsc;

import net.gripps.clustering.common.aplmodel.*;

import java.util.Iterator;

/**
 * Author: H. Kanemitsu
 * Date: 2008/11/01
 */
public class DSC_LBAlgorithm extends DSC_Algorithm{

    public DSC_LBAlgorithm(BBTask task) {
        super(task);
        this.freeClusterList = new CustomIDSet();
    }



    /**
     * @param task
     * @param file
     */
    public DSC_LBAlgorithm(BBTask task, String file) {
        super(task,file);

    }


    /**
     *
     */
    public void mainProcessLB(){
        //まずはUnderリストへの追加処理
        Iterator<TaskCluster> allClusterIte = this.retApl.clusterIterator();
        while(allClusterIte.hasNext()){
            TaskCluster cluster = allClusterIte.next();
            if(this.isClusterAboveThreshold(cluster)){

            }else{
                this.underThresholdClusterList.add(cluster.getClusterID());
            }
        }

                //System.out.println("満たさない数: "+this.underThresholdClusterList.getList().size());


        while(!this.underThresholdClusterList.isEmpty()){
            //まず，UEXから最小のクラスタを取得する．
            TaskCluster checkCluster = this.getMinSizeCluster(this.underThresholdClusterList);
            if(checkCluster == null){
                return;
            }
            //後続クラスタたちを取得する．
            //実際にはクラスタIDのリストが入っている
            CustomIDSet sucClusterIDSet = this.getClusterSuc(checkCluster);
            //もしcheckClusterがENDクラスタであれば，先行クラスタをクラスタリングする．
            if(sucClusterIDSet.isEmpty()){
                //this.uexClusterList.remove(checkCluster.getClusterID());
                TaskCluster pivotCluster = this.getClusterPred(checkCluster);
//                TaskCluster retCluster = this.clusteringClusterLB(pivotCluster, checkCluster, this.underThresholdClusterList);
            /*    if(this.isAvobeThreshold(retCluster)){
                    this.underThresholdClusterList.remove(checkCluster.getClusterID());
                }else{
                    this.underThresholdClusterList.add(checkCluster.getClusterID());
                }
                */
                continue;
            }

            //後続クラスタたちのIDを取得する．
            Iterator<Long> sucClsIte =  sucClusterIDSet.iterator();
            long size = 10000000;
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
            TaskCluster retCluster = this.clusteringClusterLB(checkCluster,toCluster, this.underThresholdClusterList);
            /*if(this.isAvobeThreshold(retCluster)){
                this.underThresholdClusterList.remove(checkCluster.getClusterID());
            }else{
                this.underThresholdClusterList.add(checkCluster.getClusterID());
            }*/
        }

    }





    public BBTask  process() {
        this.prepare();

        int size = this.retApl.getTaskList().size();
        AbstractTask endTask = this.retApl.findTaskByLastID(new Long(size));
        this.retApl.setCpLen(endTask.getTlevel() + endTask.getBlevel());

        this.mainProcess();
        this.mainProcessLB();
       // this.printDAG(this.retApl);
       // System.out.println("TIME: "+(end-start));
        //System.out.println("****後****");
        this.postProcess();

        return this.retApl;


    }



}
