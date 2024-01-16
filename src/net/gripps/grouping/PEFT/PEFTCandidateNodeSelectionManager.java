package net.gripps.grouping.PEFT;

import net.gripps.clustering.common.Constants;
import net.gripps.clustering.common.aplmodel.AbstractTask;
import net.gripps.clustering.common.aplmodel.BBTask;
import net.gripps.clustering.common.aplmodel.DataDependence;
import net.gripps.clustering.tool.Calc;
import net.gripps.environment.CPU;
import net.gripps.environment.Environment;
import net.gripps.grouping.HSV.HSVCandidateNodeSelectionManager;
import net.gripps.grouping.HSV.HSVGComparator;
import net.gripps.grouping.IndexInfo;
import net.gripps.scheduling.algorithms.heterogeneous.peft.OCTComparator;
import net.gripps.scheduling.algorithms.heterogeneous.peft.OCTDoubleComparator;
import net.gripps.util.EnvLoader;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.PriorityQueue;

/**
 * Created by kanemih on 2016/01/28.
 */
public class PEFTCandidateNodeSelectionManager extends HSVCandidateNodeSelectionManager {


    protected double averageBWFirstCPU;

    protected double currentAverageBW;

    /**
     * firstCPUによるシステム．これは仮想システム．
     */
    protected Environment firstCPUEnv;

    public PEFTCandidateNodeSelectionManager(BBTask apl, String file, Environment env) {
        super(apl, file, env);
        /**
         * 定義しなおす．
         */
        this.readyList = new PriorityQueue<AbstractTask>(5, new OCTComparator());
        this.firstCPUEnv = new Environment();
        this.averageBWFirstCPU = 0;
        this.currentAverageBW = 0;
        this.readyList = new PriorityQueue<AbstractTask>(5, new OCTDoubleComparator());


    }

    public void resetOCT() {
        Iterator<AbstractTask> taskIte = this.apl.getTaskList().values().iterator();
        while (taskIte.hasNext()) {
            AbstractTask task = taskIte.next();
            task.setAve_oct_double(Constants.INFINITY);
            task.getOctMap().clear();
        }
    }

    /**
     * @param sucTask    後続タスク
     * @param cpu        外側のCPU
     * @param aveComTime 平均通信時間
     * @return
     */
    public long calcOCT(AbstractTask sucTask, CPU cpu, double aveComTime, Hashtable<Long, CPU> cpuTable) {
        //既に計算済みであれば，何もせず終了
        if (sucTask.getAve_oct_double() != Constants.INFINITY) {
            return 0;
        }
        long totalOCT = 0;

        if (sucTask.getDsucList().isEmpty()) {
            sucTask.setAve_oct_double(0);
            if (sucTask.getOctMap().size() == cpuTable.size()) {
                return 0;
            } else {
                Iterator<CPU> cIte = cpuTable.values().iterator();
                while (cIte.hasNext()) {
                    CPU proc = cIte.next();
                    sucTask.getOctMap().put(proc.getCpuID(), new Long(0));
                }
            }
            return 0;
        }

        //sucTaskの外側のCPUの値を計算する．
        Iterator<CPU> cpuIte0 = cpuTable.values().iterator();
        while (cpuIte0.hasNext()) {

            CPU outerCPU = cpuIte0.next();

            Iterator<DataDependence> dsucIte = sucTask.getDsucList().iterator();
            long maxValue = 0;
            while (dsucIte.hasNext()) {
                DataDependence dsuc = dsucIte.next();
                AbstractTask sucSucTask = this.retApl.findTaskByLastID(dsuc.getToID().get(1));
                //後続タスクのOCTを計算する．
                //  System.out.println("From:"+sucTask.getIDVector().get(1).longValue()+"/"+sucTask.getOctMap().size() + "To:"+sucSucTask.getIDVector().get(1).longValue()+"/"+sucSucTask.getOctMap().size());
                this.calcOCT(sucSucTask, cpu, Calc.getRoundedValue(dsuc.getMaxDataSize() / this.currentAverageBW), cpuTable);
                long minValue = this.getMinSucOCT(sucSucTask, outerCPU, Calc.getRoundedValue(dsuc.getMaxDataSize() / this.currentAverageBW), cpuTable);
                if (minValue >= maxValue) {
                    maxValue = minValue;
                }

            }

            sucTask.getOctMap().put(outerCPU.getCpuID(), maxValue);
            totalOCT += maxValue;
        }
        double aveOCT = Calc.getRoundedValue(totalOCT / (double) cpuTable.size());
        sucTask.setAve_oct_double(aveOCT);

        return 0;
    }


