package net.gripps.grouping.HSV;

import net.gripps.clustering.common.Constants;
import net.gripps.clustering.common.aplmodel.AbstractTask;
import net.gripps.clustering.common.aplmodel.BBTask;
import net.gripps.clustering.common.aplmodel.CustomIDSet;
import net.gripps.clustering.common.aplmodel.DataDependence;
import net.gripps.clustering.tool.Calc;
import net.gripps.environment.CPU;
import net.gripps.environment.Environment;
import net.gripps.grouping.HEFT.HEFTCandidateNodeSelectionManager;
import net.gripps.grouping.IndexComparator;
import net.gripps.grouping.IndexInfo;

import java.util.*;

/**
 * Created by kanemih on 2016/01/26.
 */
public class HSVCandidateNodeSelectionManager extends HEFTCandidateNodeSelectionManager {

    //protected double averageSpeed;

   // protected double averageBW;

    /**
     * CPUテーブル
     */
    //protected Hashtable<Long, CPU> cpuTable;

   // protected AbstractTask task_k;

    //protected AbstractTask task_s;

    protected double difHRank;

   // protected Hashtable<Long, CPU> candidateCPUmap;

    protected CustomIDSet scheduledTaskSet;

    protected PriorityQueue<AbstractTask> readyList;

    protected Hashtable<Long, Hashtable<Long, Double>> hrankMap;



    protected double currentTotalRank_k;

    protected double currentTotalRank_s;


    protected double totalAlpha;

    protected double currentTotalInverseAlpha;

    protected double totalInverseAlphaByFistCPU;

    protected double currentA;

    protected long currentNum;



    public HSVCandidateNodeSelectionManager(BBTask apl, String file, Environment env) {
        super(apl, file, env);
        this.averageBW  =0;
        this.difHRank = 0;

        this.cpuTable = this.env.getCpuList();
        this.scheduledTaskSet = new CustomIDSet();
        this.averageSpeed = 0;
        this.readyList = new PriorityQueue<AbstractTask>(5, new HSVGComparator());
        this.candidateCPUmap = new Hashtable<Long, CPU>();
        this.hrankMap = new Hashtable<Long, Hashtable<Long, Double>>();
        this.totalAlpha = 0;
        this.currentTotalInverseAlpha = 0;
        this.currentA = 0;
        this.firstCPUNum = 0;
        this.totalInverseAlphaByFistCPU = 0;
        this.currentNum = 0;
       this.initialize();

       this.prepare();


    }

    public double getHrank(Long taskID, Long cpuID){
        if(!this.hrankMap.containsKey(taskID)){
            return -1;
        }
        //タスクマップを取得する．
        Hashtable<Long, Double> taskMap = this.hrankMap.get(taskID);
        if(!taskMap.containsKey(cpuID)){
            return -1;
        }
        Double hrank = taskMap.get(cpuID);

        return hrank;
    }

    public void putHrank(Long taskID, long cpuID, double rank){
        if(!this.hrankMap.containsKey(taskID)){
            //新規にmapを作る．
            Hashtable<Long, Double> taskMap = new Hashtable<Long, Double>();
            taskMap.put(cpuID, rank);
            this.hrankMap.put(taskID, taskMap);
        }else{
            //taskマップが存在する場合
            Hashtable<Long, Double> tMap = this.hrankMap.get(taskID);
            if(!tMap.containsKey(cpuID)){
                //新規にcpu用の値をセットする．
                tMap.put(cpuID, rank);
            }else{
                tMap.put(cpuID, rank);
            }
        }

    }

