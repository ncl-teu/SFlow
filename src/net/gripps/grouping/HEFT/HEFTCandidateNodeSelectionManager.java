package net.gripps.grouping.HEFT;

import net.gripps.clustering.common.Constants;
import net.gripps.clustering.common.aplmodel.*;
import net.gripps.clustering.tool.Calc;
import net.gripps.environment.CPU;
import net.gripps.environment.Environment;
import net.gripps.grouping.CandidateNodeSelectionManager;
import net.gripps.grouping.DistanceInfo;
import net.gripps.grouping.IndexComparator;
import net.gripps.grouping.IndexInfo;
import net.gripps.scheduling.algorithms.heterogeneous.heft.BlevelComparator;
import net.gripps.scheduling.algorithms.heterogeneous.heft.HEFT_Algorithm;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.PriorityQueue;

/**
 * Created by kanemih on 2016/01/19.
 */
public class HEFTCandidateNodeSelectionManager extends CandidateNodeSelectionManager {
    /**
     * HEFTアルゴリズム
     */
    protected HEFT_Algorithm heft;

    /**
     * CPUテーブル
     */
    protected Hashtable<Long, CPU> cpuTable;

    protected AbstractTask task_k;

    protected AbstractTask task_s;

    protected long diffMinBlevel;

    protected double diffBlevel;


    protected CustomIDSet scheduledTaskSet;

    protected double averageSpeed;

    protected double averageBW;

    protected double beta_alpha;

    protected long totalDifBlevelTaskSize;

    protected long totalDifBlevelDataSize;

    protected long freeCount;

    protected double currentRightValue;

    protected boolean isLargerThanMid;

    protected CustomIDSet CPSet;

    protected double maxWorkload;

    protected CPU firstProcessor;

    protected long firstCPUNum;

    protected boolean isDeltaRankWPositive;



    public HEFTCandidateNodeSelectionManager(BBTask apl, String file, Environment env) {
        super(apl, file, env);
        this.heft = new HEFT_Algorithm(apl, file, env);

        //this.heft.initialize();
        //this.apl = this.heft.getApl();
        this.cpuTable = this.env.getCpuList();
        this.diffMinBlevel = Constants.MAXValue;
        this.scheduledTaskSet = new CustomIDSet();
        this.currentTotalSpeed = 0;
        this.currentTotalBW = 0;
        this.currentTotalLinkNum = 0;
        this.diffBlevel = 0;
        this.totalDifBlevelDataSize = 0;
        this.totalDifBlevelTaskSize = 0;
        this.freeCount = 0;
        this.currentRightValue = 0;
        this.isLargerThanMid = false;
        this.CPSet = new CustomIDSet();
        this.maxWorkload = 0;
        this.firstProcessor = null;
        this.firstCPUNum = 0;
        this.isDeltaRankWPositive = false;

        this.initialize();
        //this.initializeHEFT();
        this.prepare();

    }


    /**
     * 初期化処理を行います．
     * 各タスクについて，全CPUで実行した場合の平均時間をセットする．
     */
    public void initialize() {


        double cpuSum = 0.0;

        Iterator<CPU> cpuIte = this.cpuTable.values().iterator();
        while (cpuIte.hasNext()) {
            CPU cpu = cpuIte.next();
            //cpuSum += Calc.getRoundedValue((double)1/cpu.getSpeed());
            cpuSum += Calc.getRoundedValue(cpu.getSpeed());
        }
        long taskSum = 0;
        long cpuNum = this.cpuTable.size();
        double ave_speed = Calc.getRoundedValue(cpuSum / cpuNum);
        this.averageSpeed = ave_speed;

        // System.out.println("heikin:"+ave_speed);

        //全体の平均帯域幅を算出する．
        long[][] linkMX = this.env.getLinkMatrix();
        long len = linkMX[0].length;
        int cnt = 0;
        long totalBW = 0;
        for (int i = 0; i < len; i++) {
            for (int j = i + 1; j < len; j++) {
                if (linkMX[i][j] == -1) {
                    continue;
                } else if (!this.cpuTable.containsKey(new Long(i)) || (!this.cpuTable.containsKey(new Long(j)))) {
                    // }else if(!this.cpuTable.contains(i) || (!this.cpuTable.contains(j))){
                    continue;
                } else {
                    totalBW += linkMX[i][j];
                    cnt++;
                }
            }
        }
        long aveLink = totalBW / cnt;
        this.averageBW = Calc.getRoundedValue((double) totalBW / (double) cnt);

        this.beta_alpha = Calc.getRoundedValue(this.averageBW / (double) this.averageSpeed);


        Iterator<AbstractTask> taskIte = this.apl.getTaskList().values().iterator();
        //各タスクについて，平均実行時間をセットする．
        while (taskIte.hasNext()) {
            AbstractTask task = taskIte.next();
            //double ave_time_double = Calc.getRoundedValue(task.getMaxWeight()/(double)cpuSum/cpuNum);
            double ave_time_double = Calc.getRoundedValue(task.getMaxWeight() / ave_speed);
            long ave_time = (long) ave_time_double;
            task.setAve_procTime(ave_time);
            //System.out.println("平均:"+ave_time);

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


        }
    }