    public long getMinSucOCT(AbstractTask task, CPU cpu, double aveCommTime, Hashtable<Long, CPU> cpuTable) {
        Iterator<CPU> cpuIte = cpuTable.values().iterator();
        double minValue = Constants.MAXValue;
//System.out.println("ID:"+task.getIDVector().get(1).longValue()+":size:"+task.getOctMap().size());

        while (cpuIte.hasNext()) {
            CPU sucCPU = cpuIte.next();
            double comTime = 0;
            if (cpu.getCpuID() != sucCPU.getCpuID()) {
                comTime = aveCommTime;
            }
            double value = 0;
            if (task.getDsucList().isEmpty()) {
                value = Calc.getRoundedValue(task.getMaxWeight() / (double) sucCPU.getSpeed()) + comTime;
            } else {
                value = task.getOctMap().get(sucCPU.getCpuID()).longValue() +
                        Calc.getRoundedValue(task.getMaxWeight() / (double) sucCPU.getSpeed()) + comTime;

            }
            if (value <= minValue) {
                minValue = value;
            }
        }

        return (long) minValue;
    }

    /**
     * STARTタスクから開始して，各タスクのOCTを決めます．
     * 引数には，Envが入ります．
     */
    public void deriveOCT(Hashtable<Long, CPU> cpuTable) {

        Iterator<Long> startIte = this.apl.getStartTaskSet().iterator();
        //STARTタスクに対するループ
        while (startIte.hasNext()) {
            AbstractTask startTask = this.apl.findTaskByLastID(startIte.next());
            long totalOCT = 0;
            Iterator<CPU> cpuIte0 = cpuTable.values().iterator();
            while (cpuIte0.hasNext()) {
                CPU cpu = cpuIte0.next();
                Iterator<DataDependence> dsucIte = startTask.getDsucList().iterator();
                long maxValue = 0;
                while (dsucIte.hasNext()) {
                    DataDependence dsuc = dsucIte.next();
                    AbstractTask sucTask = this.apl.findTaskByLastID(dsuc.getToID().get(1));
                    //後続タスクのOCTを計算する．
                    this.calcOCT(sucTask, cpu, Calc.getRoundedValue(dsuc.getMaxDataSize() / this.currentAverageBW), cpuTable);
                    long minValue = this.getMinSucOCT(sucTask, cpu, Calc.getRoundedValue(dsuc.getMaxDataSize() / this.currentAverageBW), cpuTable);
                    if (minValue >= maxValue) {
                        maxValue = minValue;
                    }
                }
                startTask.getOctMap().put(cpu.getCpuID(), maxValue);
                totalOCT += maxValue;
            }
            double aveOCT = Calc.getRoundedValue(totalOCT / (double) cpuTable.size());
            startTask.setAve_oct_double(aveOCT);

        }

    }

