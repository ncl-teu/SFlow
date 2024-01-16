package net.gripps.scheduling.algorithms.heterogeneous.heft;

import net.gripps.clustering.algorithms.mwsl_delta.CMWSL_Algorithm;
import net.gripps.clustering.common.Constants;
import net.gripps.clustering.common.aplmodel.*;
import net.gripps.clustering.tool.Calc;
import net.gripps.environment.CPU;
import net.gripps.environment.Environment;

import java.util.*;

/**
 * Author: H. Kanemitsu
 * Date: 2008/10/26
 */
public class HEFT_Algorithm extends CMWSL_Algorithm {


    /**
     * 実際に用いられるCPUコアのリスト
     */
    protected Hashtable<Long, CPU> cpuTable;

    /**
     * blevelの降順で保持しているタスクリスト
     */
    protected PriorityQueue<AbstractTask> blevelQueue;

    protected CustomIDSet scheduledTaskSet;


    /**
     *
     * @param file
     * @param apl
     * @param env
     */
    public HEFT_Algorithm(BBTask apl, String file, Environment env){
        super(apl, file,  env);
        this.cpuTable = env.getCpuList();
        this.blevelQueue = new PriorityQueue<AbstractTask>(5, new BlevelComparator());
        this.scheduledTaskSet = new CustomIDSet();
    }



    /**
     * 指定のCPU集合で，アルゴリズムを実装する場合．
     * 比較実験の場合は，こちらを使う．
     *
     * @param apl
     * @param file
     * @param env
     * @param in_cpuList
     */
    public HEFT_Algorithm(BBTask apl, String file, Environment env, Hashtable<Long, CPU> in_cpuList){
        this(apl, file, env);
        this.cpuTable = in_cpuList;

    }

    public PriorityQueue<AbstractTask> getBlevelQueue() {
        return blevelQueue;
    }

    public void setBlevelQueue(PriorityQueue<AbstractTask> blevelQueue) {
        this.blevelQueue = blevelQueue;
    }