    public double calcDuratoinTime(AbstractTask task) {
        Iterator<DataDependence> dpredIte = task.getDpredList().iterator();
        double totalCom = 0;

        while (dpredIte.hasNext()) {
            DataDependence dpred = dpredIte.next();
            double comin = Calc.getRoundedValue((double) dpred.getMaxDataSize() / (double) this.averageBW);
            totalCom += comin;

        }
        double ave_comtime = 0;
        if (totalCom == 0) {
            ave_comtime = 0;
        } else {
            ave_comtime = Calc.getRoundedValue((double) totalCom / (double) task.getDpredList().size());

        }
        double ave_proctime = Calc.getRoundedValue((double) task.getMaxWeight() / (double) this.averageSpeed);

        return (ave_comtime + ave_proctime);


    }

    public boolean isUnderMid(CPU cpu){


        //追加した場合のマップの平均速度，平均帯域幅を取得する．
        double aveSpeed = Calc.getRoundedValue((this.currentTotalSpeed + cpu.getSpeed()) / (double) (this.candidateCPUmap.size() + 1));
        Iterator<CPU> cpuIte = this.candidateCPUmap.values().iterator();
        long totalBW = this.currentTotalBW;
        long totalNum = this.currentTotalLinkNum;

        while (cpuIte.hasNext()) {
            CPU proc = cpuIte.next();
            //cpu <-> proc間のリンク合計を調べる．
            long link = this.env.getLink(Long.valueOf(cpu.getCpuID()).intValue(), Long.valueOf(proc.getCpuID()).intValue());
            totalBW += link;
            totalNum++;
        }

        if (this.candidateCPUmap.isEmpty()) {
            totalNum = 1;
        }
        double aveBW = Calc.getRoundedValue(totalBW / (double) totalNum);
        double A = Calc.getRoundedValue(aveSpeed / this.averageSpeed);
        double B = Calc.getRoundedValue(aveBW / this.averageBW);
        double aveDifTask = Calc.getRoundedValue(this.totalDifBlevelTaskSize / (double) Math.max(1,this.freeCount));
        double aveDifData = Calc.getRoundedValue(this.totalDifBlevelDataSize / (double)Math.max(1, this.freeCount));


        double rightValue = (-1) * Calc.getRoundedValue((A / B) * (aveDifData / (double) aveDifTask));

        double rate = Calc.getRoundedValue(A/B);
        if(aveDifData == 0){
            aveDifData = 1;
        }
        double cof = (-1)*Calc.getRoundedValue(this.beta_alpha*aveDifTask/aveDifData);
        if(rate >=cof){
            return true;
        }else{
            return false;
        }

    }

    public boolean isOverMid(CPU cpu){

        //追加した場合のマップの平均速度，平均帯域幅を取得する．
        double aveSpeed = Calc.getRoundedValue((this.currentTotalSpeed + cpu.getSpeed()) / (double) (this.candidateCPUmap.size() + 1));
        Iterator<CPU> cpuIte = this.candidateCPUmap.values().iterator();
        long totalBW = this.currentTotalBW;
        long totalNum = this.currentTotalLinkNum;

        while (cpuIte.hasNext()) {
            CPU proc = cpuIte.next();
            //cpu <-> proc間のリンク合計を調べる．
            long link = this.env.getLink(Long.valueOf(cpu.getCpuID()).intValue(), Long.valueOf(proc.getCpuID()).intValue());
            totalBW += link;
            totalNum++;
        }

        if (this.candidateCPUmap.isEmpty()) {
            totalNum = 1;
        }
        double aveBW = Calc.getRoundedValue(totalBW / (double) totalNum);
        double A = Calc.getRoundedValue(aveSpeed / this.averageSpeed);
        double B = Calc.getRoundedValue(aveBW / this.averageBW);
        double aveDifTask = Calc.getRoundedValue(this.totalDifBlevelTaskSize / (double) Math.max(1,this.freeCount));
        double aveDifData = Calc.getRoundedValue(this.totalDifBlevelDataSize / (double)Math.max(1, this.freeCount));


        double rightValue = (-1) * Calc.getRoundedValue((A / B) * (aveDifData / (double) aveDifTask));

        double rate = Calc.getRoundedValue(A/B);
        double cof = (-1)*Calc.getRoundedValue(this.beta_alpha*aveDifTask/aveDifData);
        if(rate <=cof){
            return true;
        }else{
            return false;
        }

    }

