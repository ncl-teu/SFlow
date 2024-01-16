package net.gripps.scheduling.algorithms.heterogeneous.peft;

import net.gripps.clustering.common.aplmodel.AbstractTask;
import net.gripps.clustering.common.aplmodel.BBTask;
import net.gripps.clustering.common.aplmodel.DataDependence;
import net.gripps.clustering.common.aplmodel.TaskCluster;
import net.gripps.clustering.tool.Calc;
import net.gripps.environment.CPU;
import net.gripps.environment.Environment;
import net.gripps.scheduling.algorithms.heterogeneous.heft.HEFT_Algorithm;

import java.util.*;

/**
 * Author: H. Kanemitsu
 * Date: 14/09/14
 */
public class PEFT_AlgorithmBak extends HEFT_Algorithm {

    /**
     * Optimistic Cost Table
     * (タスクID, CPU ID）のフォーマットである．
     */
   // private long[][] octTable;
    private OCTTable octTable;

    private long count = 0;

    private long totalTime = 0;

    private PriorityQueue<AbstractTask> readyList;

    private long time1 = 0;
    private long time2 = 0;

    private long time3 = 0;

    private long time4 = 0;

    private Hashtable<Long, CPU> actualCPUTable;


    public PEFT_AlgorithmBak(BBTask apl, String file, Environment env) {
        super(apl, file, env);
        this.actualCPUTable = this.env.getCpuList();


    }

    public PEFT_AlgorithmBak(BBTask apl, String file, Environment env, Hashtable<Long, CPU> in_cpuList) {
        super(apl, file, env, in_cpuList);
        this.actualCPUTable = this.env.getCpuList();

    }


    /**
     * 当該タスクにおける，OCT(Optimistic Cost Table)を作成する．
     * まず，STARTタスクのOCT計算に移り，それから再帰的に呼び出す．
     * ENDタスクであれば，OCT値は0となり返す．
     *
     * @param task
     * @param cpu
     * @return
     */
    public long calcOCT(AbstractTask task, CPU cpu) {
        //既にtmpに入っていれば，octTableにも入っているはずである．
        // if(tmpSet.contains(task.getIDVector().get(1))){

        if(this.octTable.isExist(task.getIDVector().get(1), cpu.getCpuID())){
       // if (this.octTable[Long.valueOf(task.getIDVector().get(1)).intValue() - 1][Long.valueOf(cpu.getCpuID()).intValue()] != -1) {
            //return this.octTable[Long.valueOf(task.getIDVector().get(1)).intValue() - 1][Long.valueOf(cpu.getCpuID()).intValue()];
             long val = this.octTable.get(task.getIDVector().get(1), cpu.getCpuID());
            return val;
        }

       // this.count++;
       // System.out.println("要した時間: " + (end - start));

        //ENDタスクのOCT値は0である．
        if (task.getDsucList().isEmpty()) {
            //tmpSet.add(task.getIDVector().get(1));
            //this.octTable[Long.valueOf(task.getIDVector().get(1)).intValue() - 1][Long.valueOf(cpu.getCpuID()).intValue()] = 0;
             this.octTable.add(task.getIDVector().get(1), cpu.getCpuID(), 0);
            return 0;
        }

        //以降は，octTableに入れるための処理
        //後続タスクのOCT値を取得する．
        Iterator<DataDependence> dsucIte = task.getDsucList().iterator();
        long maxOCT = 0;
        while (dsucIte.hasNext()) {
            DataDependence dsuc = dsucIte.next();
            //後続タスクを取得する．
            AbstractTask sucTask = this.retApl.findTaskByLastID(dsuc.getToID().get(1));
            //後続タスクのOCT値を取得する．
            //かつ，各CPUでの比較を行う．
            Iterator<CPU> cpuIte = this.cpuTable.values().iterator();
            long minOCT = 9999999;
            Long start = System.currentTimeMillis();


            while (cpuIte.hasNext()) {
                CPU oCPU = cpuIte.next();
                long nw_time = 0;
                if (cpu.getMachineID() == oCPU.getMachineID()) {
                    nw_time = 0;
                } else {
                    nw_time = this.getNWTime(
                            task.getIDVector().get(1),
                            sucTask.getIDVector().get(1),
                            dsuc.getMaxDataSize(),
                            this.env.getNWLink(cpu.getCpuID(), oCPU.getCpuID())
                    );
                }

                long tmpOCT = this.calcOCT(sucTask, oCPU) + sucTask.getMaxWeight() / oCPU.getSpeed() + nw_time;
                if (tmpOCT <= minOCT) {
                    minOCT = tmpOCT;
                }
            }
            Long end = System.currentTimeMillis();
            this.totalTime += end-start;

            if (maxOCT <= minOCT) {
                maxOCT = minOCT;
            }
        }

        //実際のOCTが決まったので，OCTテーブルに保存する．
        //this.octTable[Long.valueOf(task.getIDVector().get(1)).intValue() - 1][Long.valueOf(cpu.getCpuID()).intValue()] = maxOCT;
        this.octTable.add(task.getIDVector().get(1), cpu.getCpuID(), maxOCT);
        this.count++;

        return maxOCT;


    }