    public double  getMaxHSVLevel(AbstractTask task, CPU cpu, CustomIDSet set) {
        set.add(task.getIDVector().get(1));
        AbstractTask dominatingTask = null;


        //ENDタスクであれば，blevelをそのまま帰す．
        if (task.getDsucList().isEmpty()) {
            double  endBlevel = Calc.getRoundedValue(task.getMaxWeight()/(double)cpu.getSpeed());
            this.putHrank(task.getIDVector().get(1), cpu.getCpuID(), endBlevel);
            task.addTotalHSVRankG(endBlevel);
            return endBlevel;
        }

        //後続タスクのblevel値を取得する．
        Iterator<DataDependence> dsucIte = task.getDsucList().iterator();
        double  maxBlevel = 0;
        double  realBlevel = 0;
        Vector<Long> sucIDVector = null;

        //後続タスクに対するループ
        while (dsucIte.hasNext()) {
            DataDependence dsuc = dsucIte.next();
            AbstractTask sucTask = this.apl.findTaskByLastID(dsuc.getToID().get(1));
            double sucBlevel = 0;
            sucBlevel = this.getHrank(task.getIDVector().get(1), cpu.getCpuID());
            if (sucBlevel>=0) {
                //sucBlevel = sucTask.getHsv_blevel().get(cpu.getCpuID());
            } else {
                //もし未チェックであれば，再計算する．
                sucBlevel = this.getMaxHSVLevel(sucTask, cpu, set);
            }
            //後続タスクから，自身のBlevel値を計算する．
            realBlevel = Calc.getRoundedValue(task.getMaxWeight()/(double)cpu.getSpeed()) +
                    Calc.getRoundedValue(dsuc.getMaxDataSize()/(double)this.env.getBWFromCPU(cpu)) + sucBlevel;

            if (maxBlevel <= realBlevel) {
                maxBlevel = realBlevel;
                sucIDVector = dsuc.getToID();
            }
        }
        this.putHrank(task.getIDVector().get(1), cpu.getCpuID(), maxBlevel);
        task.addTotalHSVRankG(maxBlevel);
        task.setBsuc(sucIDVector);
        //task.setHprv_rank(maxBlevel)

        return maxBlevel;

    }

    public double  getMaxFirstHSVLevel(AbstractTask task, CPU cpu, CustomIDSet set) {
        set.add(task.getIDVector().get(1));
        AbstractTask dominatingTask = null;


        //ENDタスクであれば，blevelをそのまま帰す．
        if (task.getDsucList().isEmpty()) {
            double  endBlevel = Calc.getRoundedValue(task.getMaxWeight()/(double)cpu.getSpeed());
            this.putHrank(task.getIDVector().get(1), cpu.getCpuID(), endBlevel);
            task.addTotalHSVRankG(endBlevel);
            task.setHSVRankG((long)endBlevel);
            return endBlevel;
        }

        //後続タスクのblevel値を取得する．
        Iterator<DataDependence> dsucIte = task.getDsucList().iterator();
        double  maxBlevel = 0;
        double  realBlevel = 0;

        //後続タスクに対するループ
        while (dsucIte.hasNext()) {
            DataDependence dsuc = dsucIte.next();
            AbstractTask sucTask = this.apl.findTaskByLastID(dsuc.getToID().get(1));
            double sucBlevel = 0;
            sucBlevel = this.getHrank(task.getIDVector().get(1), cpu.getCpuID());
            if (sucBlevel>=0) {
                //sucBlevel = sucTask.getHsv_blevel().get(cpu.getCpuID());
            } else {
                //もし未チェックであれば，再計算する．
                sucBlevel = this.getMaxFirstHSVLevel(sucTask, cpu, set);
            }
            //後続タスクから，自身のBlevel値を計算する．
            realBlevel = Calc.getRoundedValue(task.getMaxWeight()/(double)cpu.getSpeed()) +
                    Calc.getRoundedValue(dsuc.getMaxDataSize()/(double)this.env.getBWFromCPU(cpu)) + sucBlevel;


            if (maxBlevel <= realBlevel) {
                maxBlevel = realBlevel;
            }
        }
        this.putHrank(task.getIDVector().get(1), cpu.getCpuID(), maxBlevel);
        task.setHSVRankG(((long)maxBlevel*task.getDsucList().size()));
        task.addTotalHSVRankG(maxBlevel);
        //task.setHprv_rank(maxBlevel)

        return maxBlevel;

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
        /*Iterator<Long> startIDIte = this.apl.getStartTaskSet().iterator();
        while (startIDIte.hasNext()) {
            Long sID = startIDIte.next();
            AbstractTask startTask = this.apl.findTaskByLastID(sID);
            this.getMaxBlevel(startTask, idSet);
        }*/


        return firstCPU;
    }