    //現在のrightvalueを更新する．
    public void updateRightValue(CPU cpu){
        //まだ何も入ってなければtrueを返す
        if (this.candidateCPUmap.isEmpty()) {
            this.currentRightValue = (-1) * Calc.getRoundedValue(totalDifBlevelTaskSize / (double) this.totalDifBlevelDataSize);
            return;
        }        double aveSpeed = Calc.getRoundedValue((this.currentTotalSpeed + cpu.getSpeed()) / (double) (this.candidateCPUmap.size() + 1));


        //追加した場合のマップの平均速度，平均帯域幅を取得する．
        Iterator<CPU> cpuIte = this.candidateCPUmap.values().iterator();
        long totalBW = this.currentTotalBW;
        long totalNum = this.currentTotalLinkNum;

        while (cpuIte.hasNext()) {
            CPU proc = cpuIte.next();
            //cpu <-> proc間のリンク合計を調べる．
            long link = this.env.getLink(Long.valueOf(cpu.getCpuID()).intValue(), Long.valueOf(proc.getCpuID()).intValue());
            totalBW += link;
            totalNum++;
        }
        this.currentTotalLinkNum = totalNum;
        this.currentTotalBW = totalBW;
        this.currentTotalSpeed +=cpu.getSpeed();

        if (this.candidateCPUmap.isEmpty()) {
            totalNum = 1;
        }
        double aveBW = Calc.getRoundedValue(totalBW / (double) totalNum);
        double A = Calc.getRoundedValue(aveSpeed / this.averageSpeed);
        double B = Calc.getRoundedValue(aveBW / this.averageBW);
        double aveDifTask = Calc.getRoundedValue(this.totalDifBlevelTaskSize / (double) Math.max(1,this.freeCount));
        double aveDifData = Calc.getRoundedValue(this.totalDifBlevelDataSize / (double)Math.max(1, this.freeCount));


        double rightValue = (-1) * Calc.getRoundedValue((A / B) * (aveDifData / (double) aveDifTask));
        this.currentRightValue = rightValue;

    }

    /**
     * オーバーライド用メソッド
     *
     * @param cpu
     * @return
     */
    public boolean isCPUCandidate(CPU cpu) {
        //まだ何も入ってなければtrueを返す
        if (this.candidateCPUmap.isEmpty()) {
            this.currentRightValue = (-1) * Calc.getRoundedValue(totalDifBlevelTaskSize / (double) this.totalDifBlevelDataSize);

            return true;
        }

        //追加した場合のマップの平均速度，平均帯域幅を取得する．
        double aveSpeed = Calc.getRoundedValue((this.currentTotalSpeed + cpu.getSpeed()) / (double) (this.candidateCPUmap.size() + 1));
        Iterator<CPU> cpuIte = this.candidateCPUmap.values().iterator();
        long totalBW = this.currentTotalBW;
        long totalNum = this.currentTotalLinkNum;

        while (cpuIte.hasNext()) {
            CPU proc = cpuIte.next();
            //cpu <-> proc間のリンク合計を調べる．
            long link = this.env.getLink(Long.valueOf(cpu.getCpuID()).intValue(), Long.valueOf(proc.getCpuID()).intValue());
            totalBW += link;
            totalNum++;
        }

        if (this.candidateCPUmap.isEmpty()) {
            totalNum = 1;
        }
        double aveBW = Calc.getRoundedValue(totalBW / (double) totalNum);
        double A = Calc.getRoundedValue(aveSpeed / this.averageSpeed);
        double B = Calc.getRoundedValue(aveBW / this.averageBW);
        double aveDifTask = Calc.getRoundedValue(this.totalDifBlevelTaskSize / (double) Math.max(1,this.freeCount));
        double aveDifData = Calc.getRoundedValue(this.totalDifBlevelDataSize / (double)Math.max(1, this.freeCount));


        double rightValue = (-1) * Calc.getRoundedValue((A / B) * (aveDifData / (double) aveDifTask));

        double rate = Calc.getRoundedValue(A/B);
        if(aveDifData == 0){
            aveDifData = 1;
        }
        double cof = (-1)*Calc.getRoundedValue(this.beta_alpha*aveDifTask/aveDifData);

        if(rate >=cof &&(this.isDeltaRankWPositive==true)){
            this.currentRightValue = rightValue;

            return true;
        }else if(rate <= cof &&(this.isDeltaRankWPositive==false)){
            this.currentRightValue = rightValue;

            return true;
        } else{
            if(rightValue <= this.currentRightValue && (this.isDeltaRankWPositive)){
                this.currentRightValue = rightValue;

                //少なくとも減ればOK
                return true;
            }else if (rightValue >= this.currentRightValue && (!this.isDeltaRankWPositive)){
                //増えればダメ
                return true;

            } else{
                return false;
            }
        }

    }


