package net.gripps.scheduling.algorithms;

import net.gripps.environment.CPU;
import net.gripps.scheduling.AbstractTaskSchedulingAlgorithm;
import net.gripps.scheduling.common.TaskPriorityTlevelComparator;
import net.gripps.scheduling.common.TaskTlevelComparator;
import net.gripps.clustering.common.aplmodel.*;
import net.gripps.environment.Environment;

import java.util.*;

/**
 * Author: H. Kanemitsu
 * Date: 2008/10/27
 */
public class Tlevel_FirstScheduling extends AbstractTaskSchedulingAlgorithm {

    /**
     *
     * @param fileName
     * @param apl
     * @param env
     */
    public Tlevel_FirstScheduling(String fileName, BBTask apl, Environment env){
        super(fileName, apl,env);
    }

    /**
     *
     */
    public Tlevel_FirstScheduling() {
        super();
    }

    /**
     * 
     */
    public void initialize(){
        int size = this.retApl.getTaskList().size();
        //ENDタスクを取得する．
        AbstractTask endTask = this.retApl.findTaskByLastID(new Long(size));

        //priorityTlevelをセットする．
        this.calcPriorityTlevel(endTask, false);

    }

    /**
     *
     * @return
     */
    public BBTask schedule(){
        this.initialize();
                //そして，各クラスタに対して，tlevelの小さい順に実行させるようにする．
        //そのためには，blevelの大きい順のリストを作る必要がある．
        Iterator<TaskCluster> clusterIte = this.retApl.clusterIterator();
        //各タスククラスタに対するループ処理
        while(clusterIte.hasNext()){
            TaskCluster cluster = clusterIte.next();
            //タスクリストを取得する．
            Iterator<Long> taskIDIte = cluster.getTaskSet().iterator();
            ArrayList<AbstractTask> array = new ArrayList<AbstractTask>();

            while(taskIDIte.hasNext()){
                Long taskID = taskIDIte.next();
                AbstractTask task = this.retApl.findTaskByLastID(taskID);
                array.add(task);
            }

            Object[] oa = array.toArray();
            //ソートする．
            Arrays.sort(oa, new TaskPriorityTlevelComparator());


            //そして，データ依存辺を仮想的に加える．
            int len = oa.length;
            for(int i=0;i<len;i++){
                if(i == len-1){
                    break;
                }
                Long predID =( (AbstractTask)oa[i]).getIDVector().get(1);
                Long sucID =( (AbstractTask)oa[i+1]).getIDVector().get(1);

                AbstractTask predTask = this.retApl.findTaskByLastID(predID);
                AbstractTask sucTask = this.retApl.findTaskByLastID(sucID);

                this.addVirtualEdge(predTask, sucTask);

            }
        }
       this.calcMakeSpan();


        return this.retApl;
    }

