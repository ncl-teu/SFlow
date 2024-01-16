package net.gripps.clustering.algorithms.dsc;

import net.gripps.clustering.common.aplmodel.*;
import net.gripps.mapping.LB.TaskClusterComparator;

import java.util.Iterator;
import java.util.PriorityQueue;

/**
 * Author: H. Kanemitsu
 * Date: 2008/11/02
 */
public class DSC_WPAlgorithm extends DSC_Algorithm{

    private PriorityQueue<TaskCluster> cQueue;
        public DSC_WPAlgorithm(BBTask task) {
        super(task);
        this.freeClusterList = new CustomIDSet();
        this.cQueue = new PriorityQueue<TaskCluster>(5, new TaskClusterComparator());
    }



    /**
     * @param task
     * @param file
     */
    public DSC_WPAlgorithm(BBTask task, String file) {
        super(task,file);
                this.freeClusterList = new CustomIDSet();
        this.cQueue = new PriorityQueue<TaskCluster>(5, new TaskClusterComparator());

    }


    public BBTask  process() {
        this.prepare();
                int size = this.retApl.getTaskList().size();
        AbstractTask endTask = this.retApl.findTaskByLastID(new Long(size));
        this.retApl.setCpLen(endTask.getTlevel() + endTask.getBlevel());

        this.mainProcess();
 
        this.mainProcessWP();
        this.postProcess();

        return this.retApl;


    }

    public void mainProcessWP(){
        Iterator<TaskCluster> allClusterIte = this.retApl.clusterIterator();
        while(allClusterIte.hasNext()){
            TaskCluster cluster = allClusterIte.next();
            this.cQueue.add(cluster);
            this.underThresholdClusterList.add(cluster.getClusterID());

            if(this.isClusterAboveThreshold(cluster)){

            }else{
                cluster.setClusterSize(this.getClusterInstruction(cluster));


            }
        }

        while(!this.underThresholdClusterList.isEmpty()){
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
            
        }
    }

}
