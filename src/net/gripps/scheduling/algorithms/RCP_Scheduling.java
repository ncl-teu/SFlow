package net.gripps.scheduling.algorithms;

import net.gripps.clustering.common.aplmodel.*;
import net.gripps.environment.Environment;
import net.gripps.scheduling.common.TaskPriorityTlevelComparator;

import java.util.*;

/**
 * RCP(Ready Critocal Path: T. Yang's Algorithm)スケジューリング実装です．
 * レディタスクリストから，最もBlevelの大きな順にスケジュールします．
 * レディタスク: 入力データすべて到着している状態．
 * ここでは，「レディタスク」== Freeタスクの中で，もっともPriorityTlevelが小さいタスク集合
 * (Tlevelが等しい）
 *
 * Author: H. Kanemitsu
 * Date: 2008/10/26
 */
public class RCP_Scheduling extends Blevel_FirstScheduling {

    /**
     * FREEとなるタスクのリストです．
     *
     */
    protected CustomIDSet freeIDList;

    /**                                ｋ
     * 未スケジュールのタスクリスト
     */
    protected CustomIDSet unScheduledIDList;

    /**
     *
     * @param filename
     * @param apl
     * @param env
     */
    public RCP_Scheduling(String filename, BBTask apl, Environment env){
        super(filename, apl, env);
        this.freeIDList = new CustomIDSet();
        this.unScheduledIDList = new CustomIDSet();
    }

    public RCP_Scheduling() {
        super();
    }

    /**
      * スケジュール前処理を行います．
      * ここでは，DAG内のblevel値の計算を行います．
      */
     public void initialize() {

         int size = this.retApl.getTaskList().size();
         //ENDタスクを取得する．
         AbstractTask endTask = this.retApl.findTaskByLastID(new Long(size));

         //priorityTlevelをセットする．
         this.calcPriorityTlevel(endTask, false);

         CustomIDSet startIDSet = this.retApl.getStartTaskSet();
         Iterator<Long> startTaskIte = startIDSet.iterator();

         //STARTタスクに対するループです．
         while (startTaskIte.hasNext()) {
             Long startTaskID = startTaskIte.next();
             //STARTタスクを取得する．
             AbstractTask startTask = this.retApl.findTaskByLastID(startTaskID);
             //各スタートタスクに対し，PriorityBlevelをセットする．
             this.calcPriorityBlevel(startTask, false);
         }

     }




    /**
     *
     */
    public void preProcess(){
        //priorityTlevel, priorityBlevelをそれぞれセットする．
        this.initialize();
        Iterator<Long> startIDIte = this.retApl.getStartTaskSet().iterator();
        //STARTタスクたちに対するループ

        //まずは，STARTタスクをFREEタスクとする．
        while(startIDIte.hasNext()){
            Long sID = startIDIte.next();
            this.freeIDList.add(sID);
        }

        //まずは全タスクを未スケジュールとする．
        Iterator<AbstractTask> taskIte = this.retApl.getTaskList().values().iterator();
        while(taskIte.hasNext()){
            AbstractTask task = taskIte.next();
            this.unScheduledIDList.add(task.getIDVector().get(1));
        }


    }


    /**
     * スケジュール実装です．
     *
     * @return
     */
    public BBTask schedule(){
        this.preProcess();
        //未スケジュールタスクがなくなるまでのループ
        while(!this.unScheduledIDList.isEmpty()){
            //Freeリストから，Readyタスクを取得する．
            AbstractTask readyTask = this.getReadyTask();
            //readyタスクの後続タスクたちのTleveを更新する．
            this.updateSucTasks(readyTask);

        }
        this.calcMakeSpan();
        return this.retApl;


    }



    /**
     * 後続タスクたちのTlevelを更新します．
     * もしすべての入力辺がチェック済みであれば，Freeリストへ追加します．
     *
     * @param task
     */
    public void updateSucTasks(AbstractTask task){
        Long clusterID = task.getClusterID();
        TaskCluster cluster = this.retApl.findTaskCluster(clusterID);

        //後続タスクに対するループ
        Iterator<DataDependence> dsucIte = task.getDsucList().iterator();
        while(dsucIte.hasNext()){
            DataDependence dsuc = dsucIte.next();
            //後続タスクを取得する．
            AbstractTask sucTask = this.retApl.findTaskByLastID(dsuc.getToID().get(1));
            //後続タスクの入力辺をチェック済みとする．
            DataDependence dpred = sucTask.findDDFromDpredList(task.getIDVector(), sucTask.getIDVector());
            dpred.setReady(true);
            //もし後続タスクがFREEとなれば，FREEリストへ追加する
            if((this.isFreeTask(sucTask)) && (this.unScheduledIDList.contains(sucTask.getIDVector().get(1)))){
                this.freeIDList.add(sucTask.getIDVector().get(1));
            }
            //後続タスクのレベル値を更新する．
            this.updatePriorityTlevel(sucTask);

        }


    }

