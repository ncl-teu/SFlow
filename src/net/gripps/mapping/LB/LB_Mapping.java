package net.gripps.mapping.LB;

import net.gripps.clustering.common.aplmodel.BBTask;
import net.gripps.clustering.common.aplmodel.TaskCluster;
import net.gripps.environment.CPU;
import net.gripps.environment.Environment;
import net.gripps.mapping.AbstractMapping;

import java.util.PriorityQueue;
import java.util.Iterator;

/**
 * Author: H. Kanemitsu
 * Date: 2008/11/01
 */
public class LB_Mapping extends AbstractMapping {

    /**
     *
     */
    private PriorityQueue<CPU> mQueue;

    /**
     *
     */
    private PriorityQueue<TaskCluster> cQueue;

    /**
     * 
     * @param apl
     * @param fileName
     * @param env
     */
    public LB_Mapping(BBTask apl,String fileName, Environment env){
        super(apl, fileName, env);
        this.mQueue = new PriorityQueue<CPU>(5, new MachineComparator());
        this.cQueue = new PriorityQueue<TaskCluster>(5, new TaskClusterComparator());
    }

    /**
     * オーバーライドメソッドです．
     *
     * @return
     */
    public BBTask mapping(){
        Iterator<CPU> mIte = this.env.getCpuList().values().iterator();

        //マシンを優先度順にするためのループ
        while(mIte.hasNext()){
            CPU CPU = mIte.next();
            this.mQueue.add(CPU);
        }

        //タスククラスタを，そのサイズの大きい順にするためのループ
        Iterator<TaskCluster> cIte = this.retApl.clusterIterator();
        while(cIte.hasNext()){
            TaskCluster cluster = cIte.next();
            this.cQueue.add(cluster);
        }

        //タスククラスタに対するループ
        int len = this.retApl.getTaskClusterList().size();
        for(int i = 0; i< len; i++){
            TaskCluster cluster = this.cQueue.poll();
            CPU CPU = this.mQueue.poll();
            cluster.setCPU(CPU);
            CPU.setTaskClusterID(cluster.getClusterID());
        }
        
        //System.out.println("LB END");
        return this.retApl;


    }
}
