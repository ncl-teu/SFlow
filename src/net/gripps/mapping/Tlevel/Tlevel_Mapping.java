package net.gripps.mapping.Tlevel;

import net.gripps.environment.CPU;
import net.gripps.mapping.AbstractMapping;
import net.gripps.clustering.common.aplmodel.*;
import net.gripps.environment.Environment;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;

/**
 * Author: H. Kanemitsu
 * 各クラスタの先頭タスクのうち，Tlevel値の小さい順に割り当て対象のクラスタを選択する．
 * その後，
 * Date: 2009/05/20
 */
public class Tlevel_Mapping extends AbstractMapping {
    public Tlevel_Mapping(BBTask task, String file) {
        super(task, file);
    }

    public Tlevel_Mapping(BBTask apl, String file, Environment env) {
        super(apl, file, env);
    }

    /**
     * 未割り当てのクラスタ集合から，topタスクのtlevel値がminのクラスタを取得する．
     * 
     * @return
     */
    public  TaskCluster selectTaskCluster(){
        Iterator<Long> cIte = this.umClusterSet.iterator();
        long currentTlevel = 10000000;
        TaskCluster retCluster = null;

       //クラスタごとのループ
        while(cIte.hasNext()){
            Long cID = cIte.next();
            TaskCluster cluster = this.retApl.findTaskCluster(cID);
            CustomIDSet tSet = cluster.getTop_Set();
            Iterator<Long> tIte = tSet.iterator();
            long cTlevel = 100000000;
            //Topタスクたちに対するループ
            while(tIte.hasNext()){
                Long tID = tIte.next();
                AbstractTask topTask = this.retApl.findTaskByLastID(tID);
                long tmpTlevel = topTask.getTlevel();
                if(cTlevel >= tmpTlevel){
                    cTlevel = tmpTlevel;
                }
            }
            if(currentTlevel >= cTlevel){
                currentTlevel = cTlevel;
                retCluster = cluster;
            }
        }
        return retCluster;
    }

    /**
     *
     * @return
     */
    public BBTask mapping(){
        long start2 = System.currentTimeMillis();
        //未割り当てクラスタが存在する間のループ
        while(!this.umClusterSet.isEmpty()){
            //tlevel値がMinのクラスタを取得する．
            TaskCluster cluster = this.selectTaskCluster();
            if(cluster == null){
                break;
            }
            //次は，マシンの選択を行う．現在割り当て済み＋これから割り当てようとするCPUでのメイクスパンを最小にする
            //CPUを割り当て先CPUとする．
            //具体的には，「割り当て済みクラスタのmax{bottomタスクの完了時刻}を最小にするCPUのこと．
            Iterator<Long> uMappedCPUIte = this.unMappedCPU.iterator();
            long makeSpan = 1000000000;
            CPU retCPU = null;
            long start = System.currentTimeMillis();
            this.mappedClusterSet.add(cluster.getClusterID());

            while(uMappedCPUIte.hasNext()){
                Long cpuID = uMappedCPUIte.next();
                // long s5 = System.currentTimeMillis();
                CPU mac = this.env.findCPU(cpuID);
                 //long e5 = System.currentTimeMillis();
                //System.out.println("TIME: "+ (e5-s5));
                //マシンを割り当ててみる．
                cluster.setCPU(mac);

                //部分スケジュール上でのメイクスパンを取得する．
                long tmpMakeSpan = this.calcMakeSpan(this.retApl, this.mappedClusterSet, cluster.getClusterID());



                if(makeSpan >= tmpMakeSpan){
                    makeSpan = tmpMakeSpan;
                    retCPU = mac;
                   // cluster.setCPU(mac);
                }
            }
            long end = System.currentTimeMillis();
           // System.out.println("tmpMakeSpan:"+makeSpan+ " /SEC: "+ (end-start));
            //最終的なマシンをセットする．
            cluster.setCPU(retCPU);
            retCPU.setTaskClusterID(cluster.getClusterID());
            retCPU.setTaskClusterID(cluster.getClusterID());
            this.unMappedCPU.remove(retCPU.getCpuID());
            this.umClusterSet.remove(cluster.getClusterID());
            //System.out.println("おわった");
             //tlevel値の更新を行う．
            long endID = this.retApl.getTaskList().size();
            AbstractTask endTask = this.retApl.findTaskByLastID(new Long(endID));
            this.updateTlevel(this.retApl, endTask, new CustomIDSet());
        }
        long end2 = System.currentTimeMillis();
        //System.out.println("Tlevel END: TIME: "+(end2-start2));
                //後処理
        Iterator<AbstractTask> taskIte = this.retApl.taskIerator();
        while(taskIte.hasNext()){
            AbstractTask task = taskIte.next();
            task.setStartTime(-1);
        }



        return this.retApl;
    }

    /**
     * 
     * @param apl
     * @param task
     * @param set
     * @return
     */
    public long updateTlevel(BBTask apl, AbstractTask task, CustomIDSet set) {
        //先行タスクを取得する．
        LinkedList<DataDependence> DpredList = task.getDpredList();
        //先行タスクのサイズを取得する．
        int size = DpredList.size();
        Iterator<DataDependence> dpredIte = DpredList.iterator();
        long retTlevel = 0;

        //すでにtlevel値が計算済みであれば，返す．
        if(set.contains(task.getIDVector().get(1))){
            return task.getTlevel();
        }

        //startタスクであれば，値を0に設定する．
        if (DpredList.size() == 0) {
            task.setTlevel(0);
            set.add(task.getIDVector().get(1));
            return 0;
        }

        long maxValue = 0;
        //先行タスクたちに対するループ
       // for (int i = 0; i < size; i++) {
        while(dpredIte.hasNext()){

            DataDependence dd =dpredIte.next();
            Vector<Long> fromid = dd.getFromID();
           // AbstractTask fromTask = this.findTaskAsTop(fromid);
            AbstractTask fromTask = apl.findTaskByLastID(fromid.get(1));
            TaskCluster cluster = apl.findTaskCluster(fromTask.getClusterID());
            CPU CPU = cluster.getCPU();
            //先行タスクに対して，再帰的に呼び出す．
            long fromTlevel = this.updateTlevel(apl,fromTask,set) + (this.getInstrunction(fromTask) / CPU.getSpeed()) +
                    this.getNWTime(dd.getFromID().get(1), dd.getToID().get(1), dd.getMaxDataSize());
            if (maxValue <= fromTlevel) {
                maxValue = fromTlevel;
            }

        }

        task.setTlevel(maxValue);
        set.add(task.getIDVector().get(1));
        return maxValue;

    }


}
