package net.gripps.scheduling.algorithms.heterogeneous.hsv;

import net.gripps.clustering.common.Constants;
import net.gripps.clustering.common.aplmodel.*;
import net.gripps.environment.CPU;
import net.gripps.environment.Environment;
import net.gripps.scheduling.algorithms.heterogeneous.heft.HEFT_Algorithm;
import net.gripps.scheduling.algorithms.heterogeneous.msl.MSLComparator;
import net.gripps.scheduling.algorithms.heterogeneous.msl.MSL_Algorithm;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.PriorityQueue;

/**
 * Created by kanemih on 2015/11/09.
 */
public class HSV_Algorithm extends MSL_Algorithm {
    public HSV_Algorithm(BBTask apl, String file, Environment env) {
        super(apl, file, env);
    }

    public HSV_Algorithm(BBTask apl, String file, Environment env, Hashtable<Long, CPU> in_cpuList) {
        super(apl, file, env, in_cpuList);
    }

    public void HSV_prepare(){
        //まずは，各タスクについて，各プロセッサにおけるblevel値を計算し，その平均値をhrankとして格納する．
        Iterator<CPU> cpuIte = this.env.getCpuList().values().iterator();

        while(cpuIte.hasNext()){
            CPU cpu = cpuIte.next();
            Iterator<Long> startIte = this.retApl.getStartTaskSet().getList().iterator();
            while(startIte.hasNext()){
                Long startID = startIte.next();
                AbstractTask startTask = this.retApl.findTaskByLastID(startID);
                //当該プロッサによる，blevel計算を開始する．
                this.getMaxBlevel(startTask, cpu, new CustomIDSet());


            }
        }

        //各タスクについて，hrankを計算する．
        Iterator<AbstractTask> taskIte = this.retApl.getTaskList().values().iterator();
        while(taskIte.hasNext()){
            AbstractTask t = taskIte.next();

            long hrank = t.getTotalHSV_blevel() / this.env.getCpuList().size();
            //HRPVランクの設定
            t.setHprv_rank(hrank*t.getDsucList().size());

        }

        //再度，スタートタスクをレディリストへ入れる処理
       Iterator<Long> startIte2 =  this.retApl.getStartTaskSet().iterator();
        while(startIte2.hasNext()){
            Long id = startIte2.next();
            AbstractTask stask = this.retApl.findTaskByLastID(id);
            this.readyList.add(stask);
        }

    }

    /**
     * 指定されたCPUによるblevel値の計算ルーチン
     * @param task
     * @param cpu
     * @param set
     * @return
     */
    public long getMaxBlevel(AbstractTask task, CPU cpu, CustomIDSet set) {
        set.add(task.getIDVector().get(1));
        AbstractTask dominatingTask = null;


        //ENDタスクであれば，blevelをそのまま帰す．
        if (task.getDsucList().isEmpty()) {
            long endBlevel = task.getMaxWeight()/cpu.getSpeed();
            //task.setBlevel(endBlevel);
            if(task.getHsv_blevel().get(cpu.getCpuID()) == null){
                task.getHsv_blevel().put(cpu.getCpuID(), new Long(endBlevel));
                long preBlevel = task.getTotalHSV_blevel();
                task.setTotalHSV_blevel(preBlevel + endBlevel);

            }
            return endBlevel;
        }

        //後続タスクのblevel値を取得する．
        Iterator<DataDependence> dsucIte = task.getDsucList().iterator();
        long maxBlevel = 0;
        long realBlevel = 0;

        //後続タスクに対するループ
        while (dsucIte.hasNext()) {
            DataDependence dsuc = dsucIte.next();
            AbstractTask sucTask = this.retApl.findTaskByLastID(dsuc.getToID().get(1));
            long sucBlevel = 0;

            if (set.contains(sucTask.getIDVector().get(1))) {
                sucBlevel = sucTask.getHsv_blevel().get(cpu.getCpuID());
            } else {
                //もし未チェックであれば，再計算する．
                sucBlevel = this.getMaxBlevel(sucTask, cpu, set);
            }
            //後続タスクから，自身のBlevel値を計算する．
            realBlevel = task.getMaxWeight()/cpu.getSpeed() + dsuc.getMaxDataSize()/this.env.getBWFromCPU(cpu) + sucBlevel;


            if (maxBlevel <= realBlevel) {
                maxBlevel = realBlevel;
                task.getHsv_blevel().put(cpu.getCpuID(), new Long(realBlevel));
              //  task.setBlevel(realBlevel);
               // task.setBsuc(sucTask.getIDVector());
              //  dominatingTask = sucTask;
            }
        }

        //最大のシーケンスにその先行タスクを入れる．
      //  this.maxSequence.add(dominatingTask.getIDVector().get(1));
    //    this.blevelQueue.offer(task);
        set.add(task.getIDVector().get(1));
        long preBlevel = task.getTotalHSV_blevel();
        task.setTotalHSV_blevel(preBlevel + maxBlevel);
        return maxBlevel;

    }