    public CPU configureTaskPair(){
        this.wcp = this.calcWCP();
        //まずは，各プロセッサについてindicative valueを計算する．
        Iterator<CPU> cpuIte = this.env.getCpuList().values().iterator();
        while (cpuIte.hasNext()) {
            CPU cpu = cpuIte.next();
            double deltaopt = this.calcDeltaOpt(cpu);
            double workload = Calc.getRoundedValue(deltaopt * cpu.getSpeed());
            if(this.maxWorkload <= workload){
                this.maxWorkload = workload;
            }
            /*double com = Calc.getRoundedValue(this.averageDataSize / this.env.getAveEndToEndLink(
                    Long.valueOf(cpu.getCpuID().longValue()).intValue()));
                    */
            long bw1 = this.env.getBWFromCPU(cpu);
            double com = Calc.getRoundedValue(this.averageDataSize / (double) bw1);

            double indexValue = Calc.getRoundedValue(deltaopt + com);
            IndexInfo info = new IndexInfo(cpu, indexValue, deltaopt);
            this.indexQueue.offer(info);

        }

        PriorityQueue<IndexInfo> tmpQueue = new PriorityQueue<IndexInfo>(5, new IndexComparator());

        IndexInfo firstInfo = this.indexQueue.peek();
        CPU firstCPU = firstInfo.getCpu();
        this.firstProcessor = firstCPU;
        this.firstCPUNum = (long)Math.ceil(this.apl.getMaxWeight()/(double)(firstInfo.getLowerBound()*firstCPU.getSpeed()));

        long firstBW = this.env.getBWFromCPU(firstCPU);
        this.beta_alpha = Calc.getRoundedValue(firstBW / (double) firstCPU.getSpeed());
        this.averageSpeed = firstCPU.getSpeed();
        this.averageBW = firstBW;
        //次に，最速CPU用の環境にする．
        long ave_speed = firstCPU.getSpeed();
        long aveLink = firstBW;

        Iterator<AbstractTask> taskIte = this.apl.getTaskList().values().iterator();
        //各タスクについて，平均実行時間をセットする．
        while (taskIte.hasNext()) {
            AbstractTask task = taskIte.next();
            //double ave_time_double = Calc.getRoundedValue(task.getMaxWeight()/(double)cpuSum/cpuNum);
            double ave_time_double = Calc.getRoundedValue(task.getMaxWeight() / ave_speed);
            long ave_time = (long) ave_time_double;
            task.setAve_procTime(ave_time);
            //System.out.println("平均:"+ave_time);

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

        }

        CustomIDSet idSet = new CustomIDSet();
        //Blevelを計算しなおす．
        Iterator<Long> startIDIte = this.apl.getStartTaskSet().iterator();
        while (startIDIte.hasNext()) {
            Long sID = startIDIte.next();
            AbstractTask startTask = this.apl.findTaskByLastID(sID);
            this.getMaxBlevel(startTask, idSet);
        }
        long mBlevel = 0;
/*
        while (!this.indexQueue.isEmpty()) {
            IndexInfo idxInfo = this.indexQueue.poll();
            CPU targetCPU = idxInfo.getCpu();

            long bandwidth = this.env.getBWFromCPU(targetCPU);
            double distance = Calc.getRoundedValue(Math.sqrt(Math.pow((Math.abs(firstCPU.getSpeed() - targetCPU.getSpeed())), 2) +
                    Math.pow(Math.abs(firstCPU.getBw() - bandwidth), 2)));
            DistanceInfo diffInfo = new DistanceInfo(targetCPU, distance, idxInfo.getLowerBound());
            this.distanceQueue.offer(diffInfo);

        }
        */
        AbstractTask maxStartTask = null;
        //Bsucはセットされているので，cpタスクを集める．
        Iterator<Long> startIDIte2 = this.apl.getStartTaskSet().iterator();
        while(startIDIte2.hasNext()){
            Long sID = startIDIte2.next();
            AbstractTask startTask = this.apl.findTaskByLastID(sID);
            if(startTask.getBlevel() >= mBlevel){
                maxStartTask = startTask;
                mBlevel = startTask.getBlevel();
            }

        }

        this.CPSet.add(maxStartTask.getIDVector().get(1));

        //maxStartTaskから初めて，ENDまでの間にCPへ入れる
        while(!maxStartTask.getDsucList().isEmpty()){
            maxStartTask = this.apl.findTaskByLastID(maxStartTask.getBsuc().get(1));
            this.CPSet.add(maxStartTask.getIDVector().get(1));
        }

        return firstCPU;
    }


