package net.gripps.grouping;

import net.gripps.clustering.algorithms.mwsl_delta.CMWSL_Algorithm;
import net.gripps.clustering.algorithms.mwsl_delta.LevelComparator;
import net.gripps.clustering.algorithms.mwsl_delta.LevelInfo;
import net.gripps.clustering.common.aplmodel.AbstractTask;
import net.gripps.clustering.common.aplmodel.BBTask;
import net.gripps.clustering.common.aplmodel.DataDependence;
import net.gripps.clustering.tool.Calc;
import net.gripps.environment.CPU;
import net.gripps.environment.Environment;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.PriorityQueue;

/**
 * Created by kanemih on 2015/11/17.
 */
public class CandidateNodeSelectionManager extends CMWSL_Algorithm {
    /**
     * 入力となる環境情報クラス
     */
    protected Environment env;

    /**
     * 候補となるCPUのマッピング
     */
    protected Hashtable<Long, CPU> candidateCPUmap;


    /**
     * アプリ
     */
    protected BBTask apl;

    protected long wcp;

    protected double averageTaskSize;

    protected double averageDataSize;

    protected PriorityQueue<IndexInfo> indexQueue;

    protected PriorityQueue<DistanceInfo> distanceQueue;

    protected PriorityQueue<IndexInfo> comQueue;

    protected long totalWorkload;

    /**
     * Candidate内のCPU帯域幅合計
     */
    protected long currentTotalBW;

    protected long currentTotalLinkNum;


    /**
     * Candidate内のCPU速度合計
     */
    protected long currentTotalSpeed;


    public CandidateNodeSelectionManager(BBTask apl, String file, Environment env) {
        super(apl, file, env);
        this.env = env;
        this.apl = apl;
        this.candidateCPUmap = new Hashtable<Long, CPU>();
        this.calcAverageValues();
        this.indexQueue = new PriorityQueue<IndexInfo>(5, new IndexComparator());
        this.distanceQueue = new PriorityQueue<DistanceInfo>(5, new DistanceComparator());
        this.comQueue = new PriorityQueue<IndexInfo>(5, new BWComparator());

        this.currentTotalBW = 0;
        this.currentTotalLinkNum = 0;
        this.currentTotalSpeed = 0;


    }

    public void calcAverageValues() {
        long taskNum = apl.getTaskList().size();
        long taskTotalSize = 0;
        long edgeNum = 0;
        long edgeTotalSize = 0;
        Iterator<AbstractTask> taskIte = apl.getTaskList().values().iterator();
        while (taskIte.hasNext()) {
            AbstractTask t = taskIte.next();
            taskTotalSize += t.getMaxWeight();

            Iterator<DataDependence> dsucIte = t.getDsucList().iterator();
            while (dsucIte.hasNext()) {
                DataDependence dsuc = dsucIte.next();
                edgeNum++;
                edgeTotalSize += dsuc.getMaxDataSize();

            }

        }
        this.totalWorkload = taskTotalSize;
        double aveTaskSize = Calc.getRoundedValue((double) taskTotalSize / (double) taskNum);
        double aveEdgeSize = Calc.getRoundedValue((double) edgeTotalSize / (double) edgeNum);
        this.averageDataSize = aveEdgeSize;
        this.averageTaskSize = aveTaskSize;
        this.setMaxMin();


    }