    /**
     * 指定クラスタに対して未マップCPUを割り当てたときの，
     * スケジューリング後の指定クラスタ集合のメイクスパンを計算します．
     * 割り当て済みクラスタ＋当該クラスタでの，メイクスパンを計算する．
     * スケジュールするのは，「当該クラスタ」のみである．
     * これは，スケジューリングにかかる計算量を抑えることが目的．
     * マッピング済みクラスタのスケジュールはしないけど，レベル(tlevel)+開始時刻の
     * 更新は全タスク に対して必要．
     *
     * @param apl               APL
     * @return                  メイクスパン
     */
    public long partialSchedule(BBTask apl,
                                CustomIDSet mappedClusterSet,
                                Long clusterID){
        
        long endID = apl.getTaskList().size();
        AbstractTask endTask = apl.findTaskByLastID(new Long(endID));
        this.updateTlevel(apl, endTask, new CustomIDSet());

        //そして，各クラスタに対して，tlevelの大きい順に実行させるようにする．
        //そのためには，tlevelの大きい順のリストを作る必要がある．
        Iterator<TaskCluster> clusterIte = apl.clusterIterator();

        //各タスククラスタに対するループ処理
        //当該クラスタの，実行順を決める．
        TaskCluster cluster = apl.findTaskCluster(clusterID);
        //タスクリストを取得する．
        Iterator<Long> taskIDIte = cluster.getTaskSet().iterator();
        ArrayList<AbstractTask> array = new ArrayList<AbstractTask>();

        while(taskIDIte.hasNext()){
            Long taskID = taskIDIte.next();
            AbstractTask task = apl.findTaskByLastID(taskID);
            array.add(task);
        }

        Object[] oa = array.toArray();
        //当該クラスタ内で，タスクのtlevel順にソートする．
        Arrays.sort(oa, new TaskTlevelComparator());


        //そして，実行順を決める．
        int len = oa.length;
        LinkedList<AbstractTask> pSchedTaskList = cluster.getPSchedSet();
        //いったん初期化する．
        pSchedTaskList.clear();
        for(int i=0;i<len;i++){
            if(i == len-1){
                break;
            }
            //先行タスク                                        .
            Long predID =( (AbstractTask)oa[i]).getIDVector().get(1);
            //後続タスク
            Long sucID =( (AbstractTask)oa[i+1]).getIDVector().get(1);

            //先行タスク
            AbstractTask predTask = apl.findTaskByLastID(predID);
            //後続タスク
            AbstractTask sucTask = apl.findTaskByLastID(sucID);

            //先行タスク，後続タスクのIDをPartial Scheduling領域に順序付けられた状態
            //で入れる．
            pSchedTaskList.add(predTask);
            pSchedTaskList.add(sucTask);

        }

        //以降，マッピング済みクラスタ+当該クラスタのbottomタスクのうち，完了時刻が最も大きな値を算出する．
        //この値を，partial Makespanとする．
        //順序付けられたリストを基にして，各タスクの開始時刻を求める．
        //そして，各タスクの開始時刻を求める．
        long endStartTime = this.calcPstartTime(apl, endTask, new CustomIDSet());
        //メイクスパンを取得する．
        TaskCluster endCluster = apl.findTaskCluster(endTask.getClusterID());
        CPU endCPU = endCluster.getCPU();
        long makeSpan = endStartTime + (endTask.getMaxWeight()/ endCPU.getSpeed());


        //そして，指定クラスタ集合のBottomタスクのうち，最も値が大きいものをメイクスパンとする．
        Iterator<Long> cIte = mappedClusterSet.iterator();
        long retMakeSpan = 0;
        //クラスタごとのループ
        while(cIte.hasNext()){
            Long cID = cIte.next();
            TaskCluster cls = apl.findTaskCluster(cID);
            CPU CPU = cls.getCPU();
            Iterator<Long> btmIte = cls.getBottomSet().iterator();
            long btmMakeSpan = 0;
            while(btmIte.hasNext()){
                Long bID = btmIte.next();
                AbstractTask btmTask = apl.findTaskByLastID(bID);
                long tmpMakeSpan = btmTask.getStartTime() + btmTask.getMaxWeight()/ CPU.getSpeed();
                if(tmpMakeSpan >= btmMakeSpan){
                    btmMakeSpan = tmpMakeSpan;
                }
            }
            if(btmMakeSpan >= retMakeSpan){
                retMakeSpan = btmMakeSpan;
            }

        }
        return retMakeSpan;
    }

    /**
     * 
     * @param apl
     * @return
     */
    public long partialSchedule(BBTask apl
                                //CustomIDSet mappedClusterSet,
                                //Long clusterID,
                               // CPU machine,
                               /* CustomIDSet clusterSet*/){
        //まずは割り当て対象となるクラスタを取得する．
        //TaskCluster tCluster = apl.findTaskCluster(clusterID);

        //ターゲットクラスタにマシンを割り当ててみる．
        //tCluster.setCPU(machine);

        long s1 = System.currentTimeMillis();
        //マシン割り当て後のTlevelの更新を行う．
        long endID = apl.getTaskList().size();
        AbstractTask endTask = apl.findTaskByLastID(new Long(endID));
        this.updateTlevel(apl, endTask, new CustomIDSet());

        long e1 = System.currentTimeMillis();
        //System.out.println("TLEVEL UPDATE: "+ (e1-s1));
        
        //そして，各クラスタに対して，tlevelの大きい順に実行させるようにする．
        //そのためには，tlevelの大きい順のリストを作る必要がある．
        Iterator<TaskCluster> clusterIte = apl.clusterIterator();

        //各タスククラスタに対するループ処理
        while(clusterIte.hasNext()){
            TaskCluster cluster = clusterIte.next();
            //タスクリストを取得する．
            Iterator<Long> taskIDIte = cluster.getTaskSet().iterator();
            ArrayList<AbstractTask> array = new ArrayList<AbstractTask>();

            while(taskIDIte.hasNext()){
                Long taskID = taskIDIte.next();
                AbstractTask task = apl.findTaskByLastID(taskID);
                array.add(task);
            }

            Object[] oa = array.toArray();
            //一つのクラスタ内で，タスクのtlevel順にソートする．
            Arrays.sort(oa, new TaskTlevelComparator());


            //そして，実行順を決める．
            int len = oa.length;
            for(int i=0;i<len;i++){
                if(i == len-1){
                    break;
                }
                //先行タスク                                        .
                Long predID =( (AbstractTask)oa[i]).getIDVector().get(1);
                //後続タスク
                Long sucID =( (AbstractTask)oa[i+1]).getIDVector().get(1);

                //先行タスク
                AbstractTask predTask = this.retApl.findTaskByLastID(predID);
                //後続タスク
                AbstractTask sucTask = this.retApl.findTaskByLastID(sucID);
                LinkedList<AbstractTask> pSchedTaskList = cluster.getPSchedSet();

                //先行タスク，後続タスクのIDをPartial Scheduling領域に順序付けられた状態
                //で入れる．
               // TaskInfo predInfo = new TaskInfo(predTask.getIDVector().get(1), predTask.getMaxWeight());
               // TaskInfo sucInfo  = new TaskInfo(sucTask.getIDVector().get(1), sucTask.getMaxWeight());
                pSchedTaskList.add(predTask);
                pSchedTaskList.add(sucTask);

            }
        }

        //順序付けられたリストを基にして，各タスクの開始時刻を求める．
        //そして，各タスクの開始時刻を求める．
        long endStartTime = this.calcPstartTime(apl, endTask, new CustomIDSet());
        //メイクスパンを取得する．
        TaskCluster endCluster = apl.findTaskCluster(endTask.getClusterID());
        CPU endCPU = endCluster.getCPU();
        long makeSpan = endStartTime + endTask.getMaxWeight()/ endCPU.getSpeed();
        return makeSpan;
    }

   