    /**
     * HSVアルゴリズムのメイン処理です．
     */
    public void mainProcess(){

        long EFT = 0;
        while(!this.readyList.isEmpty()){
            Iterator<CPU> cpuIte = this.cpuTable.values().iterator();

            //当該タスクの先行タスクがスケジュールされているかわからないので，
            //とりあえずpeekする（削除はしない）
            // AbstractTask maxBlevelTask = this.blevelQueue.poll();
            Object[] oa = this.readyList.toArray();
            //一つのクラスタ内で，タスクのtlevel順にソートする．
            Arrays.sort(oa, new HPRVComparator());
            int len = oa.length;

            AbstractTask assignTask = null;
            //while(taskIte.hasNext()){
            for (int i = 0; i < len; i++) {
                boolean isAllScheduled = true;
                // AbstractTask maxBlevelTask = taskIte.next();
                AbstractTask maxRankTask = (AbstractTask) oa[i];
                Iterator<DataDependence> dpredIte = maxRankTask.getDpredList().iterator();
                if (maxRankTask.getDpredList().isEmpty()) {
                    isAllScheduled = true;
                    assignTask = maxRankTask;
                    break;
                }

                while (dpredIte.hasNext()) {
                    DataDependence dpred = dpredIte.next();
                    AbstractTask dpredTask = this.retApl.findTaskByLastID(dpred.getFromID().get(1));
                    if (this.scheduledTaskSet.contains(dpredTask.getIDVector().get(1))) {

                    } else {
                        isAllScheduled = false;
                        break;
                    }
                }
                if (!isAllScheduled) {
                    continue;
                } else {
                    assignTask = maxRankTask;
                    break;
                }
            }
            this.readyList.remove(assignTask);

            //各CPUについて，タスクtを実行した場合のEFTを計算する．
            //EFTが最も小さくなるようなCPUに割り当てる．
            EFT = 1000000000;
            long HSV = Constants.MAXValue;
            long start_time = 0;
            CPU assignedCPU = null;
            while (cpuIte.hasNext()) {
                CPU cpu = cpuIte.next();
                long tmpStartTime = this.calcEST2(assignTask, cpu);
                long tmpEFT = tmpStartTime + assignTask.getMaxWeight() / cpu.getSpeed();
                //ここから，HSV（Heterogeneous SelectionValue)の計算を行う．
                //まずはLDET(Longest Distance Exit Time)の計算
                long ldet = assignTask.getHsv_blevel().get(cpu.getCpuID()) - assignTask.getMaxWeight()/cpu.getSpeed();
                long tmpHSV = tmpEFT * ldet;
                if (HSV >= tmpHSV) {
                    HSV = tmpHSV;
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

    public BBTask process() {
            this.readyList = new PriorityQueue<AbstractTask>(5, new MSLComparator());


            try {
                //初期化処理
                this.initialize();
                this.prepare();
                //HSV用の初期化処理
                this.HSV_prepare();

                this.wcp = this.calcWCP();
                this.retApl.setMinCriticalPath(this.wcp / this.maxSpeed);

                long start = System.currentTimeMillis();
                //メイン処理
                this.mainProcess();

                long end = System.currentTimeMillis();
                retApl.setProcessTime((end - start));

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




}