    /**
     * まずは，IndexQueueに各プロセッサを格納し，firstCPUだけのプロセッサを用意する．
     * そしてfirstCPUによるのシステム上でNを決めて，NコのfirstCPUによるrank_octを各タスク
     * に対して割り当てる．具体的には，
     *
     * @return
     */
    public Hashtable<Long, CPU> deriveCaindidateCPUMapbyPEFT() {
        //初期化
        //まずは，firstCPUの決定，indexQueueの格納，maxWorkloadの決定，firstCPUによる平均タスクサイズ，データサイズを設定する．
        //さらに firstCPUによる数Nを決める．
        this.configureTaskPair();
        this.averageBWFirstCPU = this.env.getBWFromCPU(this.firstProcessor);

        //とりあえずはfirstCPUのBWをセットする．
        this.currentAverageBW = this.averageBWFirstCPU;

        Hashtable<Long, CPU> firstCPUMap = new Hashtable<Long, CPU>();

        //仮想環境を生成する．
        for (int i = 0; i < this.firstCPUNum; i++) {
            CPU cpu = new CPU(new Long(i), this.firstProcessor.getSpeed(), null, null);
            cpu.setBw(this.env.getBWFromCPU(this.firstProcessor));
            cpu.setMachineID(new Long(i));
            firstCPUMap.put(new Long(cpu.getCpuID()), cpu);
        }
        //firstCPUによるEnvを生成する．
        this.firstCPUEnv = new EnvLoader(this.file, firstCPUMap);
        //firstCPUEnvによるOCTテーブルを構築する．
        this.deriveOCT(this.firstCPUEnv.getCpuList());
        double mBlevel = 0;

        Iterator<Long> startIte = this.apl.getStartTaskSet().iterator();
        AbstractTask maxStartTask = null;
        while (startIte.hasNext()) {
            AbstractTask startTask = this.apl.findTaskByLastID(startIte.next());
            this.readyList.add(startTask);
            if (startTask.getAve_oct_double() >= mBlevel) {
                maxStartTask = startTask;
                mBlevel = startTask.getAve_oct_double();
            }

        }


        this.CPSet.add(maxStartTask.getIDVector().get(1));

        //maxStartTaskから初めて，ENDまでの間にCPへ入れる
        while (!maxStartTask.getDsucList().isEmpty()) {
            //後続タスクからrank_octの最大値を選ぶ．
            Iterator<DataDependence> dsucIte = maxStartTask.getDsucList().iterator();
            double maxOCT = -1;
            while (dsucIte.hasNext()) {
                AbstractTask sucTask = this.apl.findTaskByLastID(dsucIte.next().getToID().get(1));
                double tmpOCT = sucTask.getAve_oct_double();
                if (tmpOCT >= maxOCT) {
                    maxStartTask = sucTask;
                    maxOCT = tmpOCT;
                }
            }

            this.CPSet.add(maxStartTask.getIDVector().get(1));

        }

        this.diffBlevel = Constants.MAXValue;

        // while(!this.freeClusterList.isEmpty()){
        while (!this.readyList.isEmpty()) {
            AbstractTask maxTask = null;
            //まずは，選ばれたものに対して最もblevelの差が小さなものを選択する．
            //先頭タスクを取得する．
            maxTask = readyList.poll();
            double maxDuration = this.calcDuratoinTime(maxTask);
            Object[] oa = this.readyList.toArray();
            //一つのクラスタ内で，タスクのtlevel順にソートする．

            Arrays.sort(oa, new OCTDoubleComparator());
            int len = oa.length;
            //最後の一つだったのなら，そこで終了．
            if (len == 0 && maxTask.getDsucList().isEmpty()) {
                break;
            }else if(len == 0){
                //1つしかなければ，schedule済みを増やして次へ
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
                continue;
            }
            AbstractTask nextTask = (AbstractTask) oa[0];


            //maxTaskとnextTaskの差分を算出する．
            // long dif = maxTask.getHprv_rank() - nextTask.getHprv_rank();

            if ((this.CPSet.contains(maxTask.getIDVector().get(1))) && (!this.CPSet.contains(nextTask.getIDVector().get(1)))) {
                double octDif = maxTask.getAve_oct_double() - nextTask.getAve_oct_double();

                if ((octDif <= this.diffBlevel) && (octDif > 0)) {
                    this.diffBlevel = octDif;
                    this.task_k = maxTask;
                    this.task_s = nextTask;
                }


           }
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

        this.candidateCPUmap = this.mergeProcess();

        return this.candidateCPUmap;

    }

    /**
     * 最低限のノード数までは無条件に追加して，それ以降は，規定の数に達するまでは
     * task_kのoct > task_sのoctである限りループする．
     *
     * @return
     */
    public Hashtable<Long, CPU> mergeProcess() {

        Iterator<IndexInfo> ite = this.indexQueue.iterator();
        long ttlTaskSize = 0;
        long newIndex = 0;
        //先頭の要素を閲覧する．
        IndexInfo dinfo = this.indexQueue.peek();
        double fworkload = Calc.getRoundedValue(dinfo.getCpu().getSpeed() * dinfo.getLowerBound());
        //最低限の数を決める．
        long minNum = (long) Math.ceil(Calc.getRoundedValue(this.apl.getMaxWeight() / this.maxWorkload));

        long globalCount = 0;

        boolean isUnder = false;
        while (!this.indexQueue.isEmpty()) {
            IndexInfo info = this.indexQueue.poll();
            CPU cpu0 = info.getCpu();
            long bw0 = this.env.getBWFromCPU(cpu0);
            //ここで，各スケジューリング用の判断基準に入る．
            if (globalCount <= minNum) {
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
                this.currentAverageBW = Calc.getRoundedValue(this.currentTotalBW / this.currentTotalLinkNum);


                cpu.setBw(bw);
                cpu.setCpuID(new Long(newIndex));
                cpu.setMachineID(new Long(newIndex));

                this.candidateCPUmap.put(info.getCpu().getCpuID(), info.getCpu());

                newIndex++;
                this.currentNum++;
            } else {
                //現在の状態でOCTテーブルを再構築し，task_kとtask_sのrank_octが逆転しないかをチェックする．
                this.resetOCT();
                if (globalCount == minNum) {
                    //初回は，現状のチェックのみ
                    this.deriveOCT(this.candidateCPUmap);
                    if (this.task_k.getAve_oct_double() >= this.task_s.getAve_oct_double()) {
                        //まだ逆転しなければ，続ける．
                    } else {
                        //逆転するならば，抜ける．
                        break;
                    }


                } else {
                    //tmpのhashtableを用意する．
                    Hashtable<Long, CPU> tmpTable = new Hashtable<Long, CPU>();
                    Iterator<CPU> cpuIte = this.candidateCPUmap.values().iterator();
                    while (cpuIte.hasNext()) {
                        CPU cpu = cpuIte.next();
                        tmpTable.put(cpu.getCpuID(), cpu);
                    }
                    this.deriveOCT(tmpTable);

                    if (this.task_k.getAve_oct_double() >= this.task_s.getAve_oct_double()) {
                        //まだ逆転しなければ，続ける．
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
                            long link = this.env.getLink(Long.valueOf(cpu.getCpuID()).intValue(), Long.valueOf(proc.getCpuID()).intValue());
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

                    } else {
                        //逆転するならば，抜ける．
                        break;
                    }
                }

            }
            globalCount++;


            //System.out.println("ID:"+info.getCpu().getCpuID()+" :Value:"+info.getIndexValue());
        }


        return this.candidateCPUmap;

    }

}