    /**
     * 単純に，帯域幅の大きい順に選択する．
     * @return
     */
    public Hashtable<Long, CPU> deriveCaindidateCPUMapByCom(){
        this.wcp = this.calcWCP();
        Iterator<CPU> cpuIte = this.env.getCpuList().values().iterator();
        //PriorityQueue<CPU> cpuQueue = new PriorityQueue<CPU>(5, new BWComparator());
        while(cpuIte.hasNext()){
            CPU cpu = cpuIte.next();
           // cpuQueue.offer(cpu);
            double deltaopt = this.calcDeltaOpt(cpu);
           /* double com = Calc.getRoundedValue(this.averageDataSize / this.env.getAveEndToEndLink(
                    Long.valueOf(cpu.getCpuID().longValue()).intValue()));      */

            double com = Calc.getRoundedValue(this.averageDataSize / (double)this.env.getBWFromCPU(cpu));
            double indexValue = Calc.getRoundedValue(deltaopt + com);
            IndexInfo info = new IndexInfo(cpu, indexValue, deltaopt);
            this.comQueue.offer(info);
        }
        //あとは，仕事量が上限を超えるまでCPUを取ってくる．
        long remainIndex = 0;

        while (!this.comQueue.isEmpty()) {
            IndexInfo info = this.comQueue.poll();
            CPU tCPU = info.getCpu();
            //CPU tCPU = dinfo.getCpu();
            long bw2 = this.env.getBWFromCPU(tCPU);


            Iterator<CPU> currentCPUIte = this.candidateCPUmap.values().iterator();

            if (this.candidateCPUmap.isEmpty()) {
                this.currentTotalBW += this.env.getBWFromCPU(tCPU);
                this.currentTotalLinkNum++;
            }
            while (currentCPUIte.hasNext()) {
                CPU proc = currentCPUIte.next();
                //cpu <-> proc間のリンク合計を調べる．
                long link = this.env.getLink(Long.valueOf(tCPU.getOldCPUID()).intValue(), Long.valueOf(proc.getOldCPUID()).intValue());
                this.currentTotalBW += link;
                this.currentTotalLinkNum++;
            }
            this.currentTotalSpeed += tCPU.getSpeed();

            tCPU.setBw(bw2);
            tCPU.setCpuID(new Long(remainIndex));
            tCPU.setMachineID(new Long(remainIndex));
            this.candidateCPUmap.put(tCPU.getCpuID(), tCPU);
            this.totalWorkload -= info.getLowerBound() * tCPU.getSpeed();

            if (this.totalWorkload <= 0) {
                break;
            }
            remainIndex++;


        }

        return this.candidateCPUmap;
    }