    public void mainProcess(){

          long O_EFT = 0;
          /**
           * スケジュールリストが空にならない間のループ
           */
          while(!this.readyList.isEmpty()){
              Iterator<CPU> cpuIte = this.cpuTable.values().iterator();

              //当該タスクの先行タスクがスケジュールされているかわからないので，
              //とりあえずpeekする（削除はしない）
              Object[] oa = this.readyList.toArray();
               //一つのクラスタ内で，タスクのtlevel順にソートする．
                Arrays.sort(oa, new OCTComparator());
               int len = oa.length;


              AbstractTask assignTask = null;

             // while(taskIte.hasNext()){
              for(int i=0;i<len;i++){
                  boolean isAllScheduled = true;

                 // AbstractTask maxBlevelTask = taskIte.next();
                  AbstractTask maxOCTTask = (AbstractTask)oa[i];
                  Iterator<DataDependence> dpredIte = maxOCTTask.getDpredList().iterator();
                  if(maxOCTTask.getDpredList().isEmpty()){
                      isAllScheduled = true;
                      assignTask = maxOCTTask;
                      break;
                  }

                  while(dpredIte.hasNext()){
                      DataDependence dpred = dpredIte.next();
                      AbstractTask dpredTask = this.retApl.findTaskByLastID(dpred.getFromID().get(1));
                      if(this.scheduledTaskSet.contains(dpredTask.getIDVector().get(1))){

                      }else{
                          isAllScheduled = false;
                          break;
                      }
                  }
                  if(!isAllScheduled){
                      continue;
                  }else{
                      assignTask = maxOCTTask;
                      break;
                  }
              }
              this.readyList.remove(assignTask);

              //各CPUについて，タスクtを実行した場合のEFTを計算する．
              //EFTが最も小さくなるようなCPUに割り当てる．
              O_EFT = 1000000000;
              long start_time = 0;
              CPU assignedCPU = null;
              while(cpuIte.hasNext()){
                  CPU cpu = cpuIte.next();
                  long tmpStartTime =  this.calcEST2(assignTask, cpu);
                  long tmpO_EFT = tmpStartTime  + assignTask.getMaxWeight()/cpu.getSpeed() + this.octTable.get(assignTask.getIDVector().get(1), cpu.getCpuID());
                  if(O_EFT >= tmpO_EFT){
                      O_EFT = tmpO_EFT;
                      assignedCPU = cpu;
                      start_time = tmpStartTime;
                  }
              }
              assignTask.setStartTime(start_time);
              //System.out.println("Task: "+assignTask.getIDVector().get(1) + "-> CPU:"+assignedCPU.getCpuID());
              //taskをCPUへ割り当てる処理．
              this.assignProcess(assignTask, assignedCPU);
          }


      }