    /**
     *
     * @param apl
     * @param task
     * @param set                                                        r
     * @return
     */
    public long calcPstartTime(BBTask apl, AbstractTask task, CustomIDSet set){

        //すでにタスクの開始時刻が設定済みであれば，そのままリターンする．
        if(set.contains(task.getIDVector().get(1))){
            return task.getStartTime();
        }
        //以降の処理は，まだ開始時刻が設定されていないタスクに対する処理
        Iterator<DataDependence> dpredIte = task.getDpredList().iterator();

        long predArrivalTime = 0;
        TaskCluster cls = this.retApl.findTaskCluster(task.getClusterID());
        //先行タスクたちから，開始時刻を取得する．
        while(dpredIte.hasNext()){
            DataDependence dpred = dpredIte.next();
            AbstractTask dpredTask = apl.findTaskByLastID(dpred.getFromID().get(1));
            //開始時刻の再起呼び出し
            long startTime = this.calcPstartTime(apl, dpredTask, set);
            TaskCluster predCluster = apl.findTaskCluster(dpredTask.getClusterID());
            CPU predCPU = predCluster.getCPU();
            long arrivalTime = 0;
            if(this.isHetero()){
                long nw_time = 0;
                if(cls.getCPU().getMachineID() == predCPU.getMachineID()){

                }else{
                    nw_time =this.env.getSetupTime()+dpred.getMaxDataSize()/this.env.getNWLink(predCPU.getCpuID(),cls.getCPU().getCpuID());
                }
                arrivalTime = startTime + dpredTask.getMaxWeight()/ predCPU.getSpeed() + nw_time;

            }else{
                arrivalTime = startTime + dpredTask.getMaxWeight()/ predCPU.getSpeed() +
                                    this.getNWTime(dpredTask.getIDVector().get(1), task.getIDVector().get(1), dpred.getMaxDataSize());
            }

            if(predArrivalTime <= arrivalTime){
                predArrivalTime  = arrivalTime;
            }
        }

        //次は，スケジュール上での開始時刻を求める．この場合，クラスタのスケジュールリストを見て，自分の先行タスク
        //の開始時刻を計算する．
        TaskCluster cluster = apl.findTaskCluster(task.getClusterID());
        CPU mac = cluster.getCPU();
        LinkedList<AbstractTask> pSchedList = cluster.getPSchedSet();
        int idx=0;
        long schedArrivalTime = 0;
        Iterator<AbstractTask> pSchedIte = pSchedList.iterator();
        while(pSchedIte.hasNext()){
            AbstractTask tmpTask = pSchedIte.next();
            //もしタスクが見つかれば，その一つ前のタスクについての開始時刻を取得する．
            if(tmpTask.getIDVector().get(1).longValue() == task.getIDVector().get(1).longValue()){
                if(idx == 0){
                }else{
                    //一つ手前のタスクを取得する．
                    AbstractTask schedPredTask = pSchedList.get(idx-1);
                    //その前タスクの，開始時刻を求める．
                    long schedPredStartTime = this.calcPstartTime(apl, schedPredTask, set);
                    //その前タスクからの完了時刻を求める．
                    long schedStartTime = schedPredStartTime + schedPredTask.getMaxWeight()/mac.getSpeed();

                    //スケジュール上の開始時刻を更新する．
                    if(schedArrivalTime <= schedStartTime){
                        schedArrivalTime = schedStartTime;
                    }
                }
                break;
            }
            idx++;
        }

        //集合に自身のIDを追加する．
        set.add(task.getIDVector().get(1));
        long retStartTime = Math.max(predArrivalTime, schedArrivalTime);
        task.setStartTime(retStartTime);
        //Data Ready Timeと，スケジュール上の開始時刻の大きいほうを，開始時刻とする
        return retStartTime;
    }
    
}