    public void HSV_prepare(){
        //まずは，firstCPUの決定，indexQueueの格納，maxWorkloadの決定，firstCPUによる平均タスクサイズ，データサイズを設定する．
        //さらに，firstCPUによるCP, firstCPUによる数Nを決める．
        this.configureTaskPair();

        //firstCPUによる，逆数の合計を計算する．
        this.totalInverseAlphaByFistCPU = Calc.getRoundedValue(this.firstCPUNum * (1/(double)this.firstProcessor.getSpeed()));

        this.beta_alpha = Calc.getRoundedValue(this.averageBW*this.totalInverseAlphaByFistCPU);

        //まずは，各タスクについて，各プロセッサにおけるblevel値を計算し，その平均値をhrankとして格納する．
        //実査には，すべてfirstCPUなので，そのままhrankとして格納できる．
        Iterator<CPU> cpuIte = this.env.getCpuList().values().iterator();

        CPU cpu = cpuIte.next();
        Iterator<Long> startIte = this.apl.getStartTaskSet().getList().iterator();

        while(startIte.hasNext()){
            Long startID = startIte.next();
            AbstractTask startTask = this.apl.findTaskByLastID(startID);
            //当該プロッサによる，blevel計算を開始する．
            this.getMaxFirstHSVLevel(startTask, this.firstProcessor, new CustomIDSet());
        }

        double  mBlevel = 0;

        AbstractTask maxStartTask = null;
        //Bsucはセットされているので，cpタスクを集める．
        Iterator<Long> startIDIte2 = this.apl.getStartTaskSet().iterator();
        while(startIDIte2.hasNext()){
            Long sID = startIDIte2.next();
            AbstractTask startTask = this.apl.findTaskByLastID(sID);
            if(startTask.getHSVRankG() >= mBlevel){
                maxStartTask = startTask;
                mBlevel = startTask.getHSVRankG();
            }

        }

        this.CPSet.add(maxStartTask.getIDVector().get(1));

        //maxStartTaskから初めて，ENDまでの間にCPへ入れる
        while(!maxStartTask.getDsucList().isEmpty()){
            maxStartTask = this.apl.findTaskByLastID(maxStartTask.getBsuc().get(1));
            this.CPSet.add(maxStartTask.getIDVector().get(1));
        }

     /*
        while(cpuIte.hasNext()){
            CPU cpu = cpuIte.next();
            Iterator<Long> startIte = this.apl.getStartTaskSet().getList().iterator();
            double alpha_inverse = Calc.getRoundedValue(1/(double)cpu.getSpeed());
            this.totalAlpha += alpha_inverse;

            while(startIte.hasNext()){
                Long startID = startIte.next();
                AbstractTask startTask = this.apl.findTaskByLastID(startID);
                //当該プロッサによる，blevel計算を開始する．
                this.getMaxFirstHSVLevel(startTask, cpu, new CustomIDSet());


            }
        }
        */



        //再度，スタートタスクをレディリストへ入れる処理
        Iterator<Long> startIte2 =  this.apl.getStartTaskSet().iterator();
        while(startIte2.hasNext()){
            Long id = startIte2.next();
            AbstractTask stask = this.apl.findTaskByLastID(id);
            this.readyList.add(stask);
        }

    }