      public TaskCluster assignProcess(AbstractTask task, CPU cpu){
          //もしCPUに，タスククラスタが未割り当てならば，当該タスクのクラスタID
          //をセットする．


          if(cpu.getTaskClusterID() < 0){
              cpu.setTaskClusterID(task.getClusterID());
              TaskCluster cls = this.retApl.findTaskCluster(task.getClusterID());
              cls.setCPU(cpu);

          }else{

              TaskCluster pivot = this.retApl.findTaskCluster(cpu.getTaskClusterID());
              pivot.addTask(task.getIDVector().get(1));
              TaskCluster target = this.retApl.findTaskCluster(task.getClusterID());

              this.updateOutSet(pivot, target);
              //InSetを更新する（後のレベル値の更新のため）
              this.updateInSet(pivot, target);
              this.retApl.removeTaskCluster(task.getClusterID());
              task.setClusterID(pivot.getClusterID());


              pivot.setCPU(cpu);

          }

              //CPUの実行終了時刻を更新する．
              //long cpu_finish_time = task.getStartTime() + task.getMaxWeight()/cpu.getSpeed();
              //cpu.setFinishTime(cpu_finish_time);
          cpu.getFtQueue().add(task);
          cpu.setFinishTime(cpu.getEndTime());

          //スケジュール済みセットにタスクを追加する．
          this.scheduledTaskSet.add(task.getIDVector().get(1));

          //readyListの更新処理
          Iterator<DataDependence> sucIte = task.getDsucList().iterator();
          while(sucIte.hasNext()){
              DataDependence dsuc = sucIte.next();
              AbstractTask t = this.retApl.findTaskByLastID(dsuc.getToID().get(1));
              //tの先行タスクを見る．
              Iterator<DataDependence> dpred = t.getDpredList().iterator();
              boolean isAllScheduled = true;
              while(dpred.hasNext()){
                  DataDependence sucDpred = dpred.next();
                  AbstractTask sucDpredTask = this.retApl.findTaskByLastID(sucDpred.getFromID().get(1));
                  if(this.scheduledTaskSet.contains(sucDpredTask.getIDVector().get(1))){
                      continue;
                  }else{
                      isAllScheduled = false;
                      break;
                  }
              }
              if(isAllScheduled){
                  this.readyList.add(t);
              }

          }
          return  this.retApl.findTaskCluster(cpu.getTaskClusterID());


      }