    public Hashtable<Long, CPU> deriveCaindidateCPUMapByAverageDiffBlevel2() {
        //まずは高性能プロセッサを一つ選ぶ
        //distanceInfoに格納し，firstCPUに基づくblevelをセットし，CPタスク集合をセットするまで
        CPU firstCPU = this.configureTaskPair();

        //long freeCount = 0;
        this.freeCount = 0;
        double minDif = 999999999;
        double maxDif = 0;

       this.diffBlevel = Constants.MAXValue;
       // this.diffBlevel = 0;

        //Freeタスクがなくなるまでに行うループ
        // while(!this.freeClusterList.isEmpty()){
        while (!this.freeClusterList.isEmpty()) {
            AbstractTask maxTask = null;
            //まずは，選ばれたものに対して最もblevelの差が小さなものを選択する．
            Iterator<Long> freeIte = this.freeClusterList.iterator();
            PriorityQueue<AbstractTask> rankQueue = new PriorityQueue<AbstractTask>(5, new BlevelComparator());
            while (freeIte.hasNext()) {
                Long id = freeIte.next();
                AbstractTask t = this.apl.findTaskByLastID(id);
                rankQueue.offer(t);
            }
            //先頭タスクを取得する．
            maxTask = rankQueue.poll();
            if (maxTask.getDsucList().isEmpty()) {
                break;
            }
            AbstractTask nextTask = rankQueue.poll();
            if (nextTask == null) {
            } else {

                //dif_task * dif_dataのマイナスであるときだけ，逆転の可能性がある．よって，この場合だけ考慮にいれる．
                //左側と真ん中の差分の小さいものを選ぶ．真ん中にちかいもののを基準に考える．これにより，どの二者も逆転することがなくなる．
                //が，現実にやろうとするとプロセッサが極端に少なくなってしまうのでうまくいかない．そこで「ある程度の逆転は許す」という
                //方針とする．応答時間に影響しないタスク同士の逆転は許すが，クリティカルなタスクはダメ．
                //クリティカルパス上のタスクと次のタスクの差分の最小値を超えないようにする．
                //クリティカルパス上のタスクとそうでないタスクのでの?割合とmidとの差分の最小値をとるときの値とする．
                if((this.CPSet.contains(maxTask.getIDVector().get(1)))&&(!this.CPSet.contains(nextTask.getIDVector().get(1)))){
                   long diftask = maxTask.getBlevelTotalTaskSize() - nextTask.getBlevelTotalTaskSize();
                    long difdata = maxTask.getBlevelTotalDataSize() - nextTask.getBlevelTotalDataSize();
                    //if (diftask * difdata < 0) {
                      //  long dblevel = maxTask.getBlevel() - nextTask.getBlevel();
                        //long dblevel =
                    double leftvalue = 0.0;
                    if(diftask*difdata==0){
                        leftvalue = 0;
                    }else{
                        leftvalue = (-1)*Calc.getRoundedValue(Calc.getRoundedValue((double)difdata/(double)diftask));

                    }
                        double dblevel = this.beta_alpha - leftvalue;

                        if((Math.abs(dblevel)<= this.diffBlevel)/*&&(dblevel >0)*/){
                            this.diffBlevel = Math.abs(dblevel);
                            if(diftask<0){
                                this.isDeltaRankWPositive = false;

                            } else{
                                this. isDeltaRankWPositive = true;
                            }

                            //this.task_k = maxTask;
                           // this.task_s = nextTask;
                            this.totalDifBlevelDataSize = difdata;
                            this.totalDifBlevelTaskSize = diftask;
                        }

                  //  }
               }
            }


            this.freeClusterList.remove(maxTask.getIDVector().get(1));
            this.scheduledTaskSet.add(maxTask.getIDVector().get(1));
            //後続タスクがfreeかどうか
            Iterator<DataDependence> dsucIte = maxTask.getDsucList().iterator();
            while (dsucIte.hasNext()) {
                DataDependence dsuc = dsucIte.next();
                AbstractTask dsucTask = this.apl.findTaskByLastID(dsuc.getToID().get(1));
                //dsucTaskの先行タスクを調べる．
                Iterator<DataDependence> dpredIte = dsucTask.getDpredList().iterator();
                boolean isAllScheduled = true;
                while (dpredIte.hasNext()) {
                    DataDependence dpred = dpredIte.next();
                    if (!this.scheduledTaskSet.contains(dpred.getFromID().get(1))) {
                        isAllScheduled = false;
                        break;
                    }
                }
                if (isAllScheduled) {
                    this.freeClusterList.add(dsucTask.getIDVector().get(1));
                }
            }

        }
        //あとは，A/Bを選択するのみ．
        //  this.candidateCPUmap = this.selectionProcess();
        if(this.totalDifBlevelTaskSize==0&&totalDifBlevelDataSize==0){
            this.candidateCPUmap = this.deriveCandidateCPUMap();
        }else{
            this.candidateCPUmap = this.deriveCandidateCPUMapForScheduling();

        }
        // this.candidateCPUmap = this.selectionProcess();
        //this.candidateCPUmap = this.deriveCandidateCPUMap();


        return this.candidateCPUmap;
    }