    /**
     * 評価値の最小のプロセッサをまず選び，その(処理速度,帯域幅）のユークリッド距離が小さいもの
     * を選ぶ．
     *
     * @return
     */
    public Hashtable<Long, CPU> deriveCaindidateCPUMapByDistance() {
        //CPwの値を取得する．
        this.wcp = this.calcWCP();
        //まずは，各プロセッサについてindicative valueを計算する．
        Iterator<CPU> cpuIte = this.env.getCpuList().values().iterator();
        while (cpuIte.hasNext()) {
            CPU cpu = cpuIte.next();
            double deltaopt = this.calcDeltaOpt(cpu);
           /* double com = Calc.getRoundedValue(this.averageDataSize / this.env.getAveEndToEndLink(
                    Long.valueOf(cpu.getCpuID().longValue()).intValue()));      */

            double com = Calc.getRoundedValue(this.averageDataSize / (double)this.env.getBWFromCPU(cpu));
            double indexValue = Calc.getRoundedValue(deltaopt + com);
            IndexInfo info = new IndexInfo(cpu, indexValue, deltaopt);
            this.indexQueue.offer(info);

        }

        /**
         * 先頭の要素を取り出す．
         */
        IndexInfo firstInfo = this.indexQueue.poll();
        CPU cpu = firstInfo.getCpu();
        long bw = this.env.getBWFromCPU(cpu);
        cpu.setBw(bw);
        cpu.setCpuID(new Long(0));
        cpu.setMachineID(new Long(0));
        //先頭の要素をとりあえず入れる．
        this.candidateCPUmap.put(firstInfo.getCpu().getCpuID(), firstInfo.getCpu());
        double lowerbound = firstInfo.getLowerBound();
        this.totalWorkload -= lowerbound * firstInfo.getCpu().getSpeed();

        //ほかのプロセッサとのユークリッド距離を算出する．
        //   Iterator<CPU> cpuIte2 = this.env.getCpuList().values().iterator();

        while (!this.indexQueue.isEmpty()) {
            IndexInfo idxInfo = this.indexQueue.poll();
            CPU targetCPU = idxInfo.getCpu();

            long bandwidth = this.env.getBWFromCPU(targetCPU);
            double distance = Calc.getRoundedValue(Math.sqrt(Math.pow((Math.abs(cpu.getSpeed() - targetCPU.getSpeed())), 2) +
                    Math.pow(Math.abs(cpu.getBw() - bandwidth), 2)));
            DistanceInfo diffInfo = new DistanceInfo(targetCPU, distance, idxInfo.getLowerBound());
            this.distanceQueue.offer(diffInfo);

        }
        long remainIndex = 1;
        //キューに対するループ
        while (!this.distanceQueue.isEmpty()) {
            DistanceInfo dinfo = this.distanceQueue.poll();
            CPU tCPU = dinfo.getCpu();
            long bw2 = this.env.getBWFromCPU(tCPU);


            Iterator<CPU> currentCPUIte = this.candidateCPUmap.values().iterator();

            if (this.candidateCPUmap.isEmpty()) {
                this.currentTotalBW += this.env.getBWFromCPU(tCPU);
                this.currentTotalLinkNum++;
            }
            while (currentCPUIte.hasNext()) {
                CPU proc = currentCPUIte.next();
                //cpu <-> proc間のリンク合計を調べる．
                long link = this.env.getLink(Long.valueOf(tCPU.getOldCPUID()).intValue(), Long.valueOf(proc.getOldCPUID()).intValue());
                this.currentTotalBW += link;
                this.currentTotalLinkNum++;
            }
            this.currentTotalSpeed += tCPU.getSpeed();

            tCPU.setBw(bw2);
            tCPU.setCpuID(new Long(remainIndex));
            tCPU.setMachineID(new Long(remainIndex));
            this.candidateCPUmap.put(tCPU.getCpuID(), tCPU);
            this.totalWorkload -= dinfo.getLowerbound() * tCPU.getSpeed();

            if (this.totalWorkload <= 0) {
                break;
            }
            remainIndex++;


        }
        return this.candidateCPUmap;


    }

    /**
     * 二者のユークリッド距離を算出します．
     *
     * @param firstCPU
     * @param targetCPU
     * @return
     */
    public double getDistance(CPU firstCPU, CPU targetCPU) {
        return 0;

    }



    /**
     * オーバーライド用メソッド
     * @param cpu
     * @return
     */
    public boolean isCPUCandidate1(CPU cpu){
        return true;
    }
    /**
     * 入力として与えられたCPUリストから，候補となるCPUリスト
     * を指標の小さい順に選択する．そして，その結果をハッシュに格納する．
     *
     * @return
     */
    public Hashtable<Long, CPU> deriveCandidateCPUMap() {

        //CPwの値を取得する．

        this.wcp = this.calcWCP();
        //まずは，各プロセッサについてindicative valueを計算する．
        Iterator<CPU> cpuIte = this.env.getCpuList().values().iterator();
        while (cpuIte.hasNext()) {
            CPU cpu = cpuIte.next();
            double deltaopt = this.calcDeltaOpt(cpu);
            /*double com = Calc.getRoundedValue(this.averageDataSize / this.env.getAveEndToEndLink(
                    Long.valueOf(cpu.getCpuID().longValue()).intValue()));
                    */
            long bw0 = this.env.getBWFromCPU(cpu);
            double com = Calc.getRoundedValue(this.averageDataSize / (double)bw0);
            double indexValue = Calc.getRoundedValue(deltaopt + com);
            IndexInfo info = new IndexInfo(cpu, indexValue, deltaopt);
            this.indexQueue.offer(info);

        }

        Iterator<IndexInfo> ite = this.indexQueue.iterator();
        long ttlTaskSize = 0;
        long newIndex = 0;
        while (!this.indexQueue.isEmpty()) {
            IndexInfo info = this.indexQueue.poll();
            //ここで，各スケジューリング用の判断基準に入る．
            if(!isCPUCandidate1(info.getCpu())){
                break;
            }else{
                double lowerbound = info.getLowerBound();
                this.totalWorkload -= lowerbound * info.getCpu().getSpeed();
                CPU cpu = info.getCpu();
                //long bw = this.env.getBWFromCPU(cpu);
                long bw = cpu.getBw();

                Iterator<CPU> currentCPUIte = this.candidateCPUmap.values().iterator();

                if (this.candidateCPUmap.isEmpty()) {
                    this.currentTotalBW += this.env.getBWFromCPU(cpu);
                    this.currentTotalLinkNum++;
                }
                while (currentCPUIte.hasNext()) {
                    CPU proc = currentCPUIte.next();
//System.out.println("cpuID:"+cpu.getCpuID()+"/procID:"+proc.getCpuID());
                    //cpu <-> proc間のリンク合計を調べる．

                    long link = this.env.getLink(Long.valueOf(cpu.getOldCPUID()).intValue(), Long.valueOf(proc.getOldCPUID()).intValue());
                    this.currentTotalBW += link;
                    this.currentTotalLinkNum++;
                }
                this.currentTotalSpeed += cpu.getSpeed();
                cpu.setBw(bw);
                cpu.setCpuID(new Long(newIndex));
                cpu.setMachineID(new Long(newIndex));
               // this.candidateCPUmap.put(info.getCpu().getCpuID(), info.getCpu());
                this.candidateCPUmap.put(cpu.getCpuID(), cpu);
                if (this.totalWorkload <= 0) {
                    break;
                }
                newIndex++;
                //System.out.println("ID:"+info.getCpu().getCpuID()+" :Value:"+info.getIndexValue());
            }

        }


        return this.candidateCPUmap;


    }