    /**
     *
     * @param task
     */
    public void updatePriorityTlevel(AbstractTask task){
        Iterator<DataDependence> dpredIte = task.getDpredList().iterator();
        TaskCluster sucCluster = this.retApl.findTaskCluster(task.getClusterID());

        long tlevel = 0;
        TaskCluster cls = this.retApl.findTaskCluster(task.getClusterID());

        while(dpredIte.hasNext()){
            DataDependence dpred = dpredIte.next();
            //先行タスクを取得する．
            AbstractTask predTask = this.retApl.findTaskByLastID(dpred.getFromID().get(1));
            //先行タスクが属するタスククラスタを取得する．
            TaskCluster predCluster = this.retApl.findTaskCluster(predTask.getClusterID());
            long value = 0;
            if(this.isHetero()){
                long nw_time = 0;
                if(cls.getCPU().getMachineID() == predCluster.getCPU().getMachineID()){

                }else{
                    nw_time = this.env.getSetupTime()+dpred.getMaxDataSize()/this.env.getNWLink(predCluster.getCPU().getCpuID(), cls.getCPU().getCpuID());
                }
                value = predTask.getPriorityTlevel() + (this.getInstrunction(predTask)/predCluster.getCPU().getSpeed()) + nw_time;

                                    //this.getNWTime(predTask.getIDVector().get(1), task.getIDVector().get(1), dpred.getMaxDataSize());

            }else{
                value = predTask.getPriorityTlevel() + (this.getInstrunction(predTask)/predCluster.getCPU().getSpeed()) +
                                   this.getNWTime(predTask.getIDVector().get(1), task.getIDVector().get(1), dpred.getMaxDataSize());
            }

            if(value >= tlevel){
                tlevel = value;

            }
        }
        task.setPriorityTlevel(tlevel);

    }

    /**
     *
     * @param task
     * @return
     */
    public boolean isFreeTask(AbstractTask task){
        //入力辺を取得
        Iterator<DataDependence> dpredIte = task.getDpredList().iterator();

        boolean ret = true;
        while(dpredIte.hasNext()){
            DataDependence dpred = dpredIte.next();
            if(!dpred.isReady()){
                ret = false;
                break;
            }
        }

        return ret;
    }

    /**
     *
     * @return
     */
    public AbstractTask getReadyTask(){
        //Freeリストから，もっともTlevelが小さいものを選択する．
        //もし複数あれば，その中から最もblevelの大きいものをReadyタスクとする．
        Iterator<Long> freeIte = this.freeIDList.iterator();
        CustomIDSet readySet = new CustomIDSet();

        ArrayList<AbstractTask> array = new ArrayList<AbstractTask>();
        while(freeIte.hasNext()){
            Long freeID = freeIte.next();
            AbstractTask freeTask = this.retApl.findTaskByLastID(freeID);
            array.add(freeTask);
        }


        Object[] oa = array.toArray();
        //Tlevelの小さい順→blevelの大きい順にソートする．
        Arrays.sort(oa, new TaskPriorityTlevelComparator());
        //先頭のタスクをレディタスクとする．
        AbstractTask tmpReadyTask = this.retApl.findTaskByLastID(((AbstractTask)oa[0]).getIDVector().get(1));
        int cnt = array.size();
        long pTlevel = tmpReadyTask.getPriorityTlevel();
        long pBlevel = tmpReadyTask.getPriorityBlevel();

        AbstractTask readyTask = tmpReadyTask;
        for(int i=1;i<cnt;i++){
            AbstractTask tmpTask = this.retApl.findTaskByLastID(((AbstractTask)oa[i]).getIDVector().get(1));
            if(tmpTask.getPriorityTlevel() == pTlevel){
                 if(pBlevel < tmpTask.getPriorityBlevel()){
                     readyTask = tmpTask;
                 }else{
                     break;
                 }
            }
        }

        TaskCluster cluster = this.retApl.findTaskCluster(readyTask.getClusterID());
        LinkedList<Long> schedList = cluster.getScheduledTaskList();
        //クラスタの中に，スケジュール順を追加しておく．
        schedList.add(readyTask.getIDVector().get(1));
        if(schedList.size() <= 1){

        }else{
            int len = schedList.size();
            //後のタスクに対して，仮想的な辺をつける．
            //この段階では，どのタスクが次に実行されるかは分からない．
            //したがって，未スケジュールなタスク全てに対して辺をつける．
            Iterator<Long> taskIte = cluster.getTaskSet().iterator();
            Long preTaskID = schedList.get(len - 1);
            AbstractTask preTask = this.retApl.findTaskByLastID(preTaskID);

           // this.addVirtualEdge(preTask, readyTask);

            //long start = System.currentTimeMillis();
            while(taskIte.hasNext()){
                Long taskID = taskIte.next();
                AbstractTask task = this.retApl.findTaskByLastID(taskID);
                //もしスケジュール済みタスクであれば，何もしない
                if(schedList.contains(taskID)){

                }else{
                    //未スケジュールなタスクであれば，仮想的な辺を設ける．
                    this.addVirtualEdge(readyTask, task);
                }
            }
           // long end = System.currentTimeMillis();
           // System.out.println("TIME: "+(end-start));


        }
        //そして，FREEからレディタスクを削除する．
        this.freeIDList.remove(readyTask.getIDVector().get(1));
        //未スケジュールタスクリストからレディタスクを削除する．
        this.unScheduledIDList.remove(readyTask.getIDVector().get(1));

        return readyTask;
        

    }





}