    /**
     * 入力として与えられたCPUリストから，候補となるCPUリスト
     * を指標の小さい順に選択する．そして，その結果をハッシュに格納する．
     *
     * @return
     */
    public Hashtable<Long, CPU> deriveCandidateCPUMapForScheduling() {

        Iterator<IndexInfo> ite = this.indexQueue.iterator();
        long ttlTaskSize = 0;
        long newIndex = 0;
        //先頭の要素を閲覧する．
        IndexInfo  dinfo = this.indexQueue.peek();
        double fworkload = Calc.getRoundedValue(dinfo.getCpu().getSpeed()*dinfo.getLowerBound());
        //最低限の数を決める．
       // long minNum = (long)Math.ceil(Calc.getRoundedValue(this.apl.getMaxWeight()/fworkload));
        long minNum = (long)Math.ceil(Calc.getRoundedValue(this.apl.getMaxWeight()/this.maxWorkload));

        long globalcount = 0;
        boolean isUnder = false;
        boolean isOver = false;
        while (!this.indexQueue.isEmpty()) {
            //DistanceInfo  info = this.distanceQueue.poll();
            IndexInfo info = this.indexQueue.poll();
            CPU cpu0 = info.getCpu();
            long bw0 = this.env.getBWFromCPU(cpu0);
            //System.out.println("S:"+cpu0.getSpeed() + "B:"+bw0);
            //ここで，各スケジューリング用の判断基準に入る．
            if(globalcount <= minNum){
               // double lowerbound = info.getLowerbound();
                double lowerbound = info.getLowerBound();
                this.totalWorkload -= lowerbound * info.getCpu().getSpeed();
                CPU cpu = info.getCpu();
                long bw = this.env.getBWFromCPU(cpu);

                Iterator<CPU> currentCPUIte = this.candidateCPUmap.values().iterator();

                if (this.candidateCPUmap.isEmpty()) {
                    this.currentTotalBW += this.env.getBWFromCPU(cpu);
                    this.currentTotalLinkNum++;
                }
                while (currentCPUIte.hasNext()) {
                    CPU proc = currentCPUIte.next();
                    //cpu <-> proc間のリンク合計を調べる．
                    long link = this.env.getLink(Long.valueOf(cpu.getOldCPUID()).intValue(), Long.valueOf(proc.getOldCPUID()).intValue());
                    this.currentTotalBW += link;
                    this.currentTotalLinkNum++;
                }
                this.currentTotalSpeed += cpu.getSpeed();


                cpu.setBw(bw);
                cpu.setCpuID(new Long(newIndex));
                cpu.setMachineID(new Long(newIndex));

                this.candidateCPUmap.put(info.getCpu().getCpuID(), info.getCpu());
                newIndex++;
                double aveSpeed = Calc.getRoundedValue((this.currentTotalSpeed) / (double) (this.candidateCPUmap.size()));
                double aveBW = Calc.getRoundedValue(this.currentTotalBW / (double) this.currentTotalLinkNum);

                double A = Calc.getRoundedValue(aveSpeed / this.averageSpeed);
                double B = Calc.getRoundedValue(aveBW / this.averageBW);
                double aveDifTask = Calc.getRoundedValue(this.totalDifBlevelTaskSize / (double) Math.max(1,this.freeCount));
                double aveDifData = Calc.getRoundedValue(this.totalDifBlevelDataSize / (double)Math.max(1, this.freeCount));
                this.currentRightValue = (-1) * Calc.getRoundedValue((A / B) * (aveDifData / (double) aveDifTask));
                //更新する．
               // this.updateRightValue(cpu);
            }else{
                //候補になりうるか
                if (!isCPUCandidate(info.getCpu())) {
                    //falseは，rightValueが前回より増えそうな時
                    //System.out.println("×");
                    continue;
                } else {
                    //rightValueが既にmid以下，もしくは前回よりも減ったとき
                    //既に超えない状態であれば終了
                    if(isUnderMid(info.getCpu()) && this.isDeltaRankWPositive==true){
                        //既に小さいのであればそこで終了させる．
                        isUnder = true;
                    }else if(isOverMid(info.getCpu()) && (this.isDeltaRankWPositive == false)){
                        isOver = true;
                    }
                    //System.out.println("◯");

                    double lowerbound = info.getLowerBound();
                    this.totalWorkload -= lowerbound * info.getCpu().getSpeed();
                    CPU cpu = info.getCpu();
                    long bw = this.env.getBWFromCPU(cpu);

                    Iterator<CPU> currentCPUIte = this.candidateCPUmap.values().iterator();

                    if (this.candidateCPUmap.isEmpty()) {
                        this.currentTotalBW += this.env.getBWFromCPU(cpu);
                        this.currentTotalLinkNum++;
                    }
                    while (currentCPUIte.hasNext()) {
                        CPU proc = currentCPUIte.next();
                        //cpu <-> proc間のリンク合計を調べる．
                        long link = this.env.getLink(Long.valueOf(cpu.getOldCPUID()).intValue(), Long.valueOf(proc.getOldCPUID()).intValue());
                        this.currentTotalBW += link;
                        this.currentTotalLinkNum++;
                    }
                    this.currentTotalSpeed += cpu.getSpeed();


                    cpu.setBw(bw);
                    cpu.setCpuID(new Long(newIndex));
                    cpu.setMachineID(new Long(newIndex));

                    this.candidateCPUmap.put(info.getCpu().getCpuID(), info.getCpu());
                    newIndex++;


                    if (this.totalWorkload <= 0) {
                        break;
                    }

                    if(isUnder && (this.isDeltaRankWPositive == true)){
                        break;
                    }

                    if(isOver && (this.isDeltaRankWPositive == false)){
                        break;
                    }
            }


                //System.out.println("ID:"+info.getCpu().getCpuID()+" :Value:"+info.getIndexValue());
            }
            globalcount++;

        }


        return this.candidateCPUmap;


    }