    public BBTask process() {
        try {
            //PEFT用の初期化処理
            long start = System.currentTimeMillis();

            this.initialize();
            long end = System.currentTimeMillis();
            retApl.setProcessTime((end - start));
            //System.out.println("時間:"+this.totalTime/1000);

            this.prepare();

            //this.wcp = this.calcWCP();

            //メイン処理
            this.mainProcess();


            //System.out.println("PEFT:"+retApl.getProcessTime());
            //System.out.println("カウント:"+this.count);

            //System.out.println("時間:"+this.totalTime/1000);

           //System.out.println("time1:"+this.time1/1000 + "/time2:"+this.time2/1000+"/time3:"+this.time3/1000);
            //後処理を行う．
            this.postProcess();
            AbstractTask endTask = this.retApl.findTaskByLastID(this.retApl.getEndTask().get(1));
            TaskCluster endCluster = this.retApl.findTaskCluster(endTask.getClusterID());
            CPU cpu = endCluster.getCPU();

            long makeSpan = endTask.getStartTime() + endTask.getMaxWeight() / cpu.getSpeed();
            this.retApl.setMakeSpan(makeSpan);


            return this.retApl;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 初期化処理を行います．
     * 各タスクについて，全CPUで実行した場合の平均時間をセットする．
     */
    public void initialize() {

        double cpuSum = 0.0;

        this.readyList = new PriorityQueue<AbstractTask>(5, new OCTComparator());

        Iterator<CPU> cpuIte = this.actualCPUTable.values().iterator();
        //Iterator<CPU> cpuIte = this.env.getCpuList().values().iterator();
        while (cpuIte.hasNext()) {
            CPU cpu = cpuIte.next();
            cpuSum += Calc.getRoundedValue((double) 1 / cpu.getSpeed());
        }


        long taskSum = 0;
        long cpuNum = this.actualCPUTable.size();
        //long cpuNum = this.env.getCpuList().size();
        int taskNum = this.retApl.getTaskList().size();
       // this.octTable = new long[taskNum][(int) cpuNum];
       this.octTable = new OCTTable(new Hashtable<Long, TaskCPUInfo>());
       /* for (int i = 0; i < taskNum; i++) {
            for (int j = 0; j < cpuNum; j++) {
                //this.octTable[i][j] = -1;
            }
        }*/

        //全体の平均帯域幅を算出する．
        long[][] linkMX = this.env.getLinkMatrix();
        long len = linkMX[0].length;
        int cnt = 0;
        long totalBW = 0;
        for (int i = 0; i < len; i++) {
            for (int j = i + 1; j < len; j++) {
                if (linkMX[i][j] == -1) {
                    continue;
                } else if (!this.actualCPUTable.containsKey(new Long(i)) || (!this.actualCPUTable.containsKey(new Long(j)))) {
                    // }else if(!this.cpuTable.contains(i) || (!this.cpuTable.contains(j))){
                    continue;
                } else {
                    totalBW += linkMX[i][j];
                    cnt++;
                }
            }
        }
        long aveLink = totalBW / cnt;


        Iterator<AbstractTask> taskIte = this.retApl.getTaskList().values().iterator();
        //各タスクについて，平均実行時間をセットする．
        while (taskIte.hasNext()) {
            AbstractTask task = taskIte.next();
            double ave_time_double = task.getMaxWeight() * cpuSum / cpuNum;
            long ave_time = (long) ave_time_double;
            task.setAve_procTime(ave_time);

            Iterator<DataDependence> dpredIte = task.getDpredList().iterator();
            while (dpredIte.hasNext()) {
                DataDependence dpred = dpredIte.next();
                dpred.setAve_comTime(dpred.getMaxDataSize() / aveLink);
            }

            Iterator<DataDependence> dsucIte = task.getDsucList().iterator();
            while (dsucIte.hasNext()) {
                DataDependence dsuc = dsucIte.next();
                dsuc.setAve_comTime(dsuc.getMaxDataSize() / aveLink);
            }
            if (task.getDpredList().isEmpty()) {
                this.retApl.getStartTaskSet().add(task.getIDVector().get(1));
            }
        }

        // int taskNum = this.retApl.getTaskList().size();
        //int cpuNum = this.env.getCpuList().size();

        //System.out.println("start数:"+this.retApl.getStartTaskSet().getList().size());
        Iterator<Long> startIte = this.retApl.getStartTaskSet().iterator();
        // CustomIDSet tmpSet = new CustomIDSet();
        long start = System.currentTimeMillis();
        while (startIte.hasNext()) {
            AbstractTask startTask = this.retApl.findTaskByLastID(startIte.next());

            Iterator<CPU> cpuIte2 = this.actualCPUTable.values().iterator();
            long octValue = 0;
            while (cpuIte2.hasNext()) {
                CPU scpu = cpuIte2.next();
                long tmpOCT = this.calcOCT(startTask, scpu);
                //this.octTable.add(startTask.getIDVector().get(1), scpu.getCpuID(), tmpOCT);
            }
           // System.out.println("ID:"+startTask.getIDVector().get(1) + "/"+this.octTable.getTable().get(startTask.getIDVector().get(1)).getCpuTable().size());

        }
        long end = System.currentTimeMillis();
        //各タスクについて，OCT値の平均値を計算する．
        Enumeration<Long> keys = this.octTable.getTable().keys();
        while(keys.hasMoreElements()){
            Long id = keys.nextElement();
            TaskCPUInfo info = this.octTable.getTable().get(id);
            Iterator<Long> vals = info.getCpuTable().values().iterator();
            long sum = 0;
            int valsize = info.getCpuTable().size();
            while(vals.hasNext()){
                sum += vals.next().longValue();
            }
            //平均値をセットする．
            AbstractTask t = this.retApl.findTaskByLastID(id);
            t.setAve_oct(sum / valsize);
            //STARTタスクなら，ready_listへ入れる．
            if(t.getDpredList().isEmpty()){
                this.readyList.add(t);
            }
        }
        //

    }


}