    /**
     * @param cpu
     * @return
     */
    public double calcDeltaOpt(CPU cpu) {
    //    long ownLink = this.env.getMachineList().get(cpu.getMachineID()).getBw();

        long ownLink = this.env.getBWFromCPU(cpu);

      /*double value = Calc.getRoundedValue(Math.sqrt(Calc.getRoundedValue((1/(double)cpu.getSpeed())*((double) this.wcp / (double) cpu.getSpeed()) *
                (Calc.getRoundedValue((double) this.maxData / (double) ownLink)
                        - Calc.getRoundedValue((double) this.maxData / (double) this.maxBW) +
                        Calc.getRoundedValue((double) this.minWorkload / (double) this.maxSpeed) +
                        Calc.getRoundedValue((double) this.minData / (double) this.maxBW)))));
                        */

     /*   double value = Calc.getRoundedValue(Math.sqrt(Calc.getRoundedValue((1/(double)cpu.getSpeed())*((double) this.wcp / (double) cpu.getSpeed()) *
                  (Calc.getRoundedValue((double) this.maxData / (double) ownLink)
                          - Calc.getRoundedValue((double) this.maxData / (double) ownLink) +
                          Calc.getRoundedValue((double) this.minWorkload / (double) cpu.getSpeed()) +
                          Calc.getRoundedValue((double) this.minData / (double) ownLink)))));
*/


        double value = Calc.getRoundedValue(Math.sqrt(Calc.getRoundedValue((1/(double)cpu.getSpeed())*((double) this.wcp / (double) cpu.getSpeed()) *
                (Calc.getRoundedValue((double) this.maxData / (double) ownLink)
                       /* - Calc.getRoundedValue((double) this.maxData / (double) ownLink)*/ +
                        Calc.getRoundedValue((double) this.minWorkload / (double) cpu.getSpeed()) +
                        Calc.getRoundedValue((double) this.minData / (double) ownLink)))));





        return value;
    }

    public Environment getEnv() {
        return env;
    }

    public void setEnv(Environment env) {
        this.env = env;
    }

    public Hashtable<Long, CPU> getCandidateCPUmap() {
        return candidateCPUmap;
    }

    public void setCandidateCPUmap(Hashtable<Long, CPU> candidateCPUmap) {
        this.candidateCPUmap = candidateCPUmap;
    }
}