    /**
     * @return
     */
    public Hashtable<Long, CPU> mergeProcess() {
        this.candidateCPUmap = this.deriveCandidateCPUMap();

        long newIndex = this.candidateCPUmap.size();
        //あとは，追加でA/Bを満たすものを選ぶのみ．
        double left = this.getRightValue(this.candidateCPUmap, null);
        if (left >= this.beta_alpha) {
        } else {
            //leftを増加させるようなCPUを追加する．
            while (!this.indexQueue.isEmpty()) {
                IndexInfo info = indexQueue.poll();
                CPU cpu = info.getCpu();
                double tmpLeft = this.getRightValue(this.candidateCPUmap, cpu);
                //増加したらOK
                if (tmpLeft >= left) {
                    //更新
                    this.currentTotalSpeed += cpu.getSpeed();
                    Iterator<CPU> currentCPUIte = this.candidateCPUmap.values().iterator();
                    while (currentCPUIte.hasNext()) {
                        CPU proc = currentCPUIte.next();
                        //cpu <-> proc間のリンク合計を調べる．
                        long link = this.env.getLink(Long.valueOf(cpu.getOldCPUID()).intValue(), Long.valueOf(proc.getOldCPUID()).intValue());
                        this.currentTotalBW += link;
                        this.currentTotalLinkNum++;
                    }
                    long bw = this.env.getBWFromCPU(cpu);
                    cpu.setBw(bw);
                    cpu.setCpuID(new Long(newIndex));
                    cpu.setMachineID(new Long(newIndex));
                    newIndex++;
                    this.candidateCPUmap.put(cpu.getCpuID(), cpu);
                    left = this.getRightValue(this.candidateCPUmap, null);


                }
                if (left >= this.beta_alpha) {
                    break;
                }

            }
        }

        return this.candidateCPUmap;

    }


    /**
     * @param map
     * @return
     */
    public double getRightValue(Hashtable<Long, CPU> map, CPU newCPU) {
        //nullなら現在のマップでの値を返す．
        if (newCPU == null) {
            double A = Calc.getRoundedValue(Calc.getRoundedValue((double) this.currentTotalSpeed /
                    (double) this.candidateCPUmap.size()) / this.averageSpeed);
            double B = Calc.getRoundedValue(Calc.getRoundedValue((double) this.currentTotalBW / (double) this.currentTotalLinkNum) / this.averageBW);
            double left = (-1) * Calc.getRoundedValue(Calc.getRoundedValue(A / B) * (this.task_k.getBlevelTotalDataSize() -
                    this.task_s.getBlevelTotalDataSize()) / Calc.getRoundedValue(this.task_k.getBlevelTotalTaskSize() - this.task_s.getBlevelTotalTaskSize()));
            return left;

        } else {
            long totalBW = this.currentTotalBW;
            long totalLinkNum = this.currentTotalLinkNum;
            long totalSpeed = this.currentTotalSpeed + newCPU.getSpeed();
            long cpuNum = map.size() + 1;
            Iterator<CPU> currentCPUIte = map.values().iterator();
            while (currentCPUIte.hasNext()) {
                CPU proc = currentCPUIte.next();
                //cpu <-> proc間のリンク合計を調べる．
                long link = this.env.getLink(Long.valueOf(newCPU.getOldCPUID()).intValue(), Long.valueOf(proc.getOldCPUID()).intValue());
                totalBW += link;
                totalLinkNum++;
            }


            double A = Calc.getRoundedValue(Calc.getRoundedValue((double) totalSpeed / (double) cpuNum) / this.averageSpeed);
            double B = Calc.getRoundedValue(Calc.getRoundedValue((double) totalBW / (double) totalLinkNum) / this.averageBW);
            double right = (-1) * Calc.getRoundedValue(Calc.getRoundedValue(A / B) * (this.task_k.getBlevelTotalDataSize() -
                    this.task_s.getBlevelTotalDataSize()) / Calc.getRoundedValue(this.task_k.getBlevelTotalTaskSize() - this.task_s.getBlevelTotalTaskSize()));
            // double right = Calc.getRoundedValue((double)totalBW/(double)totalLinkNum);
            return right;
        }


    }