    public BBTask process() {
        try {
            //HEFT用の初期化処理
            this.initialize();
            this.prepare();
            this.wcp = this.calcWCP();
            this.retApl.setMinCriticalPath(this.wcp / this.maxSpeed);


            //this.wcp = this.calcWCP();

            long start = System.currentTimeMillis();
            //メイン処理
            this.mainProcess();
            long end = System.currentTimeMillis();
            retApl.setProcessTime((end - start));

            //System.out.println("HEFTの処理時間:"+retApl.getProcessTime());

            //後処理を行う．
            this.postProcess();
            AbstractTask endTask = this.retApl.findTaskByLastID(this.retApl.getEndTask().get(1));
            TaskCluster endCluster = this.retApl.findTaskCluster(endTask.getClusterID());
            CPU cpu = endCluster.getCPU();

            long makeSpan = endTask.getStartTime() + endTask.getMaxWeight()/cpu.getSpeed();
            this.retApl.setMakeSpan(makeSpan);
            return this.retApl;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public long calcEST2(AbstractTask task, CPU cpu) {
        long arrival_time = 0;

        if (task.getDpredList().isEmpty()) {

        } else {
            Iterator<DataDependence> dpredIte = task.getDpredList().iterator();
            while (dpredIte.hasNext()) {
                DataDependence dpred = dpredIte.next();
                AbstractTask dpredTask = this.retApl.findTaskByLastID(dpred.getFromID().get(1));
                TaskCluster predCluster = this.retApl.findTaskCluster(dpredTask.getClusterID());

                CPU predCPU = predCluster.getCPU();
                long nw_time = 0;
                if (predCPU.getMachineID() == cpu.getMachineID()) {

                } else {
                    //先行タスクからのデータ到着時刻を取得する．
                    nw_time = this.env.getSetupTime()+dpred.getMaxDataSize() / this.env.getNWLink(predCPU.getCpuID(), cpu.getCpuID());
                }
                long tmp_arrival_time = dpredTask.getStartTime() + dpredTask.getMaxWeight() / predCPU.getSpeed() + nw_time;
                if (arrival_time <= tmp_arrival_time) {
                    arrival_time = tmp_arrival_time;
                }
            }
        }
        //arrival_time(DRT) ~ 最後のFinishTimeまでの範囲で，task/cpu速度の時間が埋められる
        //箇所があるかどうかを調べる．
        Object[] oa = cpu.getFtQueue().toArray();
        if(oa.length > 1){
           //Tlevelの小さい順→blevelの大きい順にソートする．
                   Arrays.sort(oa, new StartTimeComparator());
                   int len = oa.length;
                   for (int i = 0; i < len - 1; i++) {
                       AbstractTask t = ((AbstractTask) oa[i]);
                       long finish_time = t.getStartTime() + t.getMaxWeight() / cpu.getSpeed();
                       //次の要素の開始時刻を取得する．
                       AbstractTask t2 = ((AbstractTask) oa[i + 1]);
                       long start_time2 = t2.getStartTime();
                       long s_candidateTime = Math.max(finish_time, arrival_time);
                       if (s_candidateTime + (task.getMaxWeight() / cpu.getSpeed()) <= start_time2) {
                           //System.out.println("挿入したよ");
                           return s_candidateTime;
                       } else {
                           continue;
                       }

                   }
                   //挿入できない場合は，ENDテクニックを行う．
                   AbstractTask finTask = ((AbstractTask) oa[len - 1]);
                   return Math.max(finTask.getStartTime() + finTask.getMaxWeight() / cpu.getSpeed(), arrival_time);
        }else{
            if(oa.length == 1){
                AbstractTask t = ((AbstractTask)oa[0]);
                long finish_time = t.getStartTime() + t.getMaxWeight() / cpu.getSpeed();
                long f2 = arrival_time + task.getMaxWeight() / cpu.getSpeed();
                if(f2 <= t.getStartTime()){
                    return arrival_time;
                }else{
                    return finish_time;
                }
            }else{
                return arrival_time;

            }
        }

    }

    /**
     * メイン処理です．
     */
    public void mainProcess(){

        long EFT = 0;
        /**
         * スケジュールリストが空にならない間のループ
         */
        while(!this.blevelQueue.isEmpty()){
            Iterator<CPU> cpuIte = this.cpuTable.values().iterator();

            //当該タスクの先行タスクがスケジュールされているかわからないので，
            //とりあえずpeekする（削除はしない）
           // AbstractTask maxBlevelTask = this.blevelQueue.poll();
            Object[] oa = this.blevelQueue.toArray();
            //一つのクラスタ内で，タスクのtlevel順にソートする．
            Arrays.sort(oa, new BlevelComparator());
            int len = oa.length;
            //Iterator<AbstractTask> taskIte = this.blevelQueue.iterator();

            AbstractTask assignTask = null;
            //while(taskIte.hasNext()){
            for(int i=0;i<len;i++){
                boolean isAllScheduled = true;
               // AbstractTask maxBlevelTask = taskIte.next();
                AbstractTask maxBlevelTask = (AbstractTask)oa[i];
                Iterator<DataDependence> dpredIte = maxBlevelTask.getDpredList().iterator();
                if(maxBlevelTask.getDpredList().isEmpty()){
                    isAllScheduled = true;
                    assignTask = maxBlevelTask;
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
                    assignTask = maxBlevelTask;
                    break;
                }
            }
            this.blevelQueue.remove(assignTask);

            //各CPUについて，タスクtを実行した場合のEFTを計算する．
            //EFTが最も小さくなるようなCPUに割り当てる．
            EFT = Constants.MAXValue;
            long start_time = 0;
            CPU assignedCPU = null;
            while(cpuIte.hasNext()){
                CPU cpu = cpuIte.next();
                long tmpStartTime =  this.calcEST2(assignTask, cpu);
                long tmpEFT = tmpStartTime  + assignTask.getMaxWeight()/cpu.getSpeed();
                if(EFT >= tmpEFT){
                    EFT = tmpEFT;
                    assignedCPU = cpu;
                    start_time = tmpStartTime;
                }
            }
            //実際にはImageのダウンロード時間を考慮に入れる．
           
            assignTask.setStartTime(start_time);
            //System.out.println("Task: "+assignTask.getIDVector().get(1) + "-> CPU:"+assignedCPU.getCpuID());
            //taskをCPUへ割り当てる処理．
            this.assignProcess(assignTask, assignedCPU);
        }


    }

    public TaskCluster  assignProcess(AbstractTask task, CPU cpu){
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
            this.updateTopSet(pivot, target);
            this.retApl.removeTaskCluster(target.getClusterID());
            task.setClusterID(pivot.getClusterID());


            pivot.setCPU(cpu);

            CPU tcpu = target.getCPU();
           if(tcpu !=null){
               tcpu.clear();
               this.unAssignedCPUs.add(tcpu.getCpuID());
           }

        }

            //CPUの実行終了時刻を更新する．
            long cpu_finish_time = task.getStartTime() + task.getMaxWeight()/cpu.getSpeed();
           // cpu.setFinishTime(cpu_finish_time);
            cpu.getFtQueue().add(task);
            cpu.setFinishTime(cpu.getEndTime());




        //スケジュール済みセットにタスクを追加する．
        this.scheduledTaskSet.add(task.getIDVector().get(1));

        return this.retApl.findTaskCluster(cpu.getTaskClusterID());


    }

    /**
     * 初期化処理を行います．
     * 各タスクについて，全CPUで実行した場合の平均時間をセットする．
     */
    public void initialize(){

        double cpuSum = 0.0;

        Iterator<CPU> cpuIte = this.cpuTable.values().iterator();
        while(cpuIte.hasNext()){
            CPU cpu = cpuIte.next();
            //cpuSum += Calc.getRoundedValue((double)1/cpu.getSpeed());
            cpuSum += Calc.getRoundedValue(cpu.getSpeed());
        }
        long taskSum = 0;
        long cpuNum = this.cpuTable.size();
        double ave_speed = Calc.getRoundedValue(cpuSum/cpuNum);

       // System.out.println("heikin:"+ave_speed);

        //全体の平均帯域幅を算出する．
        long[][] linkMX = this.env.getLinkMatrix();
        long len = linkMX[0].length;
        int cnt  = 0;
        long totalBW = 0;
        for(int i=0;i<len;i++){
            for(int j=i+1;j<len;j++){
                if(linkMX[i][j] == -1) {
                    continue;
                }else if(!this.cpuTable.containsKey(new Long(i))|| (!this.cpuTable.containsKey(new Long(j)))){
               // }else if(!this.cpuTable.contains(i) || (!this.cpuTable.contains(j))){
                    continue;
                }else{
                     totalBW+=linkMX[i][j];
                    cnt++;
                }
            }
        }
        long aveLink = totalBW/cnt;


        Iterator<AbstractTask> taskIte = this.retApl.getTaskList().values().iterator();
        //各タスクについて，平均実行時間をセットする．
        while(taskIte.hasNext()){
            AbstractTask task = taskIte.next();
            //double ave_time_double = Calc.getRoundedValue(task.getMaxWeight()/(double)cpuSum/cpuNum);
            double ave_time_double = Calc.getRoundedValue(task.getMaxWeight()/ave_speed);
            long ave_time = (long)ave_time_double;
            task.setAve_procTime(ave_time);
            //System.out.println("平均:"+ave_time);

            Iterator<DataDependence> dpredIte = task.getDpredList().iterator();
            while(dpredIte.hasNext()){
                DataDependence dpred = dpredIte.next();
                dpred.setAve_comTime(dpred.getMaxDataSize()/aveLink);
            }

            Iterator<DataDependence> dsucIte = task.getDsucList().iterator();
            while(dsucIte.hasNext()){
                DataDependence dsuc = dsucIte.next();
                dsuc.setAve_comTime(dsuc.getMaxDataSize()/aveLink);
            }


        }
    }

    public void prepare() {
        //アプリから，タスクリストを取得
        Hashtable<Long, AbstractTask> tasklist = this.retApl.getTaskList();
        Collection<AbstractTask> col = tasklist.values();
        Iterator<AbstractTask> ite = col.iterator();

        long start = System.currentTimeMillis();
        CustomIDSet startSet = new CustomIDSet();


        //CPUを全て未割り当て状態とする．
        //Iterator<CPU> umIte = this.env.getCpuList().values().iterator();
        Iterator<CPU> umIte = this.cpuTable.values().iterator();

        //マシンに対するループ
        //各マシンを未割り当てCPUリストへ追加させる．
        while (umIte.hasNext()) {
            CPU cpu = umIte.next();
            this.unAssignedCPUs.add(cpu.getCpuID());
        }

        //仮想CPUと仮想リンクを作り，各タスクはそれぞれ仮想CPUへ割り当てられているものとする．
        // int vcpu_num = this.env.getCpuList().size();
        int vcpu_num = this.retApl.getTaskList().size();
        this.virtualEnv = new Environment();

        this.retApl.getTaskList().values().iterator();

        /*   for(int i=0;i<vcpu_num; i++){
               CPU vCPU = new CPU(new Long(i+1), this.maxSpeed, new Vector<Long>(), new Vector<Long>());
              vCPU.setVirtual(true);
           }
        */
        Hashtable vcpuList = new Hashtable<Long, CPU>();
        //CPUリストをセットする．
        this.virtualEnv.setCpuList(vcpuList);
        long[][] vlinks = new long[vcpu_num][vcpu_num];

        //仮想リンク情報を作成する．
        for (int i = 0; i < vcpu_num; i++) {
            for (int j = 0; j < vcpu_num; j++) {
                vlinks[i][j] = this.maxBW;
            }
        }
        //仮想リンク集合をセットする．
        this.virtualEnv.setLinkMatrix(vlinks);

        double taskMaxG = 0.0;

        //タスククラスタの生成
        //各タスクに対するループ
        while (ite.hasNext()) {
            AbstractTask task = ite.next();
            //CPU作成
            //CPU vCPU = new CPU(task.getIDVector().get(1), this.maxSpeed, new Vector<Long>(), new Vector<Long>());
           // CPU vCPU = new CPU(new Long(-1), this.maxSpeed, new Vector<Long>(), new Vector<Long>());
            //vCPU.setVirtual(true);
            //まずは自分自身をDestへ追加する．
            task.addDestTask(task.getIDVector().get(1));

            //タスクをクラスタへ入れる．
            TaskCluster cluster = new TaskCluster(task.getIDVector().get(1));
            //一つのタスクしか入らないので，当然Linearである．
            cluster.setLinear(true);
            //クラスタに，CPUをセットする．
            //CPU tmpCPU = this.virtualEnv.getCPU(task.getIDVector().get(1));

            task.setClusterID(cluster.getClusterID());
            cluster.addTask(task.getIDVector().get(1));

            // タスククラスタに対して，各種情報をセットする．
            /**このときは，各クラスタには一つのみのタスクが入るため，
             * 以下のような処理が明示的に可能である．
             */
            //ここで，top/outタスクは，自分自身のみをセットしておく．
            cluster.setBsucTaskID(task.getIDVector().get(1));
            cluster.getBottomSet().add(task.getIDVector().get(1));
            cluster.addIn_Set(task.getIDVector().get(1));
            cluster.setTopTaskID(task.getIDVector().get(1));
            CustomIDSet topSet = new CustomIDSet();
            topSet.add(task.getIDVector().get(1));
            cluster.setTop_Set(topSet);

            //もし後続タスクがあれば，自身のタスクをOutセットへ入れる
            if (!task.getDsucList().isEmpty()) {
                cluster.addOut_Set(task.getIDVector().get(1));
            }


            //先行タスクがなけｒば，スタートセットに入れる．
            if (task.getDpredList().isEmpty()) {
                startSet.add(task.getIDVector().get(1));
                task.setStartTime(0);
            }
            //タスク自身に，所属するクラスタIDをセットする．
            //このとき，クラスタIDをタスクIDとしてセットしておく．
            task.setClusterID(task.getIDVector().get(1));
            //クラスタ自体をDAGへ反映
            Long clusterID = this.retApl.addTaskCluster(cluster);

            //この時点で，UEXを格納しておく．
            this.uexClusterList.add(clusterID);
        }


        long endID = this.retApl.getTaskList().size();
        AbstractTask endTask = this.retApl.findTaskByLastID(new Long(endID));
        //Tlevelのセットをする．
        this.getMaxTlevel(endTask, new CustomIDSet());
        //Blevelのセットをする．
        Iterator<Long> startIDIte = startSet.iterator();
        CustomIDSet idSet = new CustomIDSet();

        while (startIDIte.hasNext()) {
            Long sID = startIDIte.next();
            AbstractTask startTask = this.retApl.findTaskByLastID(sID);
            this.getMaxBlevel(startTask, idSet);
        }

        //   this.calculateInitialTlevel(endTask, initialCPU, false);
        this.retApl.setStartTaskSet(startSet);

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
        this.retApl.setMinCriticalPath(cLevel);
        long end1 = System.currentTimeMillis();
        //System.out.println("レベル反映時間: "+(end1-start));

    }


    /**
        * Seq_{max}を取得します．
        * 指定クラスタ内のTop_Setの中で最大のTlevelを持つタスク(topTask)を取得する
        * Top_Setの内，最大のTlevel値のタスクを特定
        * そのタスクの先行タスクが属するクラスタを特定
        * それを再帰的に繰り返す．
        * <p/>
        * と思ったけど，その時のクリティカル・パスを計算することにした．
        * tlevelとblevelが関係する．
        *
        * @param task
        * @param set
        * @return
        */
       public long getMaxTlevel(AbstractTask task, CustomIDSet set) {
           set.add(task.getIDVector().get(1));
           AbstractTask dominatingTask = null;

           //Startタスクであれば，tlevel=0を返す．
           if (task.getDpredList().isEmpty()) {
               task.setTlevel(0);
               return 0;
           }
           //先行タスクのtlevel値を取得する．
           Iterator<DataDependence> dpredIte = task.getDpredList().iterator();
           long maxTlevel = 0;
           long realTlevel = 0;

           //先行タスクに対するループ
           while (dpredIte.hasNext()) {
               DataDependence dpred = dpredIte.next();
               AbstractTask predTask = this.retApl.findTaskByLastID(dpred.getFromID().get(1));
               long predTlevel = 0;

               if (set.contains(predTask.getIDVector().get(1))) {
                   predTlevel = predTask.getTlevel();
               } else {
                   //もし未チェックであれば，再計算する．
                   predTlevel = this.getMaxTlevel(predTask, set);
               }
               //先行タスクから，自身のTlevel値を計算する．
               realTlevel = predTlevel + predTask.getAve_procTime() + this.getComTime(predTask, task);

               if (maxTlevel <= realTlevel) {
                   maxTlevel = realTlevel;
                   task.setTlevel(realTlevel);
                   task.setTpred(predTask.getIDVector());
                   dominatingTask = predTask;
               }

           }

           //最大のシーケンスにその先行タスクを入れる．
           this.maxSequence.add(dominatingTask.getIDVector().get(1));
           return maxTlevel;

       }

    public long getComTime(AbstractTask fromTask, AbstractTask toTask){
        DataDependence dd = fromTask.findDDFromDsucList(fromTask.getIDVector(), toTask.getIDVector());
        return this.env.getSetupTime()+dd.getAve_comTime();
    }


    /**
     * Startタスクから順に走査することによって，各タスクの
     * Blevel値を設定する．
     *
     * @param task
     * @param set
     * @return
     */
    public long getMaxBlevel(AbstractTask task, CustomIDSet set) {
        set.add(task.getIDVector().get(1));
        AbstractTask dominatingTask = null;


        //ENDタスクであれば，blevelをそのまま帰す．
        if (task.getDsucList().isEmpty()) {
            long endBlevel = task.getAve_procTime();
            task.setBlevel(endBlevel);
            this.blevelQueue.offer(task);

            return endBlevel;
        }

        //先行タスクのtlevel値を取得する．
        Iterator<DataDependence> dsucIte = task.getDsucList().iterator();
        long maxBlevel = 0;
        long realBlevel = 0;

        //先行タスクに対するループ
        while (dsucIte.hasNext()) {
            DataDependence dsuc = dsucIte.next();
            AbstractTask sucTask = this.retApl.findTaskByLastID(dsuc.getToID().get(1));
            long sucBlevel = 0;

            if (set.contains(sucTask.getIDVector().get(1))) {
                sucBlevel = sucTask.getBlevel();
            } else {
                //もし未チェックであれば，再計算する．
                sucBlevel = this.getMaxBlevel(sucTask, set);
            }
            //後続タスクから，自身のBlevel値を計算する．
            realBlevel = task.getAve_procTime() + this.getComTime(task, sucTask) + sucBlevel;

            if (maxBlevel <= realBlevel) {
                maxBlevel = realBlevel;
                task.setBlevel(realBlevel);
                task.setBsuc(sucTask.getIDVector());
                dominatingTask = sucTask;
            }
        }

        //最大のシーケンスにその先行タスクを入れる．
        this.maxSequence.add(dominatingTask.getIDVector().get(1));
        this.blevelQueue.offer(task);
        return maxBlevel;

    }




}