    /**
     *
     * @return
     */
    public Hashtable<Long, CPU> deriveCaindidateCPUMapbyHSV(){

        this.HSV_prepare();
        if(!this.indexQueue.isEmpty()){
            this.indexQueue.clear();

        }
        this.diffBlevel = Constants.MAXValue;

        // this.initialize();
      //  this.prepare();
        //Freeタスクがなくなるまでに行うループ
        // while(!this.freeClusterList.isEmpty()){
        while (!this.readyList.isEmpty()) {
            AbstractTask maxTask = null;
            //まずは，選ばれたものに対して最もblevelの差が小さなものを選択する．
            //先頭タスクを取得する．
            maxTask = readyList.poll();
            double maxDuration = this.calcDuratoinTime(maxTask);
            Object[] oa = this.readyList.toArray();
            //一つのクラスタ内で，タスクのtlevel順にソートする．

            Arrays.sort(oa, new HSVGComparator());
            int len = oa.length;
            //最後の一つだったのなら，そこで終了．
            if(len == 0){
                break;
            }
            AbstractTask nextTask = (AbstractTask)oa[0];
            //maxTaskとnextTaskの差分を算出する．
           // long dif = maxTask.getHprv_rank() - nextTask.getHprv_rank();
            double nextDuration = this.calcDuratoinTime(nextTask);
            double durationDif = nextDuration - maxDuration;
            if((this.CPSet.contains(maxTask.getIDVector().get(1)))&&(!this.CPSet.contains(nextTask.getIDVector().get(1)))){
                long diftask =maxTask.getDsucList().size()* maxTask.getBlevelTotalTaskSize() - nextTask.getDsucList().size()*nextTask.getBlevelTotalTaskSize();
                long difdata = maxTask.getDsucList().size()*maxTask.getBlevelTotalDataSize() - nextTask.getDsucList().size()*nextTask.getBlevelTotalDataSize();
              //  if (diftask * difdata < 0) {
                    //  long dblevel = maxTask.getBlevel() - nextTask.getBlevel();
                    //long dblevel =
                    double leftvalue = (-1)*Calc.getRoundedValue((this.firstCPUNum*difdata)/(double)diftask);
                    double dblevel = Math.abs(this.beta_alpha - leftvalue);
                    if((dblevel <= this.diffBlevel)/*&&(dblevel >0)*/){
                        this.diffBlevel = dblevel;
                        if(diftask<0){
                            this.isDeltaRankWPositive = false;
                        }else{
                            this.isDeltaRankWPositive = true;
                        }

                        //this.task_k = maxTask;
                        // this.task_s = nextTask;
                        this.totalDifBlevelDataSize = difdata;
                        this.totalDifBlevelTaskSize = diftask;
                    }

            //    }
          }
         /*   if(durationDif > 0){
                if(durationDif > this.difHRank){
                    //taskの差分とdataの差分の掛け算がマイナスでなければならない
                    long difData = maxTask.getDsucList().size()*maxTask.getBlevelTotalDataSize() - nextTask.getDsucList().size()*nextTask.getBlevelTotalDataSize();
                    long difTask = maxTask.getDsucList().size()*maxTask.getBlevelTotalTaskSize() - nextTask.getDsucList().size()*nextTask.getBlevelTotalTaskSize();
                    if(difData * difTask < 0){
                       this.difHRank = durationDif;
                       this.task_k = maxTask;
                       this.task_s = nextTask;
                    }

                }

            }*/

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
                    this.readyList.add(dsucTask);
                }
            }

        }
        //あとは，A/Bを選択するのみ．
        //  this.candidateCPUmap = this.selectionProcess();
        if(this.totalDifBlevelDataSize == 0 && this.totalDifBlevelTaskSize ==0){
           // System.out.println("だめだった");
            this.candidateCPUmap = this.deriveCandidateCPUMap();
        }else{
            this.candidateCPUmap = this.mergeProcess();
        }

        // this.candidateCPUmap = this.selectionProcess();
        //this.candidateCPUmap = this.deriveCandidateCPUMap();

        return this.candidateCPUmap;


    }

    /**
     * hrankの合計を求める．出次数は含んでいないことに注意．
     * @param task
     * @param map
     * @return
     */
    public double getOrgTotalHrank(AbstractTask task, Hashtable<Long, CPU> map){
        int outdegree = task.getDsucList().size();
        double totalValue = 0;
        //指定のcpuマップ分で，rankを合計する．
        Hashtable<Long, Double> taskMap = this.hrankMap.get(task.getIDVector().get(1));
        Iterator<CPU> cpuIte = map.values().iterator();
        while(cpuIte.hasNext()){
            CPU cpu = cpuIte.next();
            Double val = taskMap.get(cpu.getCpuID());
            totalValue += val;

        }

        return totalValue;
    }





    /**
     * //orgRightが大きくなれば良い．
     * @param orgMid
     * @param orgRight
     * @return
     */
    public boolean isValueApproached( CPU cpu, double orgMid, double orgRight){

        //mapに対してcpuを加えた場合，評価式はどうなるかを調べる．
        long newNum = this.candidateCPUmap.size()+1;
        double newCurrentTotalInverseAlpha = this.currentTotalInverseAlpha + Calc.getRoundedValue(1/(double)cpu.getSpeed());
        double newA =Calc.getRoundedValue(newCurrentTotalInverseAlpha/(double)this.totalAlpha);

        //新しいBを調べる．
        Iterator<CPU> currentCPUIte = this.candidateCPUmap.values().iterator();
        long totalBW = this.currentTotalBW;
        long totalLinkNum = this.currentTotalLinkNum;
        while (currentCPUIte.hasNext()) {
            CPU proc = currentCPUIte.next();
            //cpu <-> proc間のリンク合計を調べる．
            long link = this.env.getLink(Long.valueOf(cpu.getOldCPUID()).intValue(), Long.valueOf(proc.getOldCPUID()).intValue());
            totalBW += link;
            totalLinkNum++;
        }
        double newBeta = Calc.getRoundedValue(totalBW/(double)totalLinkNum);
        double newB = Calc.getRoundedValue(newBeta/(double)this.averageBW);
        double newRight = Calc.getRoundedValue((-1)*newNum*((this.task_k.getDsucList().size()*this.task_k.getBlevelTotalDataSize() -
                this.task_s.getDsucList().size()*this.task_s.getBlevelTotalDataSize())/
                (this.task_k.getDsucList().size()*this.task_k.getBlevelTotalTaskSize() - this.task_s.getDsucList().size()*this.task_s.getBlevelTotalTaskSize()))*(1/(newA*newB)));
        if(newRight >= orgRight){
            return true;
        }else{
            return false;
        }

    }

    /**
     * 当該CPUを追加した場合，
     * rightValue<midならば，そのままtrue
     * midValue<rightValueならば，
     * 新しいrightValue < 現状rightValueならtrue
     * そうでなければfalseとする．
     * @param cpu
     * @return
     */
    public boolean isCPUCandidate(CPU cpu) {

        //追加した場合のマップの平均速度，平均帯域幅を取得する．
        double aveSpeed = Calc.getRoundedValue((this.currentTotalSpeed + cpu.getSpeed()) / (double) (this.candidateCPUmap.size() + 1));
        Iterator<CPU> cpuIte = this.candidateCPUmap.values().iterator();
        long totalBW = this.currentTotalBW;
        long totalNum = this.currentTotalLinkNum;
        double currentC = Calc.getRoundedValue(this.currentTotalInverseAlpha/this.totalInverseAlphaByFistCPU);
        double currentAveBW = Calc.getRoundedValue(this.currentTotalBW/(double)this.currentTotalLinkNum);
        double currentD = Calc.getRoundedValue(currentAveBW/this.averageBW);


        double currentRightValue = (-1)*Calc.getRoundedValue(this.currentNum*this.totalDifBlevelDataSize/
                (currentC*currentD*this.totalDifBlevelTaskSize));
        double midValue = Calc.getRoundedValue(this.averageBW*this.totalInverseAlphaByFistCPU);

        while (cpuIte.hasNext()) {
            CPU proc = cpuIte.next();
            double newTotalInverseAlpha = this.currentTotalInverseAlpha + Calc.getRoundedValue(1/(double)cpu.getSpeed());
            //cpu <-> proc間のリンク合計を調べる．
            long link = this.env.getLink(Long.valueOf(cpu.getOldCPUID()).intValue(), Long.valueOf(proc.getOldCPUID()).intValue());
            totalBW += link;
            totalNum++;
        }

        if (this.candidateCPUmap.isEmpty()) {
            totalNum = 1;
        }

        double newC = Calc.getRoundedValue((this.currentTotalInverseAlpha+(1/(double)cpu.getSpeed()))/this.totalInverseAlphaByFistCPU);
        double newAveBW = Calc.getRoundedValue(totalBW/(double)totalNum);
        double newD = Calc.getRoundedValue(newAveBW/this.averageBW);

        double newRightValue = (-1)*Calc.getRoundedValue((this.currentNum+1)*this.totalDifBlevelDataSize/
                (newC*newD*this.totalDifBlevelTaskSize));

        if(newRightValue <= midValue && this.isDeltaRankWPositive){
            return true;
        }else if(newRightValue>=midValue && !this.isDeltaRankWPositive){
            return true;

        }else{
            if(newRightValue <= currentRightValue && this.isDeltaRankWPositive){
                return true;
            }else if( newRightValue>=currentRightValue && !this.isDeltaRankWPositive){
               return true;
            }else{
                return false;
            }
        }

    }

    public boolean isOverMid(CPU cpu){
        //追加した場合のマップの平均速度，平均帯域幅を取得する．
        Iterator<CPU> cpuIte = this.candidateCPUmap.values().iterator();
        long totalBW = this.currentTotalBW;
        long totalNum = this.currentTotalLinkNum;
        double currentAveBW = Calc.getRoundedValue(this.currentTotalBW/(double)this.currentTotalLinkNum);

        double midValue = Calc.getRoundedValue(this.averageBW*this.totalInverseAlphaByFistCPU);

        while (cpuIte.hasNext()) {
            CPU proc = cpuIte.next();
            //cpu <-> proc間のリンク合計を調べる．
            long link = this.env.getLink(Long.valueOf(cpu.getOldCPUID()).intValue(), Long.valueOf(proc.getOldCPUID()).intValue());
            totalBW += link;
            totalNum++;
        }

        if (this.candidateCPUmap.isEmpty()) {
            totalNum = 1;
        }

        double newC = Calc.getRoundedValue((this.currentTotalInverseAlpha+(1/(double)cpu.getSpeed()))/this.totalInverseAlphaByFistCPU);
        double newAveBW = Calc.getRoundedValue(totalBW/(double)totalNum);
        double newD = Calc.getRoundedValue(newAveBW/this.averageBW);

        double newRightValue = (-1)*Calc.getRoundedValue((this.currentNum+1)*this.totalDifBlevelDataSize/
                (newC*newD*this.totalDifBlevelTaskSize));

        if(newRightValue >= midValue){
            return true;
        }else{
            return false;
        }

    }

    public boolean isUnderMid(CPU cpu){
        //追加した場合のマップの平均速度，平均帯域幅を取得する．
        Iterator<CPU> cpuIte = this.candidateCPUmap.values().iterator();
        long totalBW = this.currentTotalBW;
        long totalNum = this.currentTotalLinkNum;
        double currentAveBW = Calc.getRoundedValue(this.currentTotalBW/(double)this.currentTotalLinkNum);

        double midValue = Calc.getRoundedValue(this.averageBW*this.totalInverseAlphaByFistCPU);

        while (cpuIte.hasNext()) {
            CPU proc = cpuIte.next();
            //cpu <-> proc間のリンク合計を調べる．
            long link = this.env.getLink(Long.valueOf(cpu.getOldCPUID()).intValue(), Long.valueOf(proc.getOldCPUID()).intValue());
            totalBW += link;
            totalNum++;
        }

        if (this.candidateCPUmap.isEmpty()) {
            totalNum = 1;
        }

        double newC = Calc.getRoundedValue((this.currentTotalInverseAlpha+(1/(double)cpu.getSpeed()))/this.totalInverseAlphaByFistCPU);
        double newAveBW = Calc.getRoundedValue(totalBW/(double)totalNum);
        double newD = Calc.getRoundedValue(newAveBW/this.averageBW);

        double newRightValue = (-1)*Calc.getRoundedValue((this.currentNum+1)*this.totalDifBlevelDataSize/
                (newC*newD*this.totalDifBlevelTaskSize));

        if(newRightValue <= midValue){
            return true;
        }else{
            return false;
        }

    }

    public Hashtable<Long, CPU> mergeProcess() {

        if(this.indexQueue.isEmpty()){
            this.configureTaskPair();
        }
        double midValue1 = Calc.getRoundedValue(this.totalInverseAlphaByFistCPU*this.env.getBWFromCPU(this.firstProcessor));
        //double leftValue1 = (-1)*this.firstCPUNum*this.totalDifBlevelDataSize/this.totalDifBlevelTaskSize;

        Iterator<IndexInfo> ite = this.indexQueue.iterator();
        long ttlTaskSize = 0;
        long newIndex = 0;
        //先頭の要素を閲覧する．
        IndexInfo  dinfo = this.indexQueue.peek();
//        double fworkload = Calc.getRoundedValue(dinfo.getCpu().getSpeed()*dinfo.getLowerBound());
        //最低限の数を決める．
        // long minNum = (long)Math.ceil(Calc.getRoundedValue(this.apl.getMaxWeight()/fworkload));
        long minNum = (long)Math.ceil(Calc.getRoundedValue(this.apl.getMaxWeight()/this.maxWorkload));

        boolean isUnder = false;
        boolean isOver = false;
        long globalcount = 0;
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
                this.currentTotalInverseAlpha += Calc.getRoundedValue(1/(double)cpu.getSpeed());

                newIndex++;
                this.currentNum++;
            }else{
                //候補になりうるか
                if (!isCPUCandidate(info.getCpu())) {
                    //falseは，rightValueが前回より増えそうな時
                    //System.out.println("×");
                    continue;
                } else {
                    //rightValueが既にmid以下，もしくは前回よりも減ったとき
                    //既に超えない状態であれば終了
                    if(isUnderMid(info.getCpu()) && this.isDeltaRankWPositive){
                        //既に小さいのであればそこで終了させる．
                        isUnder = true;
                    }else if(isOverMid(info.getCpu()) && !this.isDeltaRankWPositive) {
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
                    this.currentNum++;



                    if (this.totalWorkload <= 0) {
                        break;
                    }

                    if(isUnder || isOver){
                        break;
                    }
                }


                //System.out.println("ID:"+info.getCpu().getCpuID()+" :Value:"+info.getIndexValue());
            }
            globalcount ++;

        }

        return this.candidateCPUmap;

    }

    /**
     * @return
     */
    public Hashtable<Long, CPU> mergeProcessBak() {

        this.candidateCPUmap = this.deriveCandidateCPUMap();
        if((this.task_k == null)||(this.task_s == null)){
            return this.candidateCPUmap;
        }
        long newIndex = this.candidateCPUmap.size();
        Iterator<CPU> cIte = this.candidateCPUmap.values().iterator();
        //現状の1/alphaの合計を調べる．
        while(cIte.hasNext()){
            CPU cpu = cIte.next();
            this.currentTotalInverseAlpha += Calc.getRoundedValue(1/(double)cpu.getSpeed());
        }
        double currentA = Calc.getRoundedValue((double)this.currentTotalInverseAlpha/(double)this.totalAlpha);
        double currentB =  Calc.getRoundedValue((this.currentTotalBW/(double)(this.currentTotalLinkNum/this.averageBW)));
        double difTotalDataSize = this.task_k.getDsucList().size()*this.task_k.getBlevelTotalDataSize() - this.task_s.getDsucList().size()*this.task_s.getBlevelTotalDataSize();
        double difTotalTaskSize = this.task_k.getDsucList().size()*this.task_k.getBlevelTotalTaskSize() - this.task_s.getDsucList().size()*this.task_s.getBlevelTotalTaskSize();
        double midValue = Calc.getRoundedValue(this.averageBW * this.totalAlpha);
        double leftValue =  Calc.getRoundedValue((-1)*this.env.getCpuList().size()*(difTotalDataSize/difTotalTaskSize));
        double rightValue = Calc.getRoundedValue((-1)*this.candidateCPUmap.size()*(difTotalDataSize/difTotalTaskSize)*(1/(currentA*currentB)));
        //現状がどうかを調べる．
        if(/*(leftValue <= midValue) &&*/(midValue<=rightValue)){
            //System.out.println("すでにOK");
            return this.candidateCPUmap;
        }
       // System.out.println("不十分");

        //以降は，candidateMapだけではダメな場合の処理
        //RightValueが小さすぎる場合．
        //プロセッサを追加する．
        //IndexQueueにはまだ残っているので，そこから順に選ぶ．
        //すでに左側が満たされているのは保証されているので，右側のみを調べる．
        //つまり，midValue > rightValueとなっているので，midValueが減少することを考える．

        while (!this.indexQueue.isEmpty()) {
            IndexInfo info = indexQueue.poll();
            CPU cpu = info.getCpu();
            //System.out.println();
            if (this.isValueApproached(cpu, midValue, rightValue)) {
                System.out.println("ちかづいた");
                this.currentTotalSpeed += cpu.getSpeed();
                Iterator<CPU> currentCPUIte = this.candidateCPUmap.values().iterator();
                this.currentTotalInverseAlpha += Calc.getRoundedValue(1/(double)cpu.getSpeed());
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
                //次に，そもそも式を満たすかどうかをチェックする．
                currentA = Calc.getRoundedValue((double)this.currentTotalInverseAlpha/(double)this.totalAlpha);
                currentB =  Calc.getRoundedValue((this.currentTotalBW/(double)(this.currentTotalLinkNum/this.averageBW)));
                rightValue = Calc.getRoundedValue((-1)*this.candidateCPUmap.size()*(difTotalDataSize/difTotalTaskSize)*(1/(currentA*currentB)));
               if(midValue <= rightValue){
                   System.out.println("おめでとう");
                   break;
               }


            }
        }


        return this.candidateCPUmap;

    }



}