    public void prepare() {
        super.prepare();
        //アプリから，タスクリストを取得
        CustomIDSet startSet = new CustomIDSet();

        //CPUを全て未割り当て状態とする．
        Iterator<CPU> umIte = this.cpuTable.values().iterator();

        //マシンに対するループ
        //各マシンを未割り当てCPUリストへ追加させる．
        while (umIte.hasNext()) {
            CPU cpu = umIte.next();
            this.unAssignedCPUs.add(cpu.getCpuID());
        }


        Iterator<AbstractTask> ite = this.apl.getTaskList().values().iterator();
        while (ite.hasNext()) {
            AbstractTask task = ite.next();
            //先行タスクがなけｒば，スタートセットに入れる．
            if (task.getDpredList().isEmpty()) {
                startSet.add(task.getIDVector().get(1));
            }
        }
        //Blevelのセットをする．
        Iterator<Long> startIDIte = startSet.iterator();
        CustomIDSet idSet = new CustomIDSet();

        while (startIDIte.hasNext()) {
            Long sID = startIDIte.next();
            AbstractTask startTask = this.apl.findTaskByLastID(sID);
            this.getMaxBlevel(startTask, idSet);
        }

        //   this.calculateInitialTlevel(endTask, initialCPU, false);
        this.apl.setStartTaskSet(startSet);

        Hashtable<Long, AbstractTask> taskTable = this.apl.getTaskList();
        Collection<AbstractTask> taskCollection = taskTable.values();
        Iterator<AbstractTask> ite2 = taskCollection.iterator();
        //各タスクに対するループ処理
        while (ite2.hasNext()) {
            AbstractTask task = ite2.next();
            //もしタスククラスタがSTARTノードであれば，この時点でFreeリストへ入れる．
            if (task.getDpredList().isEmpty()) {
                this.freeClusterList.add(task.getIDVector().get(1));
            }
        }
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
            this.heft.getBlevelQueue().offer(task);
            task.setBlevelTotalDataSize(0);
            task.setBlevelTotalTaskSize(task.getMaxWeight());
            return endBlevel;
        }

        //先行タスクのtlevel値を取得する．
        Iterator<DataDependence> dsucIte = task.getDsucList().iterator();
        long maxBlevel = 0;
        long realBlevel = 0;

        long totalData = 0;
        long totalTask = 0;

        //先行タスクに対するループ
        while (dsucIte.hasNext()) {
            DataDependence dsuc = dsucIte.next();
            AbstractTask sucTask = this.apl.findTaskByLastID(dsuc.getToID().get(1));
            long sucBlevel = 0;

            if (set.contains(sucTask.getIDVector().get(1))) {
                sucBlevel = sucTask.getBlevel();
            } else {
                //もし未チェックであれば，再計算する．
                sucBlevel = this.getMaxBlevel(sucTask, set);
            }
            //後続タスクから，自身のBlevel値を計算する．
            realBlevel = task.getAve_procTime() + this.heft.getComTime(task, sucTask) + sucBlevel;

            if (maxBlevel <= realBlevel) {
                maxBlevel = realBlevel;
                task.setBlevel(realBlevel);
                task.setBsuc(sucTask.getIDVector());
                totalData = sucTask.getBlevelTotalDataSize();
                totalTask = sucTask.getBlevelTotalTaskSize();

            }
            DataDependence bsuc = task.findDDFromDsucList(task.getIDVector(), task.getBsuc());
            //Bsucとの間のwとcの合計を計算する．
            task.setBlevelTotalDataSize(totalData + bsuc.getMaxDataSize());
            task.setBlevelTotalTaskSize(totalTask + task.getMaxWeight());
        }

        this.heft.getBlevelQueue().offer(task);
        return maxBlevel;

    }


}
